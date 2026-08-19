-- Since 1.72.0

DROP TABLE saml_configuration;
CREATE TABLE saml_configuration (
  saml_configuration_id varchar(50) NOT NULL,
  configuration_json text NOT NULL, -- the SAML configuration stored in json format
  keystore bytea NOT NULL,
  keystore_password_obfuscated varchar(200) NOT NULL,
  CONSTRAINT saml_configuration_pk PRIMARY KEY (saml_configuration_id)
);
