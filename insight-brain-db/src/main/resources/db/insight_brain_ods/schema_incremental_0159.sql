-- Since MIGRATE_PROXY_CONFIG
CREATE TABLE proxy_configuration (
  proxy_configuration_id varchar(50) NOT NULL,
  hostname varchar(255) NOT NULL,
  port int NOT NULL,
  username varchar(255),
  password varchar(255),
  exclude_hosts varchar(500),
  CONSTRAINT proxy_configuration_pk PRIMARY KEY (proxy_configuration_id)
);
