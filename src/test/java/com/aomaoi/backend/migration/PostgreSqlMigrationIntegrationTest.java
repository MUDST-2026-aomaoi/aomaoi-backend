package com.aomaoi.backend.migration;

import com.aomaoi.backend.BackendApplication;
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
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Each test owns an isolated schema in an explicitly configured PostgreSQL test database. */
@EnabledIfEnvironmentVariable(named = "AOMAOI_MIGRATION_TEST_URL", matches = ".+")
class PostgreSqlMigrationIntegrationTest {
    private Connection connection;
    private String schema;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(System.getenv("AOMAOI_MIGRATION_TEST_URL"),
                System.getenv("AOMAOI_MIGRATION_TEST_USER"), System.getenv("AOMAOI_MIGRATION_TEST_PASSWORD"));
        schema = "migration_test_" + UUID.randomUUID().toString().replace("-", "");
        execute("CREATE SCHEMA " + schema);
        connection.setSchema(schema);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                }
                connection.setAutoCommit(true);
                execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void createsCompleteSchemaAndSecondUpdatePreservesData() throws Exception {
        Liquibase liquibase = liquibase();
        liquibase.validate();
        liquibase.update(new Contexts(), new LabelExpression());
        insertRecords();
        liquibase.update(new Contexts(), new LabelExpression());

        assertEquals("6", scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema = '"
                + schema + "' AND table_name IN ('users', 'farm', 'worker', 'admin_profile', 'work_logs', 'audit_logs')"));
        assertEquals("1", scalar("SELECT count(*) FROM databasechangelog"));
        assertEquals("10.50", scalar("SELECT total FROM work_logs"));
        assertEquals("bigint", scalar("SELECT data_type FROM information_schema.columns WHERE table_schema = '"
                + schema + "' AND table_name = 'worker' AND column_name = 'farm_id'"));
        assertEquals("3", scalar("SELECT count(*) FROM pg_indexes WHERE schemaname = '" + schema
                + "' AND indexname IN ('idx_worker_farm_id', 'idx_admin_profile_farm_id', 'idx_work_logs_worker_date')"));
    }

    @Test
    void enforcesRelationshipsUniquenessAndRequiredDates() throws Exception {
        liquibase().update(new Contexts(), new LabelExpression());
        insertRecords();

        assertThrows(SQLException.class, () -> execute("INSERT INTO users(username, password, role)"
                + " VALUES ('worker', 'hash', 'worker')"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO worker(user_id) VALUES (1)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO admin_profile(user_id) VALUES (2)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO worker(farm_id) VALUES (999)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO work_logs(worker_id, type, work_date, total)"
                + " VALUES (1, 'cutting', CURRENT_DATE, 1)"));
        assertThrows(SQLException.class, () -> execute("DELETE FROM farm WHERE id = 1"));
        assertThrows(SQLException.class, () -> execute("DELETE FROM worker WHERE id = 1"));
    }

    @Test
    void demoContextSeedsExpectedAccountsAndFarms() throws Exception {
        liquibase().update(new Contexts("demo"), new LabelExpression());

        assertEquals("26", scalar("SELECT count(*) FROM users"));
        assertEquals("1", scalar("SELECT count(*) FROM users WHERE role = 'superadmin'"));
        assertEquals("5", scalar("SELECT count(*) FROM users WHERE role = 'admin'"));
        assertEquals("20", scalar("SELECT count(*) FROM users WHERE role = 'worker'"));
        assertEquals("5", scalar("SELECT count(*) FROM farm"));
        assertEquals("5", scalar("SELECT count(*) FROM farm WHERE name IN ('ฟาร์มเพื่อพ่อ', 'ไร่สุขใจ', 'ไร่แสงทอง', 'ฟาร์มอ้อยบ้านสวน', 'ไร่พอเพียง')"));
        assertEquals("0", scalar("SELECT count(*) FROM farm WHERE name LIKE '%(ตัวอย่าง)'"));
        assertEquals("5", scalar("SELECT count(*) FROM admin_profile"));
        assertEquals("20", scalar("SELECT count(*) FROM worker"));
        assertEquals("20", scalar("SELECT count(*) FROM users WHERE is_first_login"));
    }

    @Test
    void demoContextReusesExistingFarmRowsOnANonBlankDatabase() throws Exception {
        liquibase().update(new Contexts(), new LabelExpression());
        execute("INSERT INTO farm(name, location) VALUES ('ฟาร์มเพื่อพ่อ (ตัวอย่าง)', 'ชลบุรี')");

        liquibase().update(new Contexts("demo"), new LabelExpression());

        assertEquals("5", scalar("SELECT count(*) FROM farm"));
        assertEquals("1", scalar("SELECT count(*) FROM farm WHERE name = 'ฟาร์มเพื่อพ่อ'"));
        assertEquals("5", scalar("SELECT count(*) FROM admin_profile"));
        assertEquals("20", scalar("SELECT count(*) FROM worker"));
    }

    @Test
    void liquibaseRollbackRemovesSchemaAndAllowsRecreation() throws Exception {
        Liquibase liquibase = liquibase();
        liquibase.update(new Contexts(), new LabelExpression());
        insertRecords();
        liquibase.rollback(1, new Contexts(), new LabelExpression());

        assertEquals("0", scalar("SELECT count(*) FROM databasechangelog"));
        assertEquals("0", scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema = '"
                + schema + "' AND table_name IN ('users', 'farm', 'worker', 'admin_profile', 'work_logs', 'audit_logs')"));
        liquibase.update(new Contexts(), new LabelExpression());
        insertRecords();
        assertEquals("1", scalar("SELECT count(*) FROM work_logs"));
    }

    @Test
    void applicationStartsWithLiquibaseSeedingAndHibernateSchemaValidation() throws Exception {
        try (var context = new SpringApplicationBuilder(BackendApplication.class).run(
                "--server.port=0",
                "--spring.datasource.url=" + System.getenv("AOMAOI_MIGRATION_TEST_URL"),
                "--spring.datasource.username=" + System.getenv("AOMAOI_MIGRATION_TEST_USER"),
                "--spring.datasource.password=" + System.getenv("AOMAOI_MIGRATION_TEST_PASSWORD"),
                "--spring.datasource.hikari.schema=" + schema,
                "--spring.liquibase.default-schema=" + schema,
                "--spring.jpa.properties.hibernate.default_schema=" + schema,
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.liquibase.enabled=true",
                "--app.seed.enabled=true",
                "--app.seed.demo-data=true",
                "--app.bootstrap-admin-password=integration-test-password",
                "--app.migration-job=false",
                "--spring.jpa.show-sql=false",
                "--logging.level.org.springframework.security=INFO")) {
            assertTrue(context.isActive());
            assertEquals("4", scalar("SELECT count(*) FROM users"));
            assertEquals("4", scalar("SELECT count(*) FROM work_logs"));
        }
    }

    private Liquibase liquibase() throws Exception {
        var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName(schema);
        database.setLiquibaseSchemaName(schema);
        // This test owns the connection so teardown can remove the disposable schema.
        return new Liquibase("db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(), database);
    }

    private void insertRecords() throws SQLException {
        execute("INSERT INTO users(username, password, role) VALUES ('worker', 'hash', 'worker'), ('admin', 'hash', 'admin')");
        execute("INSERT INTO farm(name) VALUES ('Test farm')");
        execute("INSERT INTO worker(farm_id, user_id) VALUES (1, 1)");
        execute("INSERT INTO admin_profile(farm_id, user_id) VALUES (1, 2)");
        execute("INSERT INTO work_logs(worker_id, type, work_date, start_date, end_date, total)"
                + " VALUES (1, 'cutting', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, 10.50)");
        execute("INSERT INTO audit_logs(action, performed_by, target_user, timestamp)"
                + " VALUES ('CREATE', 'admin', 'worker', CURRENT_TIMESTAMP)");
    }

    private void execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String scalar(String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}
