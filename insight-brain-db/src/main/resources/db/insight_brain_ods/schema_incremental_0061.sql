-- Since 1.13.0
SET SCHEMA insight_brain_ods;

ALTER TABLE application_component ADD COLUMN (
  component_id_format varchar(10),
  component_id_coordinates_json CLOB
);

UPDATE application_component SET
  component_id_format='maven',
  component_id_coordinates_json='{"groupId":"' || STRINGENCODE(group_id) || '","artifactId":"' || STRINGENCODE(artifact_id) || '","version":"' || STRINGENCODE(version) || '"}'
  WHERE group_id IS NOT NULL;
  
ALTER TABLE policy_violation DROP COLUMN group_id;
ALTER TABLE policy_violation DROP COLUMN artifact_id;
ALTER TABLE policy_violation DROP COLUMN version;
