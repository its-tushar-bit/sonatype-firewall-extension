SET SCHEMA insight_brain_ods;

ALTER TABLE license_threat_group_license
  DROP CONSTRAINT license_threat_group_license_uk;

ALTER TABLE license_threat_group_license
  ADD CONSTRAINT license_threat_group_license_uk UNIQUE KEY (license_threat_group_id, license_id);
  