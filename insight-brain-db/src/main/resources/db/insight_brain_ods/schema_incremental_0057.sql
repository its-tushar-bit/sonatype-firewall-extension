-- Since 1.12
SET SCHEMA insight_brain_ods;

ALTER TABLE policy
  ADD COLUMN drools_code CLOB NULL;