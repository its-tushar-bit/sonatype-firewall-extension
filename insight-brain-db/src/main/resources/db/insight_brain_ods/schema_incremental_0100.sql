-- Since 1.22.0
SET SCHEMA insight_brain_ods;

CREATE TABLE proprietary_config (
  proprietary_config_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  packages_json CLOB NULL,
  regexes_json CLOB NULL,
  CONSTRAINT proprietary_config_pk PRIMARY KEY (proprietary_config_id),
  CONSTRAINT proprietary_config_owner_uk UNIQUE KEY (owner_id)
);
