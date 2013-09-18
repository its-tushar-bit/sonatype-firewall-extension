SET SCHEMA insight_brain_ods;

CREATE TABLE ldap_configuration (
  ldap_configuration_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  protocol varchar(5) NOT NULL,
  hostname varchar(255) NOT NULL,
  port int(5) NOT NULL,
  search_base varchar(255),
  authentication_method varchar(10) NOT NULL,
  sasl_realm varchar(255),
  system_username varchar(255),
  system_password varchar(255),
  connection_timeout smallint(3),
  retry_delay smallint(3),
  CONSTRAINT ldap_configuration_pk PRIMARY KEY (ldap_configuration_id),
  CONSTRAINT ldap_configuration_name_uk UNIQUE KEY (name_lowercase_no_whitespace)
);
