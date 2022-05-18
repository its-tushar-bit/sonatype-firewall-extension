-- Since 1.138
CREATE TABLE reverse_proxy_authentication_configuration (
  reverse_proxy_authentication_configuration_id varchar(50) NOT NULL,
  enabled boolean NOT NULL,
  username_header varchar(255) NOT NULL,
  csrf_protection_disabled boolean NOT NULL,
  logout_url varchar(2048),
  CONSTRAINT reverse_proxy_authentication_configuration_pk PRIMARY KEY (reverse_proxy_authentication_configuration_id)
);
