SET SCHEMA insight_brain_ods;

ALTER TABLE license_threat_group_license
  ADD CONSTRAINT license_threat_group_license_group_fk FOREIGN KEY (license_threat_group_id) REFERENCES license_threat_group(license_threat_group_id);
