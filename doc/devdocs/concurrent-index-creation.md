# CREATE INDEX CONCURRENTLY in Database Migrations

## The Problem

`CREATE INDEX CONCURRENTLY` cannot be used in incremental migration scripts (IQ and MTIQ).

Database migrations are protected by `ClusterLockManager.createForSchemaMigration()`, which holds a
PostgreSQL advisory lock (`pg_advisory_xact_lock`) inside an open transaction for the duration of all
migrations. `CREATE INDEX CONCURRENTLY` waits for all transactions that started before it to complete,
but the advisory lock transaction won't close until migrations finish. This creates a deadlock.

## Solution: AbstractAsyncDbMigration

Extend `AbstractAsyncDbMigration` and override `executeMigration()` to execute the DDL on its own
connection with `autoCommit=true` (CONCURRENTLY requires this because it cannot run inside an
explicit transaction block). These migrations run after startup via `AsyncDbMigrationScheduler`,
completely outside the migration lock.

See `PolicyViolationIndexAsyncDbMigration` for the reference implementation.

## Fresh installs / new tenants

`AsyncDbMigrationScheduler` is a one-shot job that runs on startup (on IQ, or on the batch pod in
MTIQ). New tenants created between restarts would not get the async migration until the next restart.

To ensure new tenants get the index at creation time:

1. For syntax compatible with both H2 and PostgreSQL, add the `CREATE INDEX` to `schema.sql`. If the
   syntax differs (e.g. partial indexes, `DESC` columns), use `schema_post_init_postgres.sql` and/or
   `schema_post_init_h2.sql` instead. Do not use `CONCURRENTLY` here - these scripts run during
   schema creation when the table is empty, so a regular `CREATE INDEX` is fast and sufficient.
2. Insert the migration tracker in `schema.sql` so the `AsyncDbMigration` is a no-op for new tenants:
   `INSERT INTO migration_tracker(migration_tracker_id) VALUES('<migration name>');`
