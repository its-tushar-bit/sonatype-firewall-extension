-- SaaS Compatible

-- Fix ancestor_distance bug in repository_ancestor view (CLM-38233)
--
-- H2 1.4.196 does not support CREATE OR REPLACE VIEW, so we drop and recreate.
DROP VIEW IF EXISTS repository_ancestor;

CREATE VIEW repository_ancestor (repository_id, ancestor_id, ancestor_type, ancestor_distance) AS
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
