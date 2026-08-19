-- Since 1.98
CREATE TABLE inner_source_component (
  inner_source_component_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  package_url varchar(1000) NOT NULL,
  CONSTRAINT inner_source_component_pk PRIMARY KEY (inner_source_component_id),
  CONSTRAINT inner_source_component_uk UNIQUE (package_url),
  CONSTRAINT inner_source_component_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id)
);
