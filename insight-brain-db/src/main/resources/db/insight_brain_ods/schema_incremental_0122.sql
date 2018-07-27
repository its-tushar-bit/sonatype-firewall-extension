-- Since POLICY_VIOLATION_GRANDFATHERING
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_violation ADD COLUMN grandfather_time datetime NULL;