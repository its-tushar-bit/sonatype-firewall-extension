-- SaaS Compatible
-- CLM-41005 (H2-only, migration 0472): plain composite index supporting both the outer keyset
-- driver scan and the NOT EXISTS anti-join dedup probe in
-- RepositoryComponentDAO.getMonitoringEligiblePagePostgres. The index leads with
-- (repository_id, hash) — the natural-key dedup correlation — followed by
-- (time DESC, repository_component_id DESC) so a single index covers:
--   (a) the inner anti-join probe ("is there a sibling row with greater (time, id) for my group?")
--   (b) the outer driver's keyset advance through the result set newest-first
-- The Postgres CREATE INDEX CONCURRENTLY equivalent cannot run inline here because
-- repository_component is a large table on production tenants (see
-- insight-brain-db/.claude/rules/sql-saas-compatibility.md).
CREATE INDEX IF NOT EXISTS repository_component_dedup_keyset_idx
  ON repository_component (repository_id, hash, time DESC, repository_component_id DESC);
