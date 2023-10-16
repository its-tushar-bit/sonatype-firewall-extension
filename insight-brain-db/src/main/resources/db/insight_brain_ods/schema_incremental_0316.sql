-- Since 1.169
-- SaaS Compatible
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
