-- Since 1.104
CREATE TABLE application_component_license (
  application_component_license_id varchar(50) NOT NULL,
  application_component_id varchar(50) NOT NULL,
  effective_license_id varchar(1000) NOT NULL,
  CONSTRAINT application_component_license_pk PRIMARY KEY (application_component_license_id),
  CONSTRAINT application_component_license_application_component_fk FOREIGN KEY (application_component_id) REFERENCES application_component(application_component_id),
  CONSTRAINT application_component_license_uk UNIQUE (application_component_id, effective_license_id)
);
CREATE INDEX application_component_license_effective_license_id_idx ON application_component_license(effective_license_id);
