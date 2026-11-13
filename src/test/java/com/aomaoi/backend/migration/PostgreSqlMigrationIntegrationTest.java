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
        assertEquals("7", scalar("SELECT count(*) FROM databasechangelog"));
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
        assertThrows(SQLException.class, () -> execute("INSERT INTO salary_advances(worker_id,advance_date,type,amount,item_name,recorded_by)"
                + " VALUES (1,CURRENT_DATE,'CASH',5,'fertilizer','admin')"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO salary_advances(worker_id,advance_date,type,amount,item_name,recorded_by)"
                + " VALUES (1,CURRENT_DATE,'GOODS',5,repeat('x',256),'admin')"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO salary_advances(worker_id,advance_date,type,amount,recorded_by,cancelled_at)"
                + " VALUES (1,CURRENT_DATE,'CASH',5,'admin',CURRENT_TIMESTAMP)"));
        assertThrows(SQLException.class, () -> execute("UPDATE worker SET farm_id = NULL WHERE id = 1"));
        assertThrows(SQLException.class, () -> execute("DELETE FROM farm WHERE id = 1"));
        assertThrows(SQLException.class, () -> execute("DELETE FROM worker WHERE id = 1"));
    }

    @Test
    void workerFarmCanChangeOnlyBeforeFinancialHistoryExists() throws Exception {
        liquibase().update(SCHEMA_ONLY, new LabelExpression());
        insertRecords();
        long secondFarmId = scalarLong(
                "INSERT INTO farm(name,location) VALUES ('Second farm','test') RETURNING id");
        long noHistoryWorkerId = scalarLong("INSERT INTO worker(full_name,nickname,farm_id,status) "
                + "VALUES ('Movable worker','movable',1,'active') RETURNING id");

        execute("UPDATE worker SET farm_id = " + secondFarmId + " WHERE id = " + noHistoryWorkerId);
        assertEquals(Long.toString(secondFarmId), scalar("SELECT farm_id FROM worker WHERE id = " + noHistoryWorkerId));
        assertSqlFailureAndRecover("UPDATE worker SET farm_id = " + secondFarmId + " WHERE id = 1");

        long advanceOnlyWorkerId = scalarLong("INSERT INTO worker(full_name,nickname,farm_id,status) "
                + "VALUES ('Advance worker','advance',1,'active') RETURNING id");
        execute("INSERT INTO salary_advances(worker_id,advance_date,type,amount,recorded_by) "
                + "VALUES (" + advanceOnlyWorkerId + ",CURRENT_DATE,'CASH',10,'admin')");
        assertSqlFailureAndRecover(
                "UPDATE worker SET farm_id = " + secondFarmId + " WHERE id = " + advanceOnlyWorkerId);
    }

    @Test
    void dailySummaryLogIsUniquePerAdminAndDayAndCascadesWithTheAdmin() throws Exception {
        liquibase().update(SCHEMA_ONLY, new LabelExpression());
        insertRecords();

        assertEquals("character varying", scalar("SELECT data_type FROM information_schema.columns WHERE table_schema = '"
                + schema + "' AND table_name = 'admin_profile' AND column_name = 'email'"));
        long adminId = scalarLong("SELECT id FROM admin_profile LIMIT 1");
        execute("INSERT INTO daily_summary_log(admin_id, summary_date, status) VALUES (" + adminId + ", DATE '2026-11-12', 'SENDING')");

        assertSqlFailureAndRecover("INSERT INTO daily_summary_log(admin_id, summary_date, status) VALUES ("
                + adminId + ", DATE '2026-11-12', 'SENDING')");
        // ON CONFLICT DO NOTHING is what the scheduler relies on to let only one replica send
        assertEquals("0", scalar("WITH ins AS (INSERT INTO daily_summary_log(admin_id, summary_date, status) VALUES ("
                + adminId + ", DATE '2026-11-12', 'SENDING') ON CONFLICT (admin_id, summary_date) DO NOTHING RETURNING 1) "
                + "SELECT count(*) FROM ins"));
        assertSqlFailureAndRecover("INSERT INTO daily_summary_log(admin_id, summary_date, status) VALUES ("
                + adminId + ", DATE '2026-11-13', 'DONE')");

        execute("DELETE FROM admin_profile WHERE id = " + adminId);
        assertEquals("0", scalar("SELECT count(*) FROM daily_summary_log"));
    }

    @Test
    void attendanceIsUniquePerWorkerAndDayRestrictedToKnownStatusesAndLocksTheWorkersFarm() throws Exception {
        liquibase().update(SCHEMA_ONLY, new LabelExpression());
        insertRecords();
        long secondFarmId = scalarLong("INSERT INTO farm(name,location) VALUES ('Second farm','test') RETURNING id");
        long freshWorker = scalarLong("INSERT INTO worker(full_name,nickname,farm_id,status) "
                + "VALUES ('Fresh','fresh',1,'active') RETURNING id");

        execute("INSERT INTO attendance(worker_id, attendance_date, status, recorded_by) "
                + "VALUES (" + freshWorker + ", DATE '2026-11-12', 'PRESENT', 'admin')");
        assertSqlFailureAndRecover("INSERT INTO attendance(worker_id, attendance_date, status, recorded_by) "
                + "VALUES (" + freshWorker + ", DATE '2026-11-12', 'ABSENT', 'admin')");
        assertSqlFailureAndRecover("INSERT INTO attendance(worker_id, attendance_date, status, recorded_by) "
                + "VALUES (" + freshWorker + ", DATE '2026-11-13', 'HOLIDAY', 'admin')");
        assertSqlFailureAndRecover("INSERT INTO attendance(worker_id, attendance_date, status, recorded_by) "
                + "VALUES (9999, DATE '2026-11-13', 'PRESENT', 'admin')");
        // history belongs to the farm where it was recorded
        assertSqlFailureAndRecover("UPDATE worker SET farm_id = " + secondFarmId + " WHERE id = " + freshWorker);
        // a worker with attendance cannot be deleted either (history is kept)
        assertSqlFailureAndRecover("DELETE FROM worker WHERE id = " + freshWorker);
    }

    @Test
    void closeDayRecordsUncheckedWorkersAndLeavesDaysOffAlone() throws Exception {
        liquibase().update(SCHEMA_ONLY, new LabelExpression());
        execute("INSERT INTO farm(name) VALUES ('Test farm'), ('Quiet farm')");
        long checked = worker("Checked", 1, "active", "2026-01-01");
        long worked = worker("Worked", 1, "active", "2026-01-01");
        long idle = worker("Idle", 1, "pending", "2026-01-01");
        long inactive = worker("Inactive", 1, "inactive", "2026-01-01");
        long joinedLater = worker("Later", 1, "active", "2026-11-20");
        // farm 2: nobody checked and nothing logged -> a day off
        long quiet = worker("Quiet", 2, "active", "2026-01-01");

        execute("INSERT INTO attendance(worker_id, attendance_date, status, note, recorded_by) VALUES ("
                + checked + ", DATE '2026-11-12', 'LEAVE', 'sick', 'admin')");
        // a multi-day watering log covers the day even though it started earlier
        execute("INSERT INTO work_logs(worker_id, type, work_date, start_date, end_date, total) VALUES ("
                + worked + ", 'watering', DATE '2026-11-10', DATE '2026-11-10', DATE '2026-11-13', 100)");

        var service = closeDayService();
        assertEquals(2, service.closeAllFarms(java.time.LocalDate.of(2026, 11, 12)));

        assertEquals("LEAVE", status(checked), "an Admin's choice is never overwritten");
        assertEquals("PRESENT", status(worked));
        assertEquals("ABSENT", status(idle));
        assertEquals("0", scalar("SELECT count(*) FROM attendance WHERE worker_id IN ("
                + inactive + "," + joinedLater + "," + quiet + ")"));
        assertEquals("t", scalar("SELECT auto_recorded FROM attendance WHERE worker_id = " + idle));
        assertEquals("system", scalar("SELECT recorded_by FROM attendance WHERE worker_id = " + idle));
        assertEquals("f", scalar("SELECT auto_recorded FROM attendance WHERE worker_id = " + checked));

        // running it again (another replica, a later 15-minute tick) adds nothing
        assertEquals(0, service.closeAllFarms(java.time.LocalDate.of(2026, 11, 12)));
        assertEquals("3", scalar("SELECT count(*) FROM attendance"));
    }

    @Test
    void leaveRegisteredInAdvanceDoesNotTurnADayOffIntoAWorkingDay() throws Exception {
        liquibase().update(SCHEMA_ONLY, new LabelExpression());
        execute("INSERT INTO farm(name) VALUES ('Test farm')");
        long planned = worker("Planned", 1, "active", "2026-01-01");
        worker("Other", 1, "active", "2026-01-01");
        execute("INSERT INTO attendance(worker_id, attendance_date, status, note, recorded_by) VALUES ("
                + planned + ", DATE '2026-11-12', 'LEAVE', 'ลาล่วงหน้า', 'admin')");

        // only a leave and no work log: still a day off, nobody is marked absent
        assertEquals(0, closeDayService().closeAllFarms(java.time.LocalDate.of(2026, 11, 12)));
        assertEquals("1", scalar("SELECT count(*) FROM attendance"));
    }

    @Test
    void closeDaySkipsWorkersOwnedByAnE2eRun() throws Exception {
        liquibase().update(SCHEMA_ONLY, new LabelExpression());
        execute("INSERT INTO farm(name) VALUES ('Test farm')");
        long regular = worker("Regular", 1, "active", "2026-01-01");
        // an E2E run owns its own farm and workers; its triggers tag rows from the session setting
        execute("INSERT INTO e2e_runs(id, token_hash, expires_at) VALUES ('run-1', 'hash', now() + interval '1 hour')");
        execute("SELECT set_config('aomaoi.e2e_run', 'run-1', false)");
        long ownedFarm = scalarLong("INSERT INTO farm(name) VALUES ('E2E farm') RETURNING id");
        long owned = worker("Owned", ownedFarm, "active", "2026-01-01");
        execute("SELECT set_config('aomaoi.e2e_run', '', false)");
        execute("INSERT INTO work_logs(worker_id, type, work_date, start_date, end_date, total) VALUES ("
                + regular + ", 'cutting', DATE '2026-11-12', DATE '2026-11-12', DATE '2026-11-12', 10)");

        execute("SELECT set_config('aomaoi.e2e_run', 'run-1', false)");
        execute("INSERT INTO work_logs(worker_id, type, work_date, start_date, end_date, total) VALUES ("
                + owned + ", 'cutting', DATE '2026-11-12', DATE '2026-11-12', DATE '2026-11-12', 10)");
        execute("SELECT set_config('aomaoi.e2e_run', '', false)");

        // only the regular farm is closed; the E2E farm's worker is left to its own run
        assertEquals(1, closeDayService().closeAllFarms(java.time.LocalDate.of(2026, 11, 12)));
        assertEquals("0", scalar("SELECT count(*) FROM attendance WHERE worker_id = " + owned));
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
        liquibase.rollback(7, SCHEMA_ONLY, new LabelExpression());

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
        var dashboardBefore = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/financial-dashboard"))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, dashboardBefore.statusCode(), dashboardBefore.body());
        var dashboardBeforeBody = mapper.readTree(dashboardBefore.body());
        var previousCashAdvances = new java.math.BigDecimal(dashboardBeforeBody.get("cashAdvances").asText());
        var previousGoodsAdvances = new java.math.BigDecimal(dashboardBeforeBody.get("goodsAdvances").asText());
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
        var cashCreate = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/salary-advances"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"workerId\":" + workerId + ",\"advanceDate\":\"2026-10-27\","
                                + "\"type\":\"CASH\",\"amount\":\"75.00\"}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(201, cashCreate.statusCode(), cashCreate.body());
        long cashAdvanceId = mapper.readTree(cashCreate.body()).get("id").asLong();

        var dashboard = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/financial-dashboard"))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, dashboard.statusCode(), dashboard.body());
        var dashboardBody = mapper.readTree(dashboard.body());
        assertTrue(dashboardBody.get("workerCount").asLong() > 0);
        assertEquals(0, previousGoodsAdvances.add(new java.math.BigDecimal("200.00")).compareTo(
                new java.math.BigDecimal(dashboardBody.get("goodsAdvances").asText())));
        assertEquals(0, previousCashAdvances.add(new java.math.BigDecimal("75.00")).compareTo(
                new java.math.BigDecimal(dashboardBody.get("cashAdvances").asText())));
        assertEquals(0, previousCashAdvances.add(previousGoodsAdvances)
                .add(new java.math.BigDecimal("275.00")).compareTo(
                new java.math.BigDecimal(dashboardBody.get("totalAdvances").asText())));

        var invalidAdvance = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/salary-advances"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"workerId\":" + workerId + ",\"advanceDate\":\"2026-10-27\","
                                + "\"type\":\"CASH\",\"amount\":\"0.00\"}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(400, invalidAdvance.statusCode(), invalidAdvance.body());

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
        assertEquals(0, previousAdvances.add(new java.math.BigDecimal("75.00")).compareTo(
                new java.math.BigDecimal(mapper.readTree(afterCancel.body()).get("totalAdvances").asText())));
        var cancelCash = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/salary-advances/" + cashAdvanceId))
                .header("Authorization", "Bearer " + token).DELETE().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cancelCash.statusCode(), cancelCash.body());
        var afterAllCancelled = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/worker-balances/" + workerId))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, afterAllCancelled.statusCode(), afterAllCancelled.body());
        assertEquals(0, previousAdvances.compareTo(new java.math.BigDecimal(
                mapper.readTree(afterAllCancelled.body()).get("totalAdvances").asText())));

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
        var dashboardForbidden = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/financial-dashboard"))
                .header("Authorization", "Bearer " + workerToken).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(403, dashboardForbidden.statusCode(), dashboardForbidden.body());
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
        var dashboard = client.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/financial-dashboard"))
                .header("Authorization", "Bearer " + token).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, dashboard.statusCode(), dashboard.body());
        long adminFarmId = jdbc.queryForObject("SELECT ap.farm_id FROM admin_profile ap "
                + "JOIN users u ON u.id=ap.user_id WHERE u.username='admin1'", Long.class);
        java.math.BigDecimal expectedOwnFarmAdvances = jdbc.queryForObject(
                "SELECT COALESCE(SUM(a.amount),0) FROM salary_advances a "
                        + "JOIN worker w ON w.id=a.worker_id WHERE w.farm_id=? AND a.cancelled_at IS NULL",
                java.math.BigDecimal.class, adminFarmId);
        assertEquals(0, expectedOwnFarmAdvances.compareTo(new java.math.BigDecimal(
                mapper.readTree(dashboard.body()).get("totalAdvances").asText())));
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
        assertEquals("3", scalar("SELECT count(*) FROM salary_advances")); // baseline plus cancelled API history
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

    private long worker(String name, long farmId, String status, String joined) throws SQLException {
        return scalarLong("INSERT INTO worker(full_name, nickname, farm_id, status, joined_date) VALUES ('" + name
                + "', '" + name + "', " + farmId + ", '" + status + "', DATE '" + joined + "') RETURNING id");
    }

    private String status(long workerId) throws SQLException {
        return scalar("SELECT status FROM attendance WHERE worker_id = " + workerId
                + " AND attendance_date = DATE '2026-11-12'");
    }

    private com.aomaoi.backend.service.AttendanceCloseDayService closeDayService() {
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(
                new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
        return new com.aomaoi.backend.service.AttendanceCloseDayService(jdbc,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                org.mockito.Mockito.mock(com.aomaoi.backend.service.AuditLogService.class));
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

    private long scalarLong(String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private void assertSqlFailureAndRecover(String sql) throws SQLException {
        boolean wasAutoCommit = connection.getAutoCommit();
        if (wasAutoCommit) {
            connection.setAutoCommit(false);
        }
        var savepoint = connection.setSavepoint();
        try {
            assertThrows(SQLException.class, () -> execute(sql));
        } finally {
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
            if (wasAutoCommit) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        }
    }
}
