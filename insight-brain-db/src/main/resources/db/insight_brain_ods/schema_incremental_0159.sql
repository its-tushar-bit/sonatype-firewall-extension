-- Since 1.84
CREATE TABLE proxy_server_configuration (
  proxy_server_configuration_id varchar(50) NOT NULL,
  hostname varchar(255) NOT NULL,
  port int NOT NULL,
  username varchar(255),
  password varchar(255),
  exclude_hosts varchar(500),
  CONSTRAINT proxy_server_configuration_pk PRIMARY KEY (proxy_server_configuration_id)
);
