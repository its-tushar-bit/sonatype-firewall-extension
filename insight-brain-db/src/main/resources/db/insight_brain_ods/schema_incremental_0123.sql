-- Since POLICY_VIOLATION_GRANDFATHERING
SET SCHEMA insight_brain_ods;

ALTER TABLE organization ADD COLUMN (
  policy_violation_grandfathering_enabled boolean,
  allow_policy_violation_grandfathering_override boolean DEFAULT true NOT NULL
);
