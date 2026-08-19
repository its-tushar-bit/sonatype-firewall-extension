-- Since 1.105
CREATE TABLE component_copyright (
  component_copyright_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  legal_content_hash varchar(64) NOT NULL,
  CONSTRAINT component_copyright_pk PRIMARY KEY (component_copyright_id),
  CONSTRAINT component_copyright_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json)
);
CREATE INDEX component_copyright_owner_component_idx ON component_copyright(owner_id, component_id_format, component_id_coordinates_json);

CREATE TABLE copyright_override (
  copyright_override_id varchar(50) NOT NULL,
  original_content_hash varchar(64),
  content_hash varchar(64) NOT NULL,
  content varchar(1000) NOT NULL,
  status varchar(20) NOT NULL,
  component_copyright_id varchar(50) NOT NULL,
  CONSTRAINT copyright_override_pk PRIMARY KEY (copyright_override_id),
  CONSTRAINT copyright_override_component_copyright_fk FOREIGN KEY (component_copyright_id) REFERENCES component_copyright(component_copyright_id)
);
CREATE INDEX copyright_override_component_copyright_id_idx ON copyright_override(component_copyright_id);

CREATE TABLE component_legal_file (
  component_legal_file_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  legal_content_hash varchar(64) NOT NULL,
  CONSTRAINT component_legal_file_pk PRIMARY KEY (component_legal_file_id),
  CONSTRAINT component_legal_file_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json)
);
CREATE INDEX component_legal_file_owner_component_idx ON component_legal_file(owner_id, component_id_format, component_id_coordinates_json);

CREATE TABLE legal_file_override (
  legal_file_override_id varchar(50) NOT NULL,
  type varchar(20) NOT NULL,
  original_content_hash varchar(64),
  content_hash varchar(64) NOT NULL,
  content text NOT NULL,
  status varchar(20) NOT NULL,
  component_legal_file_id varchar(50) NOT NULL,
  CONSTRAINT legal_file_override_pk PRIMARY KEY (legal_file_override_id),
  CONSTRAINT legal_file_override_component_legal_file_fk FOREIGN KEY (component_legal_file_id) REFERENCES component_legal_file(component_legal_file_id)
);
CREATE INDEX legal_file_override_component_legal_file_id_idx ON legal_file_override(component_legal_file_id);

CREATE TABLE component_obligation (
  component_obligation_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  obligation_name varchar(256) NOT NULL,
  comment varchar(1000),
  status varchar(20) NOT NULL,
  legal_content_hash varchar(64) NOT NULL,
  CONSTRAINT component_obligation_pk PRIMARY KEY (component_obligation_id),
  CONSTRAINT component_obligation_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json, obligation_name)
);
CREATE INDEX component_obligation_owner_component_obligation_idx ON component_obligation(owner_id, component_id_format, component_id_coordinates_json, obligation_name);

CREATE TABLE component_obligation_attribution (
  component_obligation_attribution_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  obligation_name varchar(256),
  content varchar(1000) NOT NULL,
  legal_content_hash varchar(64) NOT NULL,
  CONSTRAINT component_obligation_attribution_pk PRIMARY KEY (component_obligation_attribution_id)
);
CREATE INDEX component_obligation_attribution_owner_component_obligation_idx ON component_obligation_attribution(owner_id, component_id_format, component_id_coordinates_json, obligation_name);
