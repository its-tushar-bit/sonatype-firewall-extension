-- Since 1.52
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_waiver ADD COLUMN constraint_facts_json CLOB NULL;

ALTER TABLE policy_waiver DROP CONSTRAINT policy_waiver_uk;
