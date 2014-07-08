-- Since 1.12
SET SCHEMA insight_brain_ods;

ALTER TABLE policy
  ALTER COLUMN drools_code CLOB NOT NULL;