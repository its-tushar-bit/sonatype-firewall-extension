-- Since 1.17.0
SET SCHEMA insight_brain_ods;

ALTER TABLE repository_policy_violation ALTER COLUMN latest_evaluation RENAME TO active;
