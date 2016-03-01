-- Since 1.20.0
SET SCHEMA insight_brain_ods;

CREATE INDEX repository_component_repository_unquarantine_idx ON repository_component(repository_id, unquarantine_time);
