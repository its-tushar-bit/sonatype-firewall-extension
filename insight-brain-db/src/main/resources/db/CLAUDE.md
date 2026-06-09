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

## References

- [SaaS Friendly IQ Database Migrations](https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368)
- [SaaS Friendly IQ Database Migrations - Details](https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/59703757)
