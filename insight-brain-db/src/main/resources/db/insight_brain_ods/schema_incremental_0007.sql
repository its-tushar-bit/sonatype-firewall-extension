SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ADD COLUMN name VARCHAR(60) NULL;

ALTER TABLE application
  ADD COLUMN name_lowercase_no_whitespace VARCHAR(60) NULL;
