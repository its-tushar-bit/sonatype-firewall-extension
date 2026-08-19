-- Since 1.177
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS oidc_configuration
(
  idp_issuer                        varchar(255) NOT NULL,
  client_id                         varchar(255) NOT NULL,
  client_secret                     varchar(255) NOT NULL,
  idp_authorization_url             varchar(255) NOT NULL,
  idp_token_url                     varchar(255) NOT NULL,
  authorization_custom_params_json  text,
  token_request_custom_params_json  text,
  CONSTRAINT oidc_configuration_pk PRIMARY KEY (idp_issuer)
);
