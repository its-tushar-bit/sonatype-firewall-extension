# Database Migration Guidelines

## Execution Time Constraints

In the MTIQ deployment model, database migrations run in an init container before the application container starts, the ECS service has a health check grace period computed as:

```
grace_period = start_period + ((retries + 1) * interval)
```

See the [terraform-sca-cloud ECS module](https://github.com/sonatype/terraform-sca-cloud/tree/main/src/infrastructure/aws/cell/modules/ecs) for current configuration values.

If migrations exceed this window, the task is killed and never reaches a healthy state — causing failed deployments and production incidents.

**Rule: No migration may perform bulk DML (UPDATE, DELETE, INSERT...SELECT) on tables with more than 100,000 rows.**

### Tables Known to Be Large (>100K rows)

The following tables are known to grow large (examples from SaaS deployments; this list is not exhaustive):

- `aggregate_file`
- `policy_evaluation`
- `policy_violation`
- `application_component_license`
- `application_component`
- `policy_violation_aggregation`
- `sbom_metadata`

**Important:** If your migration touches a table not listed here, verify its size before proceeding. Ask the PR creator if the table might be large.

## What To Do Instead: Backfill Patterns

For large-table backfills, use one of these approaches:

### Option A: Application-Level Backfill

For very large tables or ongoing backfills:

1. Add application code that backfills lazily (on read) or via a scheduled job
2. The migration only adds the new column (nullable) or makes no schema changes — no DML
3. Remove the backfill code after it's been deployed long enough to cover all rows

**Reference implementation:** `PolicyWaiverTelemetryBackfillService` — application-level backfill using a scheduled task.

### Option B: AsyncDbMigration

For index creation on large tables, use `AbstractAsyncDbMigration` to run outside the migration lock:

1. Extend `AbstractAsyncDbMigration` and override `executeMigration()`
2. Run DDL on its own connection with `autoCommit=true`
3. Migration runs after startup via `AsyncDbMigrationScheduler`

**Reference implementation:** `PolicyViolationIndexAsyncDbMigration` — creates partial index using `CREATE INDEX CONCURRENTLY` outside the migration lock.

## Checklist Before Merging a Migration

Human reviewers should verify:

- [ ] Does it touch a large table? Check known large tables above. If the table is not listed, ask the PR creator about expected row count. If >100K rows, use application-level backfill or AsyncDbMigration.
- [ ] Estimated execution time extrapolated from a table matching the largest observed size?
- [ ] If adding NOT NULL to existing column: backfill plan documented?
- [ ] If touching a large table: **2 approvals required** (see `.claude/agents/code-reviewer.md` escalation policy)

## Index Creation on Large Tables

**`CREATE INDEX CONCURRENTLY` cannot be used in incremental migration scripts** — it deadlocks with `ClusterLockManager`. Use `AbstractAsyncDbMigration` instead (see Option B above).

See `doc/devdocs/concurrent-index-creation.md` for full details on why this happens and how to work around it.

## Quartz Persisted-Job Renames and Removals

Quartz persists job class names in `qrtz_job_details.JOB_CLASS_NAME`. When the scheduler boots it calls `JobStoreSupport.recoverJobs()` which `Class.forName()`s every persisted job class — if the class has been renamed, repackaged, or removed, the call throws `ClassNotFoundException` and Spring fatal-fails on `defaultTenantManagedInitializer`. The misfire handler hits the same hazard at runtime once per minute.

**Rule: any PR that renames, repackages, or removes a Quartz `Job` class (anything implementing `org.quartz.Job`, including `MtiqBatchJob` subclasses) MUST ship a paired `schema_incremental_*.sql` deleting the corresponding rows from the Quartz tables.**

This applies to:
- A class deleted outright (e.g. PR #16360 removed `HostedRepositoryMonitoringTask` and shipped `schema_incremental_0468.sql` to clean it up).
- A class moved to a new package (FQCN change → `Class.forName` fails).
- A class renamed (FQCN change → `Class.forName` fails).

### Migration template

Mirror `schema_incremental_0468.sql`. Delete in FK-safe order — child trigger tables first, then `qrtz_triggers`, then `qrtz_job_details`. The trigger name is usually the simple class name; the job name matches.

```sql
-- SaaS Compatible
-- CLM-XXXXX: Remove Quartz rows for renamed/removed <ClassName> on upgrade.
DELETE FROM QRTZ_SIMPLE_TRIGGERS  WHERE TRIGGER_NAME = '<JobName>';
DELETE FROM QRTZ_CRON_TRIGGERS    WHERE TRIGGER_NAME = '<JobName>';
DELETE FROM QRTZ_BLOB_TRIGGERS    WHERE TRIGGER_NAME = '<JobName>';
DELETE FROM QRTZ_SIMPROP_TRIGGERS WHERE TRIGGER_NAME = '<JobName>';
DELETE FROM QRTZ_FIRED_TRIGGERS   WHERE JOB_NAME     = '<JobName>';
DELETE FROM QRTZ_TRIGGERS         WHERE JOB_NAME     = '<JobName>';
DELETE FROM QRTZ_JOB_DETAILS      WHERE JOB_NAME     = '<JobName>';
```

### Reviewer checklist for Quartz job changes

- [ ] If the PR deletes / renames / repackages a class that implements `org.quartz.Job`: is there a paired `schema_incremental_*.sql` cleaning the Quartz tables?
- [ ] Does the cleanup migration delete from all 7 trigger / job tables in FK-safe order?
- [ ] Has the PR description called out which `JOB_NAME` values the migration targets?

This applies regardless of whether the affected class was scheduled directly or via the abstract framework — Quartz only cares about `JOB_CLASS_NAME` strings, not how they got there.

## References

- [SaaS Friendly IQ Database Migrations](https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368)
- [SaaS Friendly IQ Database Migrations - Details](https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/59703757)
