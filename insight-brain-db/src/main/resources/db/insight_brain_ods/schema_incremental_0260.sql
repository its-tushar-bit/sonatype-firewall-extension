-- Since 1.137
CREATE TABLE repository_identified_component (
  hash varchar(64) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  create_time timestamp NOT NULL,
  last_access_time timestamp NOT NULL,
  CONSTRAINT hash_pk PRIMARY KEY (hash)
);
