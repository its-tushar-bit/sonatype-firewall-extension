SET SCHEMA insight_brain_ods;

ALTER TABLE license_threat_group_license
  DROP CONSTRAINT license_threat_group_license_application_fk;
ALTER TABLE license_threat_group_license
  ALTER COLUMN application_id RENAME TO owner_id;

ALTER TABLE license_threat_group
  DROP CONSTRAINT license_threat_group_application_fk;
ALTER TABLE license_threat_group
  ALTER COLUMN application_id RENAME TO owner_id;
  