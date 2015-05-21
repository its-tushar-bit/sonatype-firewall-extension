-- Since 1.15.0
SET SCHEMA insight_brain_ods;

ALTER TABLE role
  ALTER COLUMN description varchar(255) NOT NULL;
