-- Since 1.139
CREATE INDEX repository_component_component_coordinates_idx ON repository_component (component_id_format, component_id_coordinates_json);
