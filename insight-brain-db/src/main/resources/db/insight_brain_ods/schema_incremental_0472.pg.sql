-- SaaS Compatible
-- CLM-41005 (Postgres-only, migration 0472): NO-OP marker.
-- The covering composite index repository_component_dedup_keyset_idx is created by
-- RepositoryComponentDedupKeysetIndexAsyncDbMigration using CREATE INDEX CONCURRENTLY,
-- because repository_component is a large table (>100K rows) and CREATE INDEX
-- CONCURRENTLY cannot run inside the migration advisory-lock transaction.
-- See insight-brain-db/.claude/rules/sql-saas-compatibility.md and db/CLAUDE.md.
SELECT 1;
