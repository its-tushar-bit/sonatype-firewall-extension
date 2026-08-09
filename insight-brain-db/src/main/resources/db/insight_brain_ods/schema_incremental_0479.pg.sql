-- SaaS Compatible
-- CLM-43707: Drop the cancelled CLM-39706 synchronous-enforcement scaffolding.
--
-- The Java code that read and wrote these objects was removed in release N by
-- CLM-42780 (IQ side) and CLM-42779 (NXRM side). Per the two-release cadence in
-- the Hosted-Repository Scanning: Target Architecture doc §4, the physical DDL
-- drops were deferred to N+1 so every draining N-era pod is gone before the
-- schema loses these objects.
--
-- Both tables are true leaf tables (nothing outside this pair references them)
-- and hold zero rows in every deployment — the synchronous-enforcement feature
-- was never invoked from NXRM after CLM-39706 was cancelled. Metadata-only; no
-- backfill or data preservation is needed.
--
-- The origin CREATE TABLE blocks live in schema_incremental_0453.sql (commit
-- 82605add07c, CLM-39706 / CLM-39870) which stays as-is per the incremental
-- immutability rule; schema.sql loses the CREATE TABLE blocks and the
-- hosted_component_scan_queue.purl column in the same PR as this incremental.

-- FK order matters: violation → block.
DROP TABLE IF EXISTS hosted_deployment_block_violation;
DROP TABLE IF EXISTS hosted_deployment_block;

ALTER TABLE hosted_component_scan_queue DROP COLUMN IF EXISTS purl;
