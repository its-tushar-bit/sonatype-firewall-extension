-- SaaS Compatible

CREATE TABLE IF NOT EXISTS inner_source_application (
  inner_source_application_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  package_url varchar(1000) NOT NULL,
  CONSTRAINT inner_source_application_pk PRIMARY KEY (inner_source_application_id),
  CONSTRAINT inner_source_application_uk UNIQUE (package_url),
  CONSTRAINT inner_source_application_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS inner_source_version (
  inner_source_version_id varchar(50) NOT NULL,
  inner_source_application_id varchar(50) NOT NULL,
  latest_version varchar(200) NOT NULL,
  stage_type_id varchar(50),
  CONSTRAINT inner_source_version_pk PRIMARY KEY (inner_source_version_id),
  CONSTRAINT inner_source_version_uk UNIQUE (inner_source_application_id, stage_type_id),
  CONSTRAINT inner_source_version_inner_source_application_fk FOREIGN KEY (inner_source_application_id) REFERENCES inner_source_application (inner_source_application_id) ON DELETE CASCADE
);
