-- Since 1.72.0

CREATE TABLE saml_configuration (
  saml_configuration_id varchar(50) NOT NULL,
  configuration_json text NOT NULL, -- the SAML configuration stored in json format
  CONSTRAINT saml_configuration_pk PRIMARY KEY (saml_configuration_id)
);
