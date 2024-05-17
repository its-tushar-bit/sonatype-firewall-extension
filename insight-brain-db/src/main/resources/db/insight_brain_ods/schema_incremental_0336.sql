-- SaaS Compatible

-- Quick lookup of all ancestor organizations for a given organization. As this data is technically redundant,
-- the implementation of this table went through a number of iterations before settling on this simple approach.
--
-- Initially, it was a VIEW. This had two downsides: performance, and separate H2 and Postgres implementations (a
-- recursive query in Postgres, and a Java-based custom database function in H2).
--
-- Then, to improve performance, H2 was switched to a table kept up-to-date with TRIGGERs, and Postgres was switched
-- to a MATERIALIZED VIEW refreshed using TRIGGERs. However, it was found that there were race conditions in the
-- execution of the Postgres triggers when multiple organizations were being created concurrently.
--
-- To address the race condition, Postgres too was switched to a regular table kept up-to-date using TRIGGERs. At
-- that point however it was realized that things could be simplified a great deal if the database-specific triggers
-- were removed and the table kept up-to-date at the DAO level instead.
CREATE TABLE organization_ancestor (
  organization_ancestor_id VARCHAR(50) NOT NULL,
  organization_id VARCHAR(50) NOT NULL,
  ancestor_id VARCHAR(50) NOT NULL,
  ancestor_distance INTEGER NOT NULL,
  CONSTRAINT organization_ancestor_pk PRIMARY KEY (organization_ancestor_id),
  CONSTRAINT organization_ancestor_uk UNIQUE (organization_id, ancestor_id),
  CONSTRAINT organization_ancestor_ancestor_id_fk FOREIGN KEY (ancestor_id) REFERENCES organization (organization_id)
);

CREATE INDEX organization_ancestor_ancestor_id_idx ON organization_ancestor (ancestor_id);

-- As of the creation of this script, this table only exists for the purpose of the ancestor views defined below.
-- It was very tempting to instead define repository_container_ancestor using a hardcoded VALUES clause, but
-- that caused the postgres query planner to completely ignore all indexes when querying the owner_ancestor view.
-- To work efficiently, all of these views apparently need to ultimately query real tables.
CREATE TABLE repository_container (
  repository_container_id VARCHAR(50) NOT NULL,
  organization_id VARCHAR(50) NOT NULL,
  CONSTRAINT repository_container_pk PRIMARY KEY (repository_container_id)
);

INSERT INTO repository_container VALUES ('REPOSITORY_CONTAINER_ID', 'ROOT_ORGANIZATION_ID');

-- The following five VIEWs build on organization_ancestor to allow querying of all ancestors of any type of owner
-- object.
CREATE VIEW application_ancestor (application_id, ancestor_id, ancestor_type, ancestor_distance) AS
SELECT
  application_id,
  application_id AS ancestor_id,
  'APPLICATION' AS ancestor_type,
  0 AS ancestor_distance
FROM
  application
UNION ALL
SELECT
  a.application_id,
  oa.ancestor_id,
  'ORGANIZATION' AS ancestor_type,
  oa.ancestor_distance + 1
FROM
  application a
  INNER JOIN organization_ancestor oa ON oa.organization_id = a.organization_id;

CREATE VIEW repository_container_ancestor (repository_container_id, ancestor_id, ancestor_type, ancestor_distance) AS
SELECT
  repository_container_id,
  repository_container_id AS ancestor_id,
  'REPOSITORY_CONTAINER' AS ancestor_type,
  0 AS ancestor_distance
FROM
  repository_container
UNION ALL
SELECT
  rc.repository_container_id,
  oa.ancestor_id,
  'ORGANIZATION' AS ancestor_type,
  oa.ancestor_distance + 1
FROM
  repository_container rc
  INNER JOIN organization_ancestor oa ON oa.organization_id = rc.organization_id;

CREATE VIEW repository_manager_ancestor (repository_manager_id, ancestor_id, ancestor_type, ancestor_distance) AS
SELECT
  repository_manager_id,
  repository_manager_id AS ancestor_id,
  'REPOSITORY_MANAGER' AS ancestor_type,
  0 AS ancestor_distance
FROM
  repository_manager
UNION ALL
SELECT
  rm.repository_manager_id,
  rca.ancestor_id,
  rca.ancestor_type,
  rca.ancestor_distance + 1
FROM
  repository_manager rm,
  repository_container_ancestor rca;

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
  rma.ancestor_distance
FROM
  repository r
  INNER JOIN repository_manager_ancestor rma ON rma.repository_manager_id = r.repository_manager_id;

CREATE VIEW owner_ancestor (owner_id, ancestor_id, ancestor_type, ancestor_distance, owner_type) AS
SELECT organization_id, ancestor_id, 'ORGANIZATION', ancestor_distance, 'ORGANIZATION' FROM organization_ancestor
UNION ALL
SELECT *, 'APPLICATION' FROM application_ancestor
UNION ALL
SELECT *, 'REPOSITORY_CONTAINER' FROM repository_container_ancestor
UNION ALL
SELECT *, 'REPOSITORY_MANAGER' FROM repository_manager_ancestor
UNION ALL
SELECT *, 'REPOSITORY' FROM repository_ancestor;

-- To help with performance of the above views
CREATE INDEX application_organization_idx ON application (organization_id);
CREATE INDEX repository_repository_manager_idx ON repository (repository_manager_id);
