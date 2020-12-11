-- Since 1.104
CREATE TABLE aggregate_file (
  aggregate_file_id varchar(50) NOT NULL,
  application_component_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  pathnames text,
  CONSTRAINT aggregate_file_pk PRIMARY KEY (aggregate_file_id),
  CONSTRAINT aggregate_file_application_component_fk FOREIGN KEY (application_component_id) REFERENCES application_component(application_component_id),
  CONSTRAINT aggregate_file_uk UNIQUE (application_component_id, hash)
);
CREATE INDEX aggregate_file_application_component_id_idx ON aggregate_file(application_component_id);
