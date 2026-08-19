-- Since 1.186
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS oidc_token
(
    oidc_token_id     varchar(255) NOT NULL,
    oidc_token        text NOT NULL,
    registration_time timestamp NOT NULL,
    CONSTRAINT oidc_token_pk PRIMARY KEY (oidc_token_id)
);
