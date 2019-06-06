-- Since 1.66.0
CREATE TABLE source_control (
  source_control_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  repository_url varchar(2048) NOT NULL,
  token varchar(512) NOT NULL,
  CONSTRAINT source_control_pk PRIMARY KEY (source_control_id),
  CONSTRAINT source_control_application_id_uk UNIQUE (application_id),
  CONSTRAINT source_control_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id)
);
