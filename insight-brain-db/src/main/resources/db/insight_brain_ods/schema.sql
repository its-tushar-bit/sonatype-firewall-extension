-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE organization (
  organization_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  CONSTRAINT organization_pk PRIMARY KEY (organization_id),
  CONSTRAINT organization_name_uk UNIQUE KEY (name_lowercase_no_whitespace)
);

-- The public_id column is what we expose as AppID to the user
CREATE TABLE application (
  application_id varchar(50) NOT NULL,
  public_id varchar(60) NOT NULL,
  public_id_lowercase varchar(60) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  organization_id varchar(50) NULL,
  CONSTRAINT application_pk PRIMARY KEY (application_id),
  CONSTRAINT application_uk UNIQUE KEY (public_id_lowercase),
  CONSTRAINT application_name_uk UNIQUE KEY (name_lowercase_no_whitespace),
  CONSTRAINT application_organization_fk FOREIGN KEY (organization_id) REFERENCES organization(organization_id)
);

CREATE TABLE label (
  label_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  label varchar(50) NOT NULL,
  label_lowercase varchar(50) NOT NULL,
  color varchar(20) NULL,
  description varchar(255) NULL,
  CONSTRAINT label_pk PRIMARY KEY (label_id),
  CONSTRAINT label_uk UNIQUE KEY (owner_id, label_lowercase)
);

CREATE TABLE component_label (
  component_label_id  varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  label_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  CONSTRAINT component_label_pk PRIMARY KEY (component_label_id),
  CONSTRAINT component_label_label_fk FOREIGN KEY (label_id) REFERENCES label(label_id),
  CONSTRAINT component_label_uk UNIQUE KEY (owner_id, hash, label_id)
);

-- owner_id can be an application or an organization id
CREATE TABLE license_threat_group (
  license_threat_group_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  threat_level smallint(2) NOT NULL,
  CONSTRAINT license_threat_group_pk PRIMARY KEY (license_threat_group_id),
  CONSTRAINT license_threat_group_uk UNIQUE KEY (owner_id, name_lowercase_no_whitespace)
);

-- owner_id can be an application or an organization id
CREATE TABLE license_threat_group_license (
  license_threat_group_license_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  license_threat_group_id varchar(50) NOT NULL,
  license_id varchar(50) NOT NULL,
  CONSTRAINT license_threat_group_license_pk PRIMARY KEY (license_threat_group_license_id),
  CONSTRAINT license_threat_group_license_group_fk FOREIGN KEY (license_threat_group_id) REFERENCES license_threat_group(license_threat_group_id),
  CONSTRAINT license_threat_group_license_uk UNIQUE KEY (license_threat_group_id, license_id)
);

CREATE TABLE hash_gav (
  hash_gav_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  group_id varchar(100) NOT NULL,
  artifact_id varchar(100) NOT NULL,
  version varchar(100) NOT NULL,
  extension varchar(50),
  classifier varchar(50),
  comment varchar(1000) NULL,
  create_time datetime NULL,
  CONSTRAINT hash_gav_pk PRIMARY KEY (hash_gav_id),
  CONSTRAINT hash_gav_hash_uk UNIQUE KEY (hash),
  CONSTRAINT hash_gav_gavec_uk UNIQUE KEY (group_id, artifact_id, version, extension, classifier)
);

CREATE TABLE policy_waiver (
  policy_waiver_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  policy_id varchar(50) NOT NULL,
  constraint_id varchar(50) NULL,
  owner_id varchar(50) NOT NULL,
  comment varchar(1000) NULL,
  create_time datetime NOT NULL,
  CONSTRAINT policy_waiver_pk PRIMARY KEY (policy_waiver_id),
  CONSTRAINT policy_waiver_uk UNIQUE KEY (hash, policy_id, constraint_id, owner_id)
);

CREATE TABLE license_override (
  license_override_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  group_id varchar(100) NOT NULL,
  artifact_id varchar(100) NOT NULL,
  version varchar(100) NOT NULL,
  status varchar(20) NOT NULL,
  license_id varchar(50) NULL,
  comment varchar(1000) NULL,
  CONSTRAINT license_override_pk PRIMARY KEY (license_override_id),
  CONSTRAINT license_override_uk UNIQUE KEY (owner_id, group_id, artifact_id, version)
);

CREATE TABLE user (
  user_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL,
  username_lowercase varchar(60) NOT NULL,
  password varchar(128) NULL,
  first_name varchar(100) NOT NULL,
  last_name varchar(100) NOT NULL,
  email varchar(255) NULL,
  CONSTRAINT user_pk PRIMARY KEY (user_id),
  CONSTRAINT user_username_uk UNIQUE KEY (username_lowercase)
);
INSERT INTO user (user_id, username, username_lowercase, password, first_name, last_name ) VALUES ('ADMIN', 'admin', 'admin', '$shiro1$SHA-256$500000$MQE0sE4AN/+RmveFR2MruQ==$AnBUsybg4CT8HjK7zofGD9A+3xdDZTpUVDpp/K7wX9M=', 'Admin', 'BuiltIn');

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
  connection_timeout smallint(3), -- in seconds
  retry_delay smallint(3), -- in seconds
  CONSTRAINT ldap_configuration_pk PRIMARY KEY (ldap_configuration_id),
  CONSTRAINT ldap_configuration_name_uk UNIQUE KEY (name_lowercase_no_whitespace)
);
