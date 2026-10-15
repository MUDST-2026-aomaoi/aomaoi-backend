package com.aomaoi.backend.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Uses disposable schemas only. Enable explicitly against a PostgreSQL test database. */
@EnabledIfEnvironmentVariable(named = "AOMAOI_MIGRATION_TEST_URL", matches = ".+")
class FarmIdMigrationIntegrationTest {
    private Connection connection;
    private String schema;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(System.getenv("AOMAOI_MIGRATION_TEST_URL"),
                System.getenv("AOMAOI_MIGRATION_TEST_USER"), System.getenv("AOMAOI_MIGRATION_TEST_PASSWORD"));
        schema = "farm_id_test_" + UUID.randomUUID().toString().replace("-", "");
        execute("CREATE SCHEMA " + schema);
        connection.setSchema(schema);
        connection.setAutoCommit(false);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
                execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void convertsExistingReferencesPreservesNullsAndAddsIndexes() throws Exception {
        legacySchema();
        execute("INSERT INTO farm(id, name) VALUES (10, 'Existing farm');"
                + "INSERT INTO worker(id, farm_id) VALUES (1, '10'), (2, NULL);"
                + "INSERT INTO admin_profile(id, farm_id) VALUES (1, '0010');");

        migrate();

        assertEquals("bigint", scalar("SELECT data_type FROM information_schema.columns"
                + " WHERE table_schema = '" + schema + "' AND table_name = 'worker' AND column_name = 'farm_id'"));
        assertEquals("bigint", scalar("SELECT data_type FROM information_schema.columns"
                + " WHERE table_schema = '" + schema + "' AND table_name = 'admin_profile' AND column_name = 'farm_id'"));
        assertEquals("10", scalar("SELECT farm_id FROM worker WHERE id = 1"));
        assertEquals("10", scalar("SELECT farm_id FROM admin_profile WHERE id = 1"));
        assertNull(scalar("SELECT farm_id FROM worker WHERE id = 2"));
        assertEquals("3", scalar("SELECT count(*) FROM pg_indexes WHERE schemaname = '" + schema
                + "' AND indexname IN ('idx_worker_farm_id', 'idx_admin_profile_farm_id', 'idx_work_logs_worker_date')"));
        execute("SET CONSTRAINTS ALL IMMEDIATE");
        Savepoint validData = connection.setSavepoint();
        assertThrows(SQLException.class, () -> execute("DELETE FROM farm WHERE id = 10"));
        connection.rollback(validData);
        execute("UPDATE farm SET status = 'inactive' WHERE id = 10");
        assertEquals("inactive", scalar("SELECT status FROM farm WHERE id = 10"));
    }

    @Test
    void rejectsNonNumericReferencesWithoutLosingLegacyData() throws Exception {
        legacySchema();
        execute("INSERT INTO worker(id, farm_id) VALUES (1, 'farm-ten')");
        Savepoint beforeMigration = connection.setSavepoint();

        SQLException error = assertThrows(SQLException.class, this::migrate);

        assertTrue(error.getMessage().contains("non-numeric farm_id"));
        connection.rollback(beforeMigration);
        assertEquals("farm-ten", scalar("SELECT farm_id FROM worker WHERE id = 1"));
    }

    @Test
    void rejectsMissingFarmReferencesWithoutDroppingRows() throws Exception {
        legacySchema();
        execute("INSERT INTO admin_profile(id, farm_id) VALUES (1, '999')");
        Savepoint beforeMigration = connection.setSavepoint();

        assertThrows(SQLException.class, this::migrate);

        connection.rollback(beforeMigration);
        assertEquals("999", scalar("SELECT farm_id FROM admin_profile WHERE id = 1"));
    }

    @Test
    void rejectsValuesOutsideBigintRange() throws Exception {
        legacySchema();
        execute("INSERT INTO worker(id, farm_id) VALUES (1, '9223372036854775808')");

        assertThrows(SQLException.class, this::migrate);
    }

    @Test
    void rejectsOrphansImportedAfterMigrationAtTransactionBoundary() throws Exception {
        legacySchema();
        migrate();
        execute("INSERT INTO worker(id, farm_id) VALUES (1, 999)");

        assertThrows(SQLException.class, () -> execute("SET CONSTRAINTS ALL IMMEDIATE"));
    }

    @Test
    void springBootChangelogBuildsNumericSchemaAndCanRunAgain() throws Exception {
        var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName(schema);
        database.setLiquibaseSchemaName(schema);
        // Connection lifecycle is owned by this test so cleanup can remove the schema.
        Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
        liquibase.update(new Contexts(), new LabelExpression());

        assertEquals("bigint", scalar("SELECT data_type FROM information_schema.columns"
                + " WHERE table_schema = '" + schema + "' AND table_name = 'worker' AND column_name = 'farm_id'"));
        assertEquals("1", scalar("SELECT count(*) FROM databasechangelog WHERE id = '009-normalize-farm-ids'"));
    }

    @Test
    void rollbackRestoresTextReferencesAndPreservesIds() throws Exception {
        legacySchema();
        execute("INSERT INTO farm(id) VALUES (10); INSERT INTO worker(id, farm_id) VALUES (1, '10')");
        migrate();
        String migration = Files.readString(Path.of("src/main/resources/db/changelog/changes/009-normalize-farm-ids.sql"));
        for (String line : migration.lines().filter(line -> line.startsWith("--rollback ")).toList()) {
            execute(line.substring("--rollback ".length()));
        }

        assertEquals("10", scalar("SELECT farm_id FROM worker WHERE id = 1"));
        assertEquals("character varying", scalar("SELECT data_type FROM information_schema.columns"
                + " WHERE table_schema = '" + schema + "' AND table_name = 'worker' AND column_name = 'farm_id'"));
    }

    private void legacySchema() throws Exception {
        execute("CREATE TABLE farm (id BIGINT PRIMARY KEY, name TEXT, status VARCHAR(32));"
                + "CREATE TABLE worker (id BIGINT PRIMARY KEY, farm_id VARCHAR(255));"
                + "CREATE TABLE admin_profile (id BIGINT PRIMARY KEY, farm_id VARCHAR(255));"
                + "CREATE TABLE work_logs (id BIGINT PRIMARY KEY, worker_id BIGINT, work_date DATE);");
    }

    private void migrate() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(
                "db/changelog/changes/009-normalize-farm-ids.sql")) {
            execute(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String scalar(String sql) throws SQLException {
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}
