SET SCHEMA insight_brain_ods;

CREATE TABLE license_threat_group (
  license_threat_group_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  threat_level smallint(2) NOT NULL,
  CONSTRAINT license_threat_group_pk PRIMARY KEY (license_threat_group_id),
  CONSTRAINT license_threat_group_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT license_threat_group_uk UNIQUE KEY (application_id, name)
);

CREATE TABLE license_threat_group_license (
  license_threat_group_license_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  license_threat_group_id varchar(50) NOT NULL,
  multi_license_id varchar(50) NOT NULL,
  CONSTRAINT license_threat_group_license_pk PRIMARY KEY (license_threat_group_license_id),
  CONSTRAINT license_threat_group_license_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT license_threat_group_license_uk UNIQUE KEY (application_id, multi_license_id)
);
