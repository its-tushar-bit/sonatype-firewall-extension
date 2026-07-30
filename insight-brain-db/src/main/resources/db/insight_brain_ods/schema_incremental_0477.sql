-- SaaS Compatible
-- CLM-42787: Introduce hosted_repository_component table + ancestor view; extend owner_ancestor.
-- All-additive; no DML on existing tables; safe in a single MTIQ release.

CREATE TABLE hosted_repository_component (
  hosted_repository_component_id varchar(50) NOT NULL,
  repository_id                  varchar(50) NOT NULL,
  pathname                       varchar(1000) NOT NULL,
  hash                           varchar(20) NOT NULL,
  component_id                   varchar(255),
  owner_component_id             varchar(50),
  CONSTRAINT hosted_repository_component_pk PRIMARY KEY (hosted_repository_component_id),
  CONSTRAINT hosted_repository_component_repository_fk
    FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  CONSTRAINT hosted_repository_component_owner_component_fk
    FOREIGN KEY (owner_component_id) REFERENCES owner_component(owner_component_id) ON DELETE SET NULL,
  CONSTRAINT hosted_repository_component_uk UNIQUE (repository_id, pathname)
);
CREATE INDEX hosted_repository_component_component_id_idx ON hosted_repository_component(component_id);
CREATE INDEX hosted_repository_component_repository_id_idx ON hosted_repository_component(repository_id);

CREATE VIEW hosted_repository_component_ancestor (
    hosted_repository_component_id, ancestor_id, ancestor_type, ancestor_distance
) AS
SELECT
  hrc.hosted_repository_component_id,
  hrc.hosted_repository_component_id AS ancestor_id,
  'HOSTED_REPOSITORY_COMPONENT' AS ancestor_type,
  0 AS ancestor_distance
FROM hosted_repository_component hrc
UNION ALL
SELECT
  hrc.hosted_repository_component_id,
  ra.ancestor_id,
  ra.ancestor_type,
  ra.ancestor_distance + 1
FROM hosted_repository_component hrc
INNER JOIN repository_ancestor ra ON ra.repository_id = hrc.repository_id;

-- Create the new ancestor view first, then DROP + CREATE owner_ancestor back-to-back so the
-- window during which owner_ancestor is missing is minimised in case an intermediate statement
-- fails. Neither dialect supports CREATE OR REPLACE across our target versions here.
DROP VIEW owner_ancestor;

CREATE VIEW owner_ancestor (owner_id, ancestor_id, ancestor_type, ancestor_distance, owner_type) AS
SELECT organization_id, ancestor_id, 'ORGANIZATION', ancestor_distance, 'ORGANIZATION' FROM organization_ancestor
UNION ALL
SELECT *, 'APPLICATION' FROM application_ancestor
UNION ALL
SELECT *, 'REPOSITORY_CONTAINER' FROM repository_container_ancestor
UNION ALL
SELECT *, 'REPOSITORY_MANAGER' FROM repository_manager_ancestor
UNION ALL
SELECT *, 'REPOSITORY' FROM repository_ancestor
UNION ALL
SELECT *, 'HOSTED_REPOSITORY_COMPONENT' FROM hosted_repository_component_ancestor;
