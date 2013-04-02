SET SCHEMA insight_brain_ods;

CREATE TABLE application_profile (
  application_profile_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  CONSTRAINT application_profile_pk PRIMARY KEY (application_profile_id),
  CONSTRAINT application_profile_name_uk UNIQUE KEY (name_lowercase_no_whitespace)
);

INSERT INTO application_profile (application_profile_id, name, name_lowercase_no_whitespace) VALUES ('default_application_profile', 'Default Application Profile', 'defaultapplicationprofile');
