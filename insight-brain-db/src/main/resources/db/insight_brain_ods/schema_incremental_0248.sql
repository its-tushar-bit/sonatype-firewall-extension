-- Since 1.133
CREATE TABLE component_source_link (
  component_source_link_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  last_updated_by_username varchar(256) NOT NULL,
  last_updated_at timestamp NOT NULL,
  CONSTRAINT component_source_link_pk PRIMARY KEY (component_source_link_id),
  CONSTRAINT component_source_link_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json)
);
CREATE INDEX component_source_link_owner_component_idx ON component_source_link(owner_id, component_id_format, component_id_coordinates_json);

CREATE TABLE source_link_override (
  source_link_override_id varchar(50) NOT NULL,
  content varchar(1000) NOT NULL,
  status varchar(20) NOT NULL,
  component_source_link_id varchar(50) NOT NULL,
  CONSTRAINT source_link_override_pk PRIMARY KEY (source_link_override_id),
  CONSTRAINT source_link_override_component_source_link_fk FOREIGN KEY (component_source_link_id) REFERENCES component_source_link(component_source_link_id)
);
CREATE INDEX source_link_override_component_source_link_id_idx ON source_link_override(component_source_link_id);

