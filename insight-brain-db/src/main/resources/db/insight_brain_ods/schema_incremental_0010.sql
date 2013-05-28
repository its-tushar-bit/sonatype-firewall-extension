SET SCHEMA insight_brain_ods;

CREATE TABLE organization (
  organization_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  CONSTRAINT organization_pk PRIMARY KEY (organization_id),
  CONSTRAINT organization_name_uk UNIQUE KEY (name_lowercase_no_whitespace)
);
