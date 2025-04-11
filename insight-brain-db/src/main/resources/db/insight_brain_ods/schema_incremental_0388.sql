-- since 1.191
-- SaaS Compatible
CREATE TABLE cpe_matching_configuration (
  cpe_matching_configuration_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  cpe_enabled boolean,
  allow_override boolean,
  CONSTRAINT cpe_matching_configuration_pk PRIMARY KEY (cpe_matching_configuration_id),
  CONSTRAINT cpe_matching_configuration_owner_id_uk UNIQUE (owner_id)
);
CREATE INDEX cpe_matching_configuration_owner_id_idx ON cpe_matching_configuration(owner_id);
