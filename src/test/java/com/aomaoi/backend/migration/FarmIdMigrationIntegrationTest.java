package com.aomaoi.backend.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
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
    void importsCurrentSnapshotAfterMigrationIncludingAdminsBeforeFarms() throws Exception {
        legacySchema();
        migrate();
        String snapshot = Files.readString(Path.of("liquibase/changes/999-latest-data.sql"));

        // Redirect every explicit public reference and sequence name into the disposable schema.
        execute(snapshot.replace("public.", schema + "."));
        connection.setSchema(schema);
        execute("SET CONSTRAINTS ALL IMMEDIATE");

        assertNotEquals("0", scalar("SELECT count(*) FROM farm"));
        assertEquals("0", scalar("SELECT count(*) FROM worker w LEFT JOIN farm f ON f.id = w.farm_id"
                + " WHERE w.farm_id IS NOT NULL AND f.id IS NULL"));
        assertEquals("0", scalar("SELECT count(*) FROM admin_profile a LEFT JOIN farm f ON f.id = a.farm_id"
                + " WHERE a.farm_id IS NOT NULL AND f.id IS NULL"));
        // Numeric snapshots produced after migration also support this insertion order.
        execute("SET CONSTRAINTS ALL DEFERRED;"
                + "INSERT INTO admin_profile(id, farm_id) VALUES (999999, 999999);"
                + "INSERT INTO farm(id, name) VALUES (999999, 'Future numeric snapshot');"
                + "SET CONSTRAINTS ALL IMMEDIATE;");
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
    void dockerChangelogImportsSnapshotAndRollsBackMigration() throws Exception {
        Path directory = Files.createTempDirectory("aomaoi-liquibase-test-");
        try {
            Files.createDirectories(directory.resolve("changes"));
            Files.copy(Path.of("liquibase/db.changelog-master.xml"), directory.resolve("db.changelog-master.xml"));
            Files.copy(Path.of("liquibase/changes/001-create-initial-tables.sql"),
                    directory.resolve("changes/001-create-initial-tables.sql"));
            Files.writeString(directory.resolve("changes/999-latest-data.sql"),
                    Files.readString(Path.of("liquibase/changes/999-latest-data.sql")).replace("public.", schema + "."));
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            database.setLiquibaseSchemaName(schema);
            try (var resources = new CompositeResourceAccessor(
                    new DirectoryResourceAccessor(directory), new ClassLoaderResourceAccessor())) {
                Liquibase liquibase = new Liquibase("db.changelog-master.xml", resources, database);
                liquibase.update(new Contexts(), new LabelExpression());
                connection.setSchema(schema);
                assertEquals("bigint", scalar("SELECT data_type FROM information_schema.columns"
                        + " WHERE table_schema = '" + schema + "' AND table_name = 'worker' AND column_name = 'farm_id'"));
                assertNotEquals("0", scalar("SELECT count(*) FROM farm"));
                liquibase.update(new Contexts(), new LabelExpression());
                liquibase.rollback(2, new Contexts(), new LabelExpression());
                assertEquals("character varying", scalar("SELECT data_type FROM information_schema.columns"
                        + " WHERE table_schema = '" + schema + "' AND table_name = 'worker' AND column_name = 'farm_id'"));
            }
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
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
        execute(Files.readString(Path.of("liquibase/changes/001-create-initial-tables.sql")));
    }

    private void migrate() throws Exception {
        execute(Files.readString(Path.of("src/main/resources/db/changelog/changes/009-normalize-farm-ids.sql")));
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
