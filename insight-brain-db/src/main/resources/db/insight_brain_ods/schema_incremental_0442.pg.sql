-- SaaS Compatible

-- Fix ancestor_distance bug in repository_ancestor view (CLM-38233)
--
-- The repository_ancestor view was incorrectly copying rma.ancestor_distance
-- directly from repository_manager_ancestor without adding 1. This caused a
-- repository's parent repository_manager to appear at ancestor_distance=0
-- (the same as the repository itself) instead of ancestor_distance=1.
--
-- This mirrors the correct pattern already used in application_ancestor,
-- which uses oa.ancestor_distance + 1 when joining organization_ancestor.
CREATE OR REPLACE VIEW repository_ancestor (repository_id, ancestor_id, ancestor_type, ancestor_distance) AS
SELECT
  repository_id,
  repository_id AS ancestor_id,
  'REPOSITORY' AS ancestor_type,
  0 AS ancestor_distance
FROM
  repository
UNION ALL
SELECT
  r.repository_id,
  rma.ancestor_id,
  rma.ancestor_type,
  rma.ancestor_distance + 1
FROM
  repository r
  INNER JOIN repository_manager_ancestor rma ON rma.repository_manager_id = r.repository_manager_id;
