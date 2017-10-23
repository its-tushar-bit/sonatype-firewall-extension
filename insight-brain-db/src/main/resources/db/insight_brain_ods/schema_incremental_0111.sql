-- Since 1.39.0
SET SCHEMA insight_brain_ods;

ALTER TABLE repository_policy_violation
  DROP COLUMN notifications;
