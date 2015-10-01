-- Since 1.17.0
SET SCHEMA insight_brain_dm;

ALTER TABLE license
  DROP COLUMN licenseUrl;
ALTER TABLE multi_license
  DROP COLUMN licenseUrl;
