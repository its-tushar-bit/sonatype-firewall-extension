-- SaaS Compatible
-- CLM-43707: Drop the cancelled CLM-39706 synchronous-enforcement scaffolding — H2 variant (on-prem).
--
-- Companion to schema_incremental_0478.pg.sql. On-prem applies both release-N
-- and release-N+1 migrations in a single maintenance window, so no deploy
-- ordering concern applies here — the drops execute alongside the release-N
-- Java removal (CLM-42780).
--
-- Same drops as the Postgres variant: metadata-only, zero rows in any
-- deployment. See the .pg.sql for full rationale.

-- FK order matters: violation → block.
DROP TABLE IF EXISTS hosted_deployment_block_violation;
DROP TABLE IF EXISTS hosted_deployment_block;

ALTER TABLE hosted_component_scan_queue DROP COLUMN IF EXISTS purl;
