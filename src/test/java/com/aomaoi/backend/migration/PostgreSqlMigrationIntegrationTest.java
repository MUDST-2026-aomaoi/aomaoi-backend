package com.aomaoi.backend.migration;

import com.aomaoi.backend.BackendApplication;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Each test owns an isolated schema in a disposable PostgreSQL container. */
class PostgreSqlMigrationIntegrationTest {
    private static final Contexts SCHEMA_ONLY = new Contexts("production");
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("aomaoi_migration_test")
            .withUsername("aomaoi_test")
            .withPassword("aomaoi_test");
    private Connection connection;
    private String schema;

    @BeforeAll
    static void startPostgres() {
        postgres.start();
    }

    @AfterAll
    static void stopPostgres() {
        postgres.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
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
        liquibase.update(SCHEMA_ONLY, new LabelExpression());
        insertRecords();
        liquibase.update(SCHEMA_ONLY, new LabelExpression());

        assertEquals("7", scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema = '"
                + schema + "' AND table_name IN ('users', 'farm', 'worker', 'admin_profile', 'work_logs', 'audit_logs', 'salary_advances')"));
        assertEquals("4", scalar("SELECT count(*) FROM databasechangelog"));
        assertEquals("10.50", scalar("SELECT total FROM work_logs"));
        assertEquals("5.00", scalar("SELECT amount FROM salary_advances"));
        assertEquals("bigint", scalar("SELECT data_type FROM information_schema.columns WHERE table_schema = '"
                + schema + "' AND table_name = 'worker' AND column_name = 'farm_id'"));
        assertEquals("3", scalar("SELECT count(*) FROM pg_indexes WHERE schemaname = '" + schema
                + "' AND indexname IN ('idx_worker_farm_id', 'idx_admin_profile_farm_id', 'idx_work_logs_worker_date')"));
    }

    @Test
    void enforcesRelationshipsUniquenessAndRequiredDates() throws Exception {
        liquibase().update(SCHEMA_ONLY, new LabelExpression());
        insertRecords();

        assertThrows(SQLException.class, () -> execute("INSERT INTO users(username, password, role)"
                + " VALUES ('worker', 'hash', 'worker')"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO worker(user_id) VALUES (1)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO admin_profile(user_id) VALUES (2)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO worker(farm_id) VALUES (999)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO work_logs(worker_id, type, work_date, total)"
                + " VALUES (1, 'cutting', CURRENT_DATE, 1)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO salary_advances(worker_id,advance_date,type,amount,recorded_by)"
                + " VALUES (1,CURRENT_DATE,'CASH',-1,'admin')"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO salary_advances(worker_id,advance_date,type,amount,recorded_by)"
                + " VALUES (1,CURRENT_DATE,'GOODS',5,'admin')"));
        assertThrows(SQLException.class, () -> execute("UPDATE worker SET farm_id = NULL WHERE id = 1"));
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
        assertEquals("25", scalar("SELECT count(*) FROM users WHERE is_first_login"));
    }

    @Test
    void demoContextReusesExistingFarmRowsOnANonBlankDatabase() throws Exception {
        liquibase().update(SCHEMA_ONLY, new LabelExpression());
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
        liquibase.update(SCHEMA_ONLY, new LabelExpression());
        insertRecords();
        liquibase.rollback(4, SCHEMA_ONLY, new LabelExpression());

        assertEquals("0", scalar("SELECT count(*) FROM databasechangelog"));
        assertEquals("0", scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema = '"
                + schema + "' AND table_name IN ('users', 'farm', 'worker', 'admin_profile', 'work_logs', 'audit_logs', 'salary_advances')"));
        liquibase.update(SCHEMA_ONLY, new LabelExpression());
        insertRecords();
        assertEquals("1", scalar("SELECT count(*) FROM work_logs"));
    }

    @Test
    void applicationStartsWithLiquibaseSeedingAndHibernateSchemaValidation() throws Exception {
        try (var context = new SpringApplicationBuilder(BackendApplication.class).run(
                "--server.port=0",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--spring.datasource.hikari.schema=" + schema,
                "--spring.liquibase.default-schema=" + schema,
                "--spring.jpa.properties.hibernate.default_schema=" + schema,
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.liquibase.enabled=true",
                "--spring.liquibase.contexts=production",
                "--app.seed.enabled=true",
                "--app.seed.demo-data=true",
                "--app.bootstrap-admin-password=integration-test-password",
                "--app.migration-job=false",
                "--app.e2e.support-key=integration-test-support-key-at-least-32-characters",
                "--jwt.secret=integration-test-jwt-secret-at-least-32-characters",
                "--spring.jpa.show-sql=false",
                "--logging.level.org.springframework.security=INFO")) {
            assertTrue(context.isActive());
            assertEquals("4", scalar("SELECT count(*) FROM users"));
            assertEquals("4", scalar("SELECT count(*) FROM work_logs"));
            verifyAdvanceApiAndBalance(context);
            verifyAdminCannotAccessAnotherFarmsFinances(context);
            verifySharedE2e(context);
        }
    }

    private void verifyAdvanceApiAndBalance(org.springframework.context.ConfigurableApplicationContext context)
            throws Exception {
        var jdbc = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        long workerId = jdbc.queryForObject("SELECT worker.id FROM worker JOIN users ON worker.user_id=users.id "
                + "WHERE users.username='worker1'", Long.class);
        int port = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context)
                .getWebServer().getPort();
        String base = "http://localhost:" + port + "/api";
        var client = java.net.http.HttpClient.newHttpClient();
        var mapper = new tools.jackson.databind.json.JsonMapper();
        var login = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"superadmin\",\"password\":\"integration-test-password\"}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode(), login.body());
        String token = mapper.readTree(login.body()).get("token").asText();
        var before = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/worker-balances/" + workerId))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, before.statusCode(), before.body());
        var previousAdvances = new java.math.BigDecimal(
                mapper.readTree(before.body()).get("totalAdvances").asText());
        var create = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/salary-advances"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"workerId\":" + workerId + ",\"advanceDate\":\"2026-10-27\","
                        + "\"type\":\"GOODS\",\"amount\":\"200.00\",\"itemName\":\"fertilizer\"}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(201, create.statusCode(), create.body());
        long advanceId = mapper.readTree(create.body()).get("id").asLong();
        var afterCreate = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/worker-balances/" + workerId))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, afterCreate.statusCode(), afterCreate.body());
        assertEquals(0, previousAdvances.add(new java.math.BigDecimal("200.00"))
                .compareTo(new java.math.BigDecimal(
                        mapper.readTree(afterCreate.body()).get("totalAdvances").asText())));
        var cancel = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/salary-advances/" + advanceId))
                .header("Authorization", "Bearer " + token).DELETE().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cancel.statusCode(), cancel.body());
        var afterCancel = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/worker-balances/" + workerId))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, afterCancel.statusCode(), afterCancel.body());
        assertEquals(0, previousAdvances.compareTo(new java.math.BigDecimal(
                mapper.readTree(afterCancel.body()).get("totalAdvances").asText())));

        var workerLogin = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"worker1\",\"password\":\"1234\"}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, workerLogin.statusCode(), workerLogin.body());
        String workerToken = mapper.readTree(workerLogin.body()).get("token").asText();
        var self = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/worker-balances/me"))
                .header("Authorization", "Bearer " + workerToken).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, self.statusCode(), self.body());
        assertEquals(workerId, mapper.readTree(self.body()).get("workerId").asLong());
        var forbidden = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/salary-advances"))
                .header("Authorization", "Bearer " + workerToken).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(403, forbidden.statusCode(), forbidden.body());
    }

    private void verifyAdminCannotAccessAnotherFarmsFinances(
            org.springframework.context.ConfigurableApplicationContext context) throws Exception {
        var jdbc = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        long farmId = jdbc.queryForObject("INSERT INTO farm(name,location) VALUES ('Other farm','test') RETURNING id",
                Long.class);
        long workerId = jdbc.queryForObject("INSERT INTO worker(full_name,nickname,farm_id,status) "
                + "VALUES ('Other worker','other',?,'active') RETURNING id", Long.class, farmId);
        long advanceId = jdbc.queryForObject("INSERT INTO salary_advances(worker_id,advance_date,type,amount,recorded_by) "
                + "VALUES (?,CURRENT_DATE,'CASH',50,'superadmin') RETURNING id", Long.class, workerId);
        int port = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context)
                .getWebServer().getPort();
        String base = "http://localhost:" + port + "/api";
        var client = java.net.http.HttpClient.newHttpClient();
        var mapper = new tools.jackson.databind.json.JsonMapper();
        var login = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"admin1\",\"password\":\"1234\"}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode(), login.body());
        String token = mapper.readTree(login.body()).get("token").asText();
        var ownList = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/salary-advances?page=0&size=10"))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ownList.statusCode(), ownList.body());
        for (var advance : mapper.readTree(ownList.body())) {
            assertNotEquals(advanceId, advance.get("id").asLong());
        }
        var balance = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/worker-balances/" + workerId))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(403, balance.statusCode(), balance.body());
        var cancel = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/salary-advances/" + advanceId))
                .header("Authorization", "Bearer " + token).DELETE().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(403, cancel.statusCode(), cancel.body());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM salary_advances "
                + "WHERE id=? AND cancelled_at IS NOT NULL", Long.class, advanceId));
    }

    private void verifySharedE2e(org.springframework.context.ConfigurableApplicationContext context) throws Exception {
        var runs = context.getBean(com.aomaoi.backend.service.E2eRunService.class);
        String key = "integration-test-support-key-at-least-32-characters";
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> runs.create("wrong"));
        var run = runs.create(key);
        var other = runs.create(key);
        var manager = context.getBean(org.springframework.transaction.PlatformTransactionManager.class);
        var tx = new org.springframework.transaction.support.TransactionTemplate(manager);
        var jdbc = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        long realFarmId = jdbc.queryForObject("SELECT min(id) FROM farm WHERE e2e_run_id IS NULL", Long.class);
        int port = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context)
                .getWebServer().getPort();
        String base = "http://localhost:" + port + "/api";
        var client = java.net.http.HttpClient.newHttpClient();
        var mapper = new tools.jackson.databind.json.JsonMapper();
        var login = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-E2E-Run", run.get("id")).header("X-E2E-Token", run.get("token"))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"" + run.get("username") + "\",\"password\":\"" + run.get("password") + "\"}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode(), login.body());
        String jwt = mapper.readTree(login.body()).get("token").asText();
        var create = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/farms"))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwt)
                .header("X-E2E-Run", run.get("id")).header("X-E2E-Token", run.get("token"))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"name\":\"E2E farm\",\"location\":\"test\"}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, create.statusCode(), create.body());
        long farmId = mapper.readTree(create.body()).get("id").asLong();
        assertEquals(run.get("id"), jdbc.queryForObject("SELECT e2e_run_id FROM farm WHERE id=?", String.class, farmId));
        var blocked = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/farms/" + realFarmId))
                .header("Authorization", "Bearer " + jwt)
                .header("X-E2E-Run", run.get("id")).header("X-E2E-Token", run.get("token"))
                .DELETE().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertTrue(blocked.statusCode() >= 400, blocked.body());
        var unguarded = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/farms"))
                .header("Authorization", "Bearer " + jwt).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(403, unguarded.statusCode());
        assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(status -> {
            runs.enter(run.get("id"), run.get("token"));
            jdbc.update("INSERT INTO worker(farm_id) VALUES (?)", realFarmId);
        }));
        assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(status -> {
            runs.enter(other.get("id"), other.get("token"));
            jdbc.update("DELETE FROM farm WHERE id=?", farmId);
        }));
        tx.executeWithoutResult(status -> {
            runs.enter(run.get("id"), run.get("token"));
            jdbc.update("UPDATE farm SET name='renamed without e2e prefix' WHERE id=?", farmId);
            jdbc.update("INSERT INTO users(username,password,role) VALUES ('e2e_fixture_worker','hash','worker')");
            long userId = jdbc.queryForObject("SELECT id FROM users WHERE username='e2e_fixture_worker'", Long.class);
            jdbc.update("INSERT INTO worker(user_id,farm_id) VALUES (?,?)", userId, farmId);
            long workerId = jdbc.queryForObject("SELECT id FROM worker WHERE user_id=?", Long.class, userId);
            jdbc.update("INSERT INTO work_logs(worker_id,type,work_date,start_date,end_date,total) "
                    + "VALUES (?,'cutting',CURRENT_DATE,CURRENT_DATE,CURRENT_DATE,100)", workerId);
            jdbc.update("INSERT INTO salary_advances(worker_id,advance_date,type,amount,recorded_by) "
                    + "VALUES (?,CURRENT_DATE,'CASH',20,?)", workerId, run.get("username"));
            jdbc.update("INSERT INTO audit_logs(action,performed_by,target_user,timestamp) "
                    + "VALUES ('TEST',?,'fixture',now())", run.get("username"));
        });
        assertEquals(0, runs.cleanup(run.get("id"), key).get("remaining"));
        assertEquals(0, runs.cleanup(run.get("id"), key).get("remaining")); // retry is harmless
        assertEquals("4", scalar("SELECT count(*) FROM work_logs")); // original rows untouched
        assertEquals("2", scalar("SELECT count(*) FROM salary_advances")); // real records remain as history
        assertEquals("0", scalar("SELECT count(*) FROM salary_advances WHERE e2e_run_id IS NOT NULL"));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM e2e_runs WHERE id=?", Long.class, other.get("id")));
        jdbc.update("UPDATE e2e_runs SET expires_at=now()-interval '1 minute' WHERE id=?", other.get("id"));
        runs.cleanupExpired();
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM e2e_runs", Long.class));
        assertEquals("4", scalar("SELECT count(*) FROM users"));
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
        execute("INSERT INTO salary_advances(worker_id,advance_date,type,amount,recorded_by)"
                + " VALUES (1,CURRENT_DATE,'CASH',5.00,'admin')");
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
