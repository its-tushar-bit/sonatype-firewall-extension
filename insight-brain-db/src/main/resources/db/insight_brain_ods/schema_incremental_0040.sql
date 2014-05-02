-- Since 1.9
SET SCHEMA insight_brain_ods;

SET MAX_LENGTH_INPLACE_LOB 100000;

CREATE TABLE policy (
  policy_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  threat_level smallint(2) NOT NULL,
  content CLOB NOT NULL,
  CONSTRAINT policy_pk PRIMARY KEY (policy_id),
  CONSTRAINT policy_name_uk UNIQUE KEY (owner_id, name_lowercase_no_whitespace)
);
  