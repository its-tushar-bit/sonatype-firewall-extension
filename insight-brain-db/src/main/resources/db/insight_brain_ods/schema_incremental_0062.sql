-- Since 1.13.0
SET SCHEMA insight_brain_ods;

-- add new columns
ALTER TABLE license_override ADD COLUMN (
  component_id_format varchar(10),
  component_id_coordinates_json CLOB
);

UPDATE license_override SET
  component_id_format='maven',
  component_id_coordinates_json='{"groupId":"' || STRINGENCODE(group_id) || '","artifactId":"' || STRINGENCODE(artifact_id) || '","version":"' || STRINGENCODE(version) || '"}';

ALTER TABLE license_override
  ALTER COLUMN component_id_format varchar(10) NOT NULL;

ALTER TABLE license_override
  ALTER COLUMN component_id_coordinates_json CLOB NOT NULL;

-- drop columns
ALTER TABLE license_override DROP CONSTRAINT license_override_uk;
ALTER TABLE license_override DROP COLUMN group_id;
ALTER TABLE license_override DROP COLUMN artifact_id;
ALTER TABLE license_override DROP COLUMN version;
