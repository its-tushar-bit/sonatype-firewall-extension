-- Since 1.11
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_evaluation
  ADD COLUMN for_obsolete_scan bool DEFAULT false NOT NULL;
