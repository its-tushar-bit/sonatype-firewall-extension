-- Since 1.50
SET SCHEMA insight_brain_ods;

ALTER TABLE application ADD COLUMN policy_violation_grandfathering_enabled boolean;
