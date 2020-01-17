-- Since 1.83
CREATE TABLE mail_configuration (
  mail_configuration_id varchar(50) NOT NULL,
  hostname varchar(255) NOT NULL,
  port int NOT NULL,
  username varchar(255),
  password varchar(255),
  ssl_enabled boolean NOT NULL,
  start_tls_enabled boolean NOT NULL,
  system_email varchar(255) NOT NULL,
  CONSTRAINT mail_configuration_pk PRIMARY KEY (mail_configuration_id)
);
