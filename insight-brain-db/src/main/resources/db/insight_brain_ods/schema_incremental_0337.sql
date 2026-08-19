-- since 1.177
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS oauth2_configuration
(
  idp_issuer               varchar(255) NOT NULL,
  idp_jwks_url             varchar(255),
  idp_jws_algorithm        varchar(255) NOT NULL,
  idp_jwks                 text,
  username_claim           varchar(255),
  first_name_claim         varchar(255),
  last_name_claim          varchar(255),
  email_claim              varchar(255),
  groups_claim             varchar(255),
  exact_match_claims_json  text,
  CONSTRAINT oauth_configuration_pk PRIMARY KEY (idp_issuer)
);
