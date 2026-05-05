-- SaaS Compatible
-- H2 does not support partial indexes. This index is only for PostgreSQL.
-- Using CONCURRENTLY to avoid blocking writes during index build on large tables.
-- Note: CONCURRENTLY requires no active transaction. Safety relies on:
--   - PostgreSQL JDBC driver defaults to autoCommit=true
--   - DBCP2's autoCommitOnReturn=true (DatabaseConfig default) ensures connections
--     are reset to autoCommit=true when returned to the pool
--   - Spring ResourceDatabasePopulator does not alter connection autoCommit state
CREATE INDEX CONCURRENTLY IF NOT EXISTS policy_violation_app_stage_open_unfixed_idx
  ON policy_violation (application_id, waive_time, stage_type_id, open_time DESC, threat_level DESC, policy_violation_id)
  WHERE fix_time IS NULL;
