-- Since 1.17.0
SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ALTER COLUMN public_id varchar(200) NOT NULL;

ALTER TABLE application
  ALTER COLUMN public_id_lowercase varchar(200) NOT NULL;
