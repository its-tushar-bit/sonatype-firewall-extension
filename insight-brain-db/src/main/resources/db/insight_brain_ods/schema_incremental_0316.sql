-- Originally: Since 1.169
-- Modified: Since 1.TBD (see notes below)
-- SaaS Compatible

/*
-- https://sonatype.atlassian.net/browse/CLM-30378
-- This migration has caused multiple customer outages due to approach taken. Ultimately it is just a simple column
-- rename. However, due to 'SaaS Compatible' requirements it was split into multiple steps. Per the SQL below this
-- included multiple `UPDATE` statements on columns which causes multiple full table rewrites. Since this can be a
-- large table this took longer than some installations allowed.

-- ORIGINAL MIGRATION as merged in https://github.com/sonatype/insight-brain/pull/10157 on Oct 16, 2023

ALTER TABLE organization ADD COLUMN legacy_violation_enabled boolean;
ALTER TABLE organization ADD COLUMN allow_legacy_violation_override boolean DEFAULT true NOT NULL;
UPDATE organization SET legacy_violation_enabled = policy_violation_grandfathering_enabled;
UPDATE organization SET allow_legacy_violation_override = allow_policy_violation_grandfathering_override;
ALTER TABLE organization ALTER COLUMN allow_policy_violation_grandfathering_override DROP NOT NULL;
ALTER TABLE organization ALTER COLUMN allow_policy_violation_grandfathering_override DROP DEFAULT;

ALTER TABLE application ADD COLUMN legacy_violation_enabled boolean;
UPDATE application SET legacy_violation_enabled = policy_violation_grandfathering_enabled;

ALTER TABLE policy ADD COLUMN legacy_violation_allowed boolean;
UPDATE policy SET legacy_violation_allowed = policy_violation_grandfathering_allowed;
ALTER TABLE policy ALTER COLUMN legacy_violation_allowed SET NOT NULL;
ALTER TABLE policy ALTER COLUMN policy_violation_grandfathering_allowed DROP NOT NULL;

ALTER TABLE policy_violation ADD COLUMN legacy_violation_time timestamp NULL;
ALTER TABLE policy_violation ADD COLUMN legacy_violation_applied bool NOT NULL DEFAULT false;
UPDATE policy_violation SET legacy_violation_time = grandfather_time;
UPDATE policy_violation SET legacy_violation_applied = grandfather_applied;
ALTER TABLE policy_violation ALTER COLUMN grandfather_applied DROP NOT NULL;
ALTER TABLE policy_violation ALTER COLUMN grandfather_applied DROP DEFAULT;
*/

-- SQL modified post-release of 1.169. Since MTIQ deployed the SaaS equivalent of 1.169 long ago, we can safely modify
-- the problem SQL (i.e. schema_incremental_0316.sql) and know it will only affect on-prem customers. Furthermore, since
-- our custom migration logic does not track if a SQL was changed, any customer who ran the original migration 316
-- will not re-run this. So only customers who still need to run migration 316

ALTER TABLE organization
    RENAME COLUMN policy_violation_grandfathering_enabled TO legacy_violation_enabled;
ALTER TABLE organization
    RENAME COLUMN allow_policy_violation_grandfathering_override TO allow_legacy_violation_override;

ALTER TABLE application
    RENAME COLUMN policy_violation_grandfathering_enabled TO legacy_violation_enabled;

ALTER TABLE policy
    RENAME COLUMN policy_violation_grandfathering_allowed TO legacy_violation_allowed;

ALTER TABLE policy_violation
    RENAME COLUMN grandfather_time TO legacy_violation_time;
ALTER TABLE policy_violation
    RENAME COLUMN grandfather_applied TO legacy_violation_applied;
