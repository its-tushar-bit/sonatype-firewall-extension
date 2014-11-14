-- Since 1.13.0
SET SCHEMA insight_brain_ods;

CREATE TABLE hash_component_identifier (
  hash_component_identifier_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json CLOB NOT NULL, -- the component identifier coordinates stored in json format
  comment varchar(1000) NULL,
  create_time datetime NULL,
  CONSTRAINT hash_component_identifier_pk PRIMARY KEY (hash_component_identifier_id),
  CONSTRAINT hash_component_identifier_hash_uk UNIQUE KEY (hash)
);

INSERT INTO hash_component_identifier
  (hash_component_identifier_id, hash, component_id_format,
    component_id_coordinates_json,
    comment, create_time) 
  SELECT hash_gav_id, hash, 'maven',
    '{"artifactId":"' || STRINGENCODE(artifact_id) || '","groupId":"' || STRINGENCODE(group_id) || '","version":"' || STRINGENCODE(version) || '"}',
    comment, create_time FROM hash_gav;

DROP TABLE hash_gav;
  