# Numeric farm references

`farm.id`, `worker.farm_id`, and `admin_profile.farm_id` now use PostgreSQL
`BIGINT` and Java `Long`. API responses return numeric farm IDs. The frontend
keeps dropdown values as strings and converts them to numbers in API requests.
Existing numeric IDs and relationships are preserved; legacy NULL assignments
remain NULL. New admin accounts must reference an existing farm.

The new `009-normalize-farm-ids` changeset converts the existing text columns,
adds foreign keys to `farm(id)`, and indexes farm membership and worker/date
work-log lookups. Farm-filtered reads now query the database directly. Existing
soft deletes continue to work; physically deleting a referenced farm is rejected.
This improves membership integrity and query efficiency. It does not add pagination,
redesign wage counters, or make the snapshot into a distributed synchronization system.

## Apply with Docker Compose

Update frontend and backend together. Run these commands from `aomaoi-backend`
with Docker Desktop running:

```powershell
docker compose stop app frontend
docker compose run --rm liquibase
# Continue only if Liquibase succeeds.
docker compose up -d --build app frontend
```

Keep the PostgreSQL volume. Do not use `down -v` to apply this migration.
If migration fails, correct the reported invalid or missing farm references and
rerun Liquibase. Do not restart the new app against an unmigrated database.
The conversion locks and rewrites the two membership tables, so apply it while
the app is stopped. Larger future databases will need a separately planned online migration.

The Docker XML changelog and Spring Boot YAML changelog include the same SQL file
from `src/main/resources/db/changelog/changes/009-normalize-farm-ids.sql`.
Compose mounts those resources for the Liquibase service. Existing historical
changesets are unchanged. Keep using the same migration entry point for an existing
database; the older SQL and YAML baselines have separate migration histories.
For a Kubernetes Liquibase job, supply both the `liquibase` directory and
`src/main/resources` on the search path, or use the Spring Boot classpath changelog
for a new database.

## Commit/push snapshot compatibility

The existing pre-commit hook can keep exporting `999-latest-data.sql` with
`pg_dump --data-only --inserts --on-conflict-do-nothing`. No manual quote changes
to that generated file are needed. The schema migration runs BEFORE the snapshot
on fresh databases and upgrades. Numeric strings in old snapshots are accepted
by PostgreSQL's BIGINT columns; new dumps contain numeric values.

The farm foreign keys are `DEFERRABLE INITIALLY DEFERRED` because pg_dump may
insert admins before farms. Liquibase executes the snapshot changeset in one
transaction and validates those relationships when it commits. Manual imports
must also wrap the entire snapshot in a transaction (for example `psql -1`).
Missing farms cause import failure instead of leaving broken relationships.

The snapshot retains its existing limitations: conflicting rows are skipped rather
than updated, deletions are not propagated, IDs can collide across independently
edited databases, and dumped `setval` statements can rewind sequences on a database
with newer rows. Use it to seed a controlled database, not to merge independently
edited databases. These limitations are not fixed by converting ID types.

## Validation

Unit tests run normally with `./mvnw test`. Real PostgreSQL integration tests are
enabled explicitly and create/drop disposable schemas without touching application
tables. Prefer a dedicated test database:

```powershell
$env:AOMAOI_MIGRATION_TEST_URL = 'jdbc:postgresql://localhost:5434/aomaoi_test'
$env:AOMAOI_MIGRATION_TEST_USER = 'your_test_user'
$env:AOMAOI_MIGRATION_TEST_PASSWORD = 'your_test_password'
.\mvnw.cmd '-Dtest=FarmIdMigrationIntegrationTest' test
```

The migration CI workflow runs these against its PostgreSQL service. Coverage
includes legacy data, NULLs, invalid and out-of-range IDs, missing farms, current
snapshot import, numeric snapshots, indexes, both changelog entry points, repeated
updates, and rollback. The changeset rollback restores text columns; deploy the
matching older application if rolling the schema back. The older snapshot rollback
truncates data, so do not roll that snapshot back on a database whose data you need.
