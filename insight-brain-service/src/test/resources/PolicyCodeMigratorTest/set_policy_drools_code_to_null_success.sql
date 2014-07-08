SET SCHEMA insight_brain_ods;

ALTER TABLE policy
  ALTER COLUMN drools_code CLOB NULL;
  
UPDATE policy SET drools_code = null;