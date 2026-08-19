-- Since 1.134
CREATE TABLE crowd_configuration (
  crowd_configuration_id varchar(50) NOT NULL,
  server_url varchar(2048) NOT NULL,
  application_name varchar(255) NOT NULL,
  application_password varchar(255) NOT NULL,
  CONSTRAINT crowd_configuration_pk PRIMARY KEY (crowd_configuration_id)
);
