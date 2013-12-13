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
  password varchar(128) NOT NULL,
  first_name varchar(100) NOT NULL,
  last_name varchar(100) NOT NULL,
  email varchar(255) NOT NULL,
  CONSTRAINT user_pk PRIMARY KEY (user_id),
  CONSTRAINT user_username_uk UNIQUE KEY (username_lowercase)
);
INSERT INTO user (user_id, username, username_lowercase, password, first_name, last_name, email ) VALUES ('ADMIN', 'admin', 'admin', '$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=', 'Admin', 'BuiltIn', 'admin@localhost');

CREATE TABLE role (
  role_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  description varchar(255) NULL,
  global boolean NOT NULL,
  CONSTRAINT role_pk PRIMARY KEY (role_id),
  CONSTRAINT role_name_uk UNIQUE KEY (name_lowercase_no_whitespace)
);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, description, global) VALUES ('1b92fae3e55a411793a091fb821c422d', 'Administrator', 'administrator', 'Allows full access to the CLM server.', TRUE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, description, global) VALUES ('1cddabf7fdaa47d6833454af10e0a3ef', 'Owner', 'owner', 'Allows to manage policies.', FALSE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, description, global) VALUES ('1da70fae1fd54d6cb7999871ebdb9a36', 'Developer', 'developer', 'Allows to evaluate policies.', FALSE);

CREATE TABLE role_permission (
  role_permission_id varchar(50) NOT NULL,
  role_id varchar(50) NOT NULL,
  permission varchar(50) NOT NULL,
  CONSTRAINT role_permission_pk PRIMARY KEY (role_permission_id),
  CONSTRAINT role_permission_uk UNIQUE KEY (role_id, permission),
  CONSTRAINT role_permission_role_fk FOREIGN KEY (role_id) REFERENCES role(role_id)
);
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1b68169c84874c69b4ac30a391b46212', '1b92fae3e55a411793a091fb821c422d', 'ADMIN');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1c5c83c335e74a138ee1ae8fa7869da7', '1b92fae3e55a411793a091fb821c422d', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1cd867a40a574ce0b46dd22c9d650d1d', '1b92fae3e55a411793a091fb821c422d', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1c8d6f420845466bbc1eb5eaf6d4baa2', '1cddabf7fdaa47d6833454af10e0a3ef', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1c2587ba144341fd9d937dd36c850f5a', '1cddabf7fdaa47d6833454af10e0a3ef', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1a6a3ba60818476781a6a2cb9adcb7f6', '1da70fae1fd54d6cb7999871ebdb9a36', 'READ');

CREATE TABLE membership_mapping (
  membership_mapping_id varchar(50) NOT NULL,
  context_id varchar(50) NOT NULL, -- either 'global' or id of org/app
  role_id varchar(50) NOT NULL,
  member_name varchar(60) NOT NULL,
  member_type varchar(20) NOT NULL,
  CONSTRAINT membership_mapping_pk PRIMARY KEY (membership_mapping_id),
  CONSTRAINT membership_mapping_uk UNIQUE KEY (context_id, role_id, member_name, member_type),
  CONSTRAINT membership_mapping_role_fk FOREIGN KEY (role_id) REFERENCES role(role_id)
);
INSERT INTO membership_mapping (membership_mapping_id, context_id, role_id, member_name, member_type) VALUES ('1d5d75c5a86742549bbf3767a985c6ee', 'global', '1b92fae3e55a411793a091fb821c422d', 'admin', 'USER');

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
  CONSTRAINT ldap_connection_server_id_uk UNIQUE KEY (ldap_server_id)
);

CREATE TABLE ldap_usermapping (
  ldap_usermapping_id varchar(50) NOT NULL,
  ldap_server_id varchar(50) NOT NULL,

  user_basedn varchar(255),
  user_subtree boolean NOT NULL,
  user_object_class varchar(255) NOT NULL,
  user_filter varchar(255),
  user_id_attribute varchar(255) NOT NULL,
  user_realname_attribute varchar(255) NOT NULL,
  user_email_attribute varchar(255) NOT NULL ,
  user_password_attribute varchar(255),

  group_mapping_type varchar(10) NOT NULL,

  group_basedn varchar(255),
  group_subtree boolean NOT NULL,
  group_object_class varchar(255),
  group_id_attribute varchar(255),
  group_member_attribute varchar(255),
  group_member_format varchar(255),

  user_memberofgroup_attribute varchar(255),

  CONSTRAINT ldap_usermapping_pk PRIMARY KEY (ldap_usermapping_id),
  CONSTRAINT ldap_usermapping_server_fk FOREIGN KEY (ldap_server_id) REFERENCES ldap_server(ldap_server_id),
  CONSTRAINT ldap_usermapping_server_id_uk UNIQUE KEY (ldap_server_id)
);

CREATE TABLE policy_monitoring (
  policy_monitoring_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  stage_type_id varchar(50) NOT NULL,
  CONSTRAINT policy_monitoring_pk PRIMARY KEY (policy_monitoring_id),
  CONSTRAINT policy_monitoring_uk UNIQUE KEY (owner_id)
);
