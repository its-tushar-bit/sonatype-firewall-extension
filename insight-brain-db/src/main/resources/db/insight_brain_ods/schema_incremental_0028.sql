SET SCHEMA insight_brain_ods;

DROP TABLE ldap_configuration;

CREATE TABLE ldap_server (
  ldap_server_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  CONSTRAINT ldap_server_pk PRIMARY KEY (ldap_server_id),
  CONSTRAINT ldap_server_name_uk UNIQUE KEY (name_lowercase_no_whitespace)
);

CREATE TABLE ldap_connection (
  ldap_connection_id varchar(50) NOT NULL,
  ldap_server_id varchar(50) NOT NULL,
  protocol varchar(5) NOT NULL,
  hostname varchar(255) NOT NULL,
  port int(5) NOT NULL,
  search_base varchar(255),
  authentication_method varchar(10) NOT NULL,
  sasl_realm varchar(255),
  system_username varchar(255),
  system_password varchar(255),
  connection_timeout smallint(3), -- in seconds
  retry_delay smallint(3), -- in seconds
  CONSTRAINT ldap_connection_pk PRIMARY KEY (ldap_connection_id),
  CONSTRAINT ldap_connection_server_fk FOREIGN KEY (ldap_server_id) REFERENCES ldap_server(ldap_server_id),
  CONSTRAINT ldap_server_id_uk UNIQUE KEY (ldap_server_id)
);

