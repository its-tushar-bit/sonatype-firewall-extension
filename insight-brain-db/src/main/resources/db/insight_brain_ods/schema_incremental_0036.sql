-- Since 1.9
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_waiver
  ALTER COLUMN hash varchar(20) NULL;
