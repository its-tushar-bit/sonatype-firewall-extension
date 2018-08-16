-- Since 1.50
SET SCHEMA insight_brain_ods;

ALTER TABLE policy ADD COLUMN policy_violation_grandfathering_allowed boolean;
UPDATE policy SET policy_violation_grandfathering_allowed = (threat_level <= 8);
ALTER TABLE policy MODIFY COLUMN policy_violation_grandfathering_allowed boolean NOT NULL;
