SET SCHEMA insight_brain_ods;

CREATE TABLE license_override (
  license_override_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  group_id varchar(100) NOT NULL,
  artifact_id varchar(100) NOT NULL,
  version varchar(100) NOT NULL,
  status varchar(20) NOT NULL,
  license_id varchar(50) NULL,
  comment varchar(1000) NULL,
  CONSTRAINT license_override_pk PRIMARY KEY (license_override_id),
  CONSTRAINT license_override_uk UNIQUE KEY (owner_id, group_id, artifact_id, version)
);
