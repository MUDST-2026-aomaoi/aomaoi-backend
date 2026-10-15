# Database migrations and Supabase

The backend uses Spring Boot's Liquibase changelog at
`src/main/resources/db/changelog/db.changelog-master.yaml`. It is the only migration
entry point. JPA/Hibernate maps entities; Liquibase owns schema changes.

Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` from the Supabase project's Connect panel. Use a
separate Supabase project for development/testing and production. Do not run test
suites or demo seeding against production data.

Docker Compose starts the backend and frontend and connects to the configured
Supabase database. The Kubernetes migration Job runs the same Spring Boot
Liquibase changelog before the API deployment. No database contents are exported
or synchronized through Git commits.

The `009-normalize-farm-ids` changeset converts worker/admin farm references to
`BIGINT`, validates farm relationships, and adds lookup indexes. It preserves
existing IDs. Back up the Supabase project before first applying this migration;
it will fail without changing the columns if it finds non-numeric or missing farm
references, which must be corrected before retrying.

CI migration integration tests use a disposable PostgreSQL service and create
temporary schemas. To run them locally, point the variables below at a dedicated
test database, never a database containing data you need:

```powershell
$env:AOMAOI_MIGRATION_TEST_URL = 'jdbc:postgresql://localhost:5432/aomaoi_test'
$env:AOMAOI_MIGRATION_TEST_USER = 'test_user'
$env:AOMAOI_MIGRATION_TEST_PASSWORD = 'test_password'
.\mvnw.cmd '-Dtest=FarmIdMigrationIntegrationTest' test
```
