-- Since 1.108
DELETE FROM legal_file_override;
DELETE FROM component_legal_file;
ALTER TABLE legal_file_override DROP COLUMN type;
ALTER TABLE component_legal_file ADD COLUMN type varchar(20) NOT NULL;
ALTER TABLE component_legal_file DROP CONSTRAINT component_legal_file_uk;
ALTER TABLE component_legal_file ADD CONSTRAINT component_legal_file_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json, type);
DROP INDEX component_legal_file_owner_component_idx;
CREATE INDEX component_legal_file_owner_component_type_idx ON component_legal_file(owner_id, component_id_format, component_id_coordinates_json, type);
