-- Since 1.13.0
SET SCHEMA insight_brain_ods;

ALTER TABLE hash_component_identifier
  ALTER COLUMN component_id_coordinates_json varchar(1000) NOT NULL;

ALTER TABLE license_override
  ALTER COLUMN component_id_coordinates_json varchar(1000) NOT NULL;

ALTER TABLE policy_violation
  ALTER COLUMN component_id_coordinates_json varchar(1000);

ALTER TABLE application_component
  ALTER COLUMN component_id_coordinates_json varchar(1000);

ALTER TABLE hash_component_identifier
  ADD CONSTRAINT hash_component_identifier_component_id_uk UNIQUE KEY (component_id_format, component_id_coordinates_json);

ALTER TABLE license_override
  ADD CONSTRAINT license_override_uk UNIQUE KEY (owner_id, component_id_format, component_id_coordinates_json);
