SET SCHEMA insight_brain_ods;

ALTER TABLE license_threat_group
  ADD COLUMN name_lowercase_no_whitespace varchar(60) NULL;

UPDATE license_threat_group SET name_lowercase_no_whitespace=REGEXP_REPLACE(LOWER(name),'\s','');

ALTER TABLE license_threat_group
  ALTER COLUMN name_lowercase_no_whitespace varchar(60) NOT NULL;

ALTER TABLE license_threat_group
  DROP CONSTRAINT license_threat_group_uk;

ALTER TABLE license_threat_group
  ADD CONSTRAINT license_threat_group_uk UNIQUE KEY (owner_id, name_lowercase_no_whitespace);
  