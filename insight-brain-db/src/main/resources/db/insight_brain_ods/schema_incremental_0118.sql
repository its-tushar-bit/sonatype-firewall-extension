-- Since 1.48.0
SET SCHEMA insight_brain_ods;

CREATE INDEX application_component_component_id_format_idx ON application_component(component_id_format);
