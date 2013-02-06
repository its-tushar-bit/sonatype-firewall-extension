SET SCHEMA insight_brain_ods;

ALTER TABLE license_threat_group_license
  DROP CONSTRAINT license_threat_group_license_uk;

ALTER TABLE license_threat_group_license
  ALTER COLUMN multi_license_id RENAME TO license_id;

ALTER TABLE license_threat_group_license
  ADD CONSTRAINT license_threat_group_license_uk UNIQUE KEY (application_id, license_id);
