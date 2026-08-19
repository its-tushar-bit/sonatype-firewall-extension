-- Since 1.170
-- SaaS Compatible

/*
-- See comment in schema_incremental_0316.sql
-- With that updated migration this one is no longer applicable

ALTER TABLE organization DROP COLUMN allow_policy_violation_grandfathering_override;
ALTER TABLE organization DROP COLUMN policy_violation_grandfathering_enabled;

ALTER TABLE application DROP COLUMN policy_violation_grandfathering_enabled;

ALTER TABLE policy DROP COLUMN policy_violation_grandfathering_allowed;

ALTER TABLE policy_violation DROP COLUMN grandfather_time;
ALTER TABLE policy_violation DROP COLUMN grandfather_applied;
*/
