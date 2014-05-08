-- Since 1.11
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_violation
  ADD COLUMN pathnames CLOB NULL;
