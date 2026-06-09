# SQL SaaS Compatibility Rules

All SQL migration files MUST be backwards-compatible with the previously deployed schema. In the MTIQ deployment model, database migrations run at ECS task startup.

## Safe Operations (can be done in a single deployment)

- Adding a new table
- Adding a new column that is **nullable** or has a **default value**
- Adding an index (use `IF NOT EXISTS` and `CONCURRENTLY` where supported; note that `CONCURRENTLY` cannot be used in incremental migration scripts — see `doc/devdocs/concurrent-index-creation.md` for details)

## Unsafe Operations: Bulk DML on Large Tables

**NEVER run UPDATE, DELETE, or INSERT...SELECT on tables with >100K rows in a migration changeset — even with WHERE clauses.**

Migrations run at ECS task startup in an init container. The ECS health check grace period has a limited window (computed from `start_period + ((retries + 1) * interval)`). Long-running DML exceeding this window will prevent deployment, causing production incidents.

See `insight-brain-db/src/main/resources/db/CLAUDE.md` for safe alternatives (application-level backfill, AsyncDbMigration).

### Known Large Tables (>100K rows)

The following tables are known to grow large (examples from SaaS deployments; this list is not exhaustive):

`aggregate_file`, `policy_evaluation`, `policy_violation`, `application_component_license`, `application_component`, `policy_violation_aggregation`, `sbom_metadata`

**Important:** If your migration touches a table not listed here, verify its size before proceeding. Ask the PR creator if the table might be large.

## Unsafe Operations (require multi-step migration across multiple deployments)

- **Renaming** a table, column, or view
- **Removing** a column (keep nullable for one release, remove in next)
- **Changing** a column type (add new column, migrate, remove old)
- **Adding a NOT NULL constraint** to an existing column (first add as nullable, backfill, then add constraint)

## References

- [SaaS Friendly IQ Database Migrations](https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368)
- [SaaS Friendly IQ Database Migrations - Details](https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/59703757)
