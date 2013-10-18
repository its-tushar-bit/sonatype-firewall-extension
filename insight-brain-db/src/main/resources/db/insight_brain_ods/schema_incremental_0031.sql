SET SCHEMA insight_brain_ods;

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
