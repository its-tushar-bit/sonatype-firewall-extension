-- Since 1.9
SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ALTER COLUMN organization_id varchar(50) NOT NULL;