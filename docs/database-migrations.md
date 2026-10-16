# PostgreSQL database and migrations

The backend uses PostgreSQL 15 and Liquibase. The single migration entry point is
`src/main/resources/db/changelog/db.changelog-master.yaml`, which includes
`changes/001-initial-schema.sql`. The initial migration creates all six application
tables, identity columns, foreign keys, unique constraints, required work dates,
and lookup indexes. Farm references are `BIGINT` from the start.

This is a fresh-database baseline. Start with an empty PostgreSQL database; old
database contents and previous migration histories are not imported or upgraded.
Hibernate uses `ddl-auto=validate` to check mappings after Liquibase runs.

## Docker Compose

```bash
cp .env.example .env
# Set DB_PASSWORD, JWT_SECRET, and BOOTSTRAP_ADMIN_PASSWORD in .env.
docker compose up --build -d postgres migrate app
docker compose logs migrate
```

PostgreSQL is exposed at `localhost:5434` by default. The API is at
`http://localhost:8081`. The `migrate` service waits for PostgreSQL to become
healthy, runs the changelog, bootstraps the initial superadmin, and exits. The API
waits for that service to succeed, then starts with schema validation enabled.
Start the optional frontend with `docker compose up -d frontend` when the sibling
frontend repository is available.

Data lives in the named volume `aomaoi_postgres_data` across container restarts.
For later schema changes, rebuild and rerun `docker compose up --build -d migrate app`.
Liquibase records completed changesets in `databasechangelog` and applies only new
ones. PostgreSQL initialization variables apply when its volume is first created;
changing `.env` does not change credentials in an initialized database.

## Running Java directly

Start PostgreSQL using `docker compose up -d postgres`, then export the values in
`.env` before starting the backend (Spring Boot does not load `.env` itself):

```bash
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

The host JDBC URL, username, and password must match the `DB_*` values if you
customize them. With a separately managed PostgreSQL server, set
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` for that database. Liquibase runs on application
startup by default. `APP_SEED_ENABLED=true` creates the superadmin account;
`APP_SEED_DEMO_DATA=true` additionally enables development sample data.

## Kubernetes

`scripts/k8s-deploy.sh` creates PostgreSQL credentials once, starts the persistent
StatefulSet, waits for readiness, runs the same changelog in `aomaoi-migrate`, then
deploys the API. API Pods disable Liquibase and seeding and validate the schema.
See [Kubernetes setup](../k8s/README.md).

## Adding migrations and rollback

After the baseline has been applied, keep it unchanged. Add the next numbered
Liquibase formatted SQL file under `changes/` and include it at the end of the
master YAML. Each file needs a unique changeset ID and explicit `--rollback`
statements in dependency order. Use transactional PostgreSQL DDL for atomic
changes. Keep sample data in the optional application seeder.

The initial migration supports Liquibase rollback by dropping application tables
in reverse dependency order. Rolling it back deletes all application records;
use it only for disposable databases. Never edit `databasechangelog` manually to
force a migration to run again.

## Verification

Run unit tests with `./mvnw test`. To include migration integration tests, configure
a dedicated PostgreSQL test database:

```bash
export AOMAOI_MIGRATION_TEST_URL=jdbc:postgresql://localhost:5432/aomaoi_test
export AOMAOI_MIGRATION_TEST_USER=test_user
export AOMAOI_MIGRATION_TEST_PASSWORD=test_password
./mvnw -Dtest=PostgreSqlMigrationIntegrationTest test
```

Tests create and remove their own temporary schemas. They check fresh schema
creation, repeat updates preserving data, constraints, rollback/recreation, and
full Spring Boot startup with Hibernate validation. CI runs them against a
disposable PostgreSQL 15 service.
