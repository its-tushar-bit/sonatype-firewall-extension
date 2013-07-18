SET SCHEMA insight_brain_ods;

ALTER TABLE license_threat_group
  ALTER COLUMN name varchar(60) NOT NULL;
  