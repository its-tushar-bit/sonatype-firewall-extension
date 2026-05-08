-- For tests only
CREATE TABLE IF NOT EXISTS test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE organization (
  organization_id varchar(50) NOT NULL,
  parent_organization_id varchar(50) NULL,
  name varchar(200) NOT NULL,
  name_lowercase_no_whitespace varchar(200) NOT NULL,
  legacy_violation_enabled        boolean,
  allow_legacy_violation_override boolean DEFAULT true NOT NULL, -- Whether policy legacy status can be overridden by children (orgs and apps).
  repository_connection_enabled boolean,
  allow_repository_connection_override boolean DEFAULT true NOT NULL,
  artifactory_connection_enabled boolean,
  allow_artifactory_connection_override boolean DEFAULT true NOT NULL,
  related_repository_manager_id varchar(50),
  related_repository_id varchar(50),
  related_repository_container_id varchar(50),
  CONSTRAINT organization_pk PRIMARY KEY (organization_id),
  CONSTRAINT organization_name_uk UNIQUE (name_lowercase_no_whitespace),
  CONSTRAINT organization_parent_organization_fk FOREIGN KEY (parent_organization_id) REFERENCES organization(organization_id)
);
INSERT INTO organization (organization_id, parent_organization_id, name, name_lowercase_no_whitespace) VALUES('ROOT_ORGANIZATION_ID', null, 'Root Organization', 'rootorganization');

-- The public_id column is what we expose as AppID to the user
CREATE TABLE application (
  application_id varchar(50) NOT NULL,
  public_id varchar(200) NOT NULL,
  public_id_lowercase varchar(200) NOT NULL,
  name varchar(200) NOT NULL,
  name_lowercase_no_whitespace varchar(200) NOT NULL,
  organization_id varchar(50) NOT NULL,
  contact_internal_name varchar(60) NULL, -- The internal name of the contact User (CLM User or LDAP user)
  legacy_violation_enabled boolean,
  repository_connection_enabled boolean,
  artifactory_connection_enabled boolean,
  CONSTRAINT application_pk PRIMARY KEY (application_id),
  CONSTRAINT application_uk UNIQUE (public_id_lowercase),
  CONSTRAINT application_name_uk UNIQUE (name_lowercase_no_whitespace),
  CONSTRAINT application_organization_fk FOREIGN KEY (organization_id) REFERENCES organization(organization_id)
);

CREATE TABLE label (
  label_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  label varchar(50) NOT NULL,
  label_lowercase varchar(50) NOT NULL,
  color varchar(20) NOT NULL,
  description varchar(255) NULL,
  CONSTRAINT label_pk PRIMARY KEY (label_id),
  CONSTRAINT label_uk UNIQUE (owner_id, label_lowercase)
);

CREATE TABLE component_label (
  component_label_id  varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  label_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  CONSTRAINT component_label_pk PRIMARY KEY (component_label_id),
  CONSTRAINT component_label_label_fk FOREIGN KEY (label_id) REFERENCES label(label_id),
  CONSTRAINT component_label_uk UNIQUE (owner_id, hash, label_id)
);

-- owner_id can be an application or an organization id
CREATE TABLE license_threat_group (
  license_threat_group_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  threat_level smallint NOT NULL,
  CONSTRAINT license_threat_group_pk PRIMARY KEY (license_threat_group_id),
  CONSTRAINT license_threat_group_uk UNIQUE (owner_id, name_lowercase_no_whitespace)
);

-- owner_id can be an application or an organization id
CREATE TABLE license_threat_group_license (
  license_threat_group_license_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  license_threat_group_id varchar(50) NOT NULL,
  license_id varchar(1000) NOT NULL,
  CONSTRAINT license_threat_group_license_pk PRIMARY KEY (license_threat_group_license_id),
  CONSTRAINT license_threat_group_license_group_fk FOREIGN KEY (license_threat_group_id) REFERENCES license_threat_group(license_threat_group_id),
  CONSTRAINT license_threat_group_license_uk UNIQUE (license_threat_group_id, license_id)
);

CREATE TABLE hash_component_identifier (
  hash_component_identifier_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL, -- the component identifier coordinates stored in json format
  comment varchar(1000) NULL,
  create_time timestamp NULL,
  claimer_id varchar(60) NULL,
  claimer_name varchar(210) NULL,
  CONSTRAINT hash_component_identifier_pk PRIMARY KEY (hash_component_identifier_id),
  CONSTRAINT hash_component_identifier_component_id_uk UNIQUE (component_id_format, component_id_coordinates_json),
  CONSTRAINT hash_component_identifier_hash_uk UNIQUE (hash)
);

-- owner_id can be an application or an organization id
CREATE TABLE policy (
  policy_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  threat_level smallint NOT NULL,
  legacy_violation_allowed boolean NOT NULL,
  content text NOT NULL,
  drools_code text NOT NULL,
  CONSTRAINT policy_pk PRIMARY KEY (policy_id),
  CONSTRAINT policy_name_uk UNIQUE (owner_id, name_lowercase_no_whitespace)
);

CREATE TABLE policy_waiver (
  policy_waiver_id varchar(50) NOT NULL,
  hash varchar(20) NULL,  -- null if waiver applies to all components of app/org
  policy_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  -- record of the policy constraints/conditions that were violated
  -- it is nullable because legacy policy waivers (before Insight Brain 1.53) do not store this data.
  constraint_facts_json text NULL,
  associated_package_url varchar(1000) NULL,
  component_match_strategy varchar(30) NULL,
  comment varchar(1000) NULL,
  create_time timestamp NOT NULL,
  expiry_time timestamp default NULL,
  creator_id varchar(60) default NULL,
  creator_name varchar(210) default NULL,
  component_upgrade_available boolean,
  waiver_reason_id varchar(50) NULL,
  expire_when_remediation_available boolean DEFAULT false,
  is_for_container_image boolean DEFAULT false,
  is_for_container_image_component boolean DEFAULT false,
  CONSTRAINT policy_waiver_pk PRIMARY KEY (policy_waiver_id),
  CONSTRAINT policy_waiver_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id)
);
CREATE INDEX policy_waiver_owner_id_idx ON policy_waiver(owner_id);

CREATE TABLE policy_waiver_request (
  policy_waiver_request_id varchar(50) NOT NULL,
  hash varchar(20) NULL,  -- null if waiver request applies to all components of app/org
  policy_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  -- record of the policy constraints/conditions that were violated
  constraint_facts_json text NOT NULL,
  associated_package_url varchar(1000) NULL,
  component_match_strategy varchar(30) NOT NULL,
  comment varchar(1000) NULL,
  request_time timestamp NOT NULL,
  expiry_time timestamp default NULL,
  component_upgrade_available boolean,
  waiver_reason_id varchar(50) NULL,
  expire_when_remediation_available boolean DEFAULT false,
  status varchar(20) NOT NULL,
  -- ID of the policy waiver created on policy waiver request approval
  policy_waiver_id varchar(50) NULL,
  requester_id varchar(60) NOT NULL,
  requester_name varchar(210) NOT NULL,
  reviewer_id varchar(60) NULL,
  reviewer_name varchar(210) NULL,
  review_time timestamp NULL,
  rejection_reason text NULL,
  note_to_reviewer text NULL,
  -- ID of the policy violation requested to be waived. Can be an application or repository policy violation.
  policy_violation_id varchar(50) NOT NULL,
  CONSTRAINT policy_waiver_request_pk PRIMARY KEY (policy_waiver_request_id),
  CONSTRAINT policy_waiver_request_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id)
     ON DELETE CASCADE,
  CONSTRAINT policy_waiver_request_policy_waiver_fk FOREIGN KEY (policy_waiver_id) REFERENCES policy_waiver(policy_waiver_id)
     ON DELETE CASCADE
);
CREATE INDEX policy_waiver_request_owner_id_idx ON policy_waiver_request(owner_id);

CREATE TABLE license_override (
  license_override_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL, -- the component identifier coordinates stored in json format
  status varchar(20) NOT NULL,
  comment varchar(1000) NULL,
  CONSTRAINT license_override_pk PRIMARY KEY (license_override_id),
  CONSTRAINT license_override_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json)
);

CREATE TABLE license_override_license (
  license_override_license_id varchar(50) NOT NULL,
  license_override_id varchar(50) NOT NULL,
  license_id varchar(1000) NOT NULL,
  CONSTRAINT license_override_license_pk PRIMARY KEY (license_override_license_id),
  CONSTRAINT license_override_license_uk UNIQUE (license_override_id, license_id),
  CONSTRAINT license_override_license_override_fk FOREIGN KEY (license_override_id) REFERENCES license_override(license_override_id)
);

CREATE TABLE sv_override (
  sv_override_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  source varchar(10) NOT NULL,
  reference_id varchar(20) NOT NULL,
  status varchar(20) NOT NULL,
  comment varchar(1000) NULL,
  CONSTRAINT sv_override_pk PRIMARY KEY (sv_override_id),
  CONSTRAINT sv_override_uk UNIQUE (owner_id, hash, source, reference_id)
);

CREATE TABLE "user" (
  user_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL,
  username_lowercase varchar(60) NOT NULL,
  password varchar(128) NOT NULL,
  first_name varchar(100) NOT NULL,
  last_name varchar(100) NOT NULL,
  email varchar(255) NOT NULL,
  CONSTRAINT user_pk PRIMARY KEY (user_id),
  CONSTRAINT user_username_uk UNIQUE (username_lowercase)
);
INSERT INTO "user" (user_id, username, username_lowercase, password, first_name, last_name, email ) VALUES ('ADMIN', 'admin', 'admin', '$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=', 'Admin', 'BuiltIn', 'admin@localhost');

CREATE TABLE role (
  role_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  sort_order int NOT NULL,
  description varchar(255) NOT NULL,
  global boolean NOT NULL,
  built_in boolean DEFAULT false NOT NULL,
  CONSTRAINT role_pk PRIMARY KEY (role_id),
  CONSTRAINT role_name_uk UNIQUE (name_lowercase_no_whitespace)
);

INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('1b92fae3e55a411793a091fb821c422d', 'System Administrator', 'systemadministrator', 100, 'Manages system configuration and users.', TRUE, TRUE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('b9646757e98e486da7d730025f5245f8', 'Policy Administrator', 'policyadministrator', 150, 'Manages all organizations, applications, policies, and policy violations.', TRUE, TRUE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('1cddabf7fdaa47d6833454af10e0a3ef', 'Owner', 'owner', 200, 'Manages assigned organizations, applications, policies, and policy violations.', FALSE, TRUE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('1da70fae1fd54d6cb7999871ebdb9a36', 'Developer', 'developer', 300, 'Views all information for their assigned organization or application.', FALSE, TRUE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('2cb71b3468d649789163ea2e212b541e', 'Application Evaluator', 'applicationevaluator', 400, 'Evaluates applications and views policy violation summary results.', FALSE, TRUE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('90c7c98683b4471cb77a916744540bcc', 'Component Evaluator', 'componentevaluator', 500, 'Evaluates individual components and views policy violation results for a specified application.', FALSE, TRUE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('0df46317c031440795007f4ce9c7f002', 'Legal Reviewer', 'legalreviewer', 600, 'Reviews legal obligations for component licenses.', FALSE, TRUE);

CREATE TABLE role_permission (
  role_permission_id varchar(50) NOT NULL,
  role_id varchar(50) NOT NULL,
  permission varchar(50) NOT NULL,
  CONSTRAINT role_permission_pk PRIMARY KEY (role_permission_id),
  CONSTRAINT role_permission_uk UNIQUE (role_id, permission),
  CONSTRAINT role_permission_role_fk FOREIGN KEY (role_id) REFERENCES role(role_id)
);
-- System Administrator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1b68169c84874c69b4ac30a391b46212', '1b92fae3e55a411793a091fb821c422d', 'CONFIGURE_SYSTEM');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('971e6e9fa55e402f9809a814993261d8', '1b92fae3e55a411793a091fb821c422d', 'VIEW_ROLES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('3229ffe4e3a945c3bf11ab649ffa875e', '1b92fae3e55a411793a091fb821c422d', 'ACCESS_AUDIT_LOG');
-- Policy Administrator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1539fa2c5afd4cd4b7102ef6c8d0bf6b', 'b9646757e98e486da7d730025f5245f8', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('c49843ffa4ae4bb68c3e35b25244486e', 'b9646757e98e486da7d730025f5245f8', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('0353b7d5b3134c0f9546b07fea0b37a1', 'b9646757e98e486da7d730025f5245f8', 'EDIT_ACCESS_CONTROL');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('06af8c357eb04568bd73b246440ea063', 'b9646757e98e486da7d730025f5245f8', 'MANAGE_PROPRIETARY');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('6da52edbbc554b3ab4dd502e30a80acd', 'b9646757e98e486da7d730025f5245f8', 'EVALUATE_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('e07e0e487c074a4fa95a1abde2f68aed', 'b9646757e98e486da7d730025f5245f8', 'EVALUATE_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('00286ae0ccf5441391333f050c25170b', 'b9646757e98e486da7d730025f5245f8', 'VIEW_ROLES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('869815aecdc849a8ade21ffc5ccc41ea', 'b9646757e98e486da7d730025f5245f8', 'EDIT_ROLES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('f3fe74d34577471c936c332293c5ba0a', 'b9646757e98e486da7d730025f5245f8', 'CLAIM_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('f3fe74d34577471c936c332293c5ba0b', 'b9646757e98e486da7d730025f5245f8', 'ADD_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('043024fcd34c47a1bea907cc0c84955f', 'b9646757e98e486da7d730025f5245f8', 'MANAGE_AUTOMATIC_APPLICATION_CREATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('0df19389f12947fabfa3028afb28eb26', 'b9646757e98e486da7d730025f5245f8', 'MANAGE_AUTOMATIC_SCM_CONFIGURATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('d6760c9dc2d1425a925cfb1296dbbbf9', 'b9646757e98e486da7d730025f5245f8', 'WAIVE_POLICY_VIOLATIONS');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1a837897990c489faf669f2b091427fd', 'b9646757e98e486da7d730025f5245f8', 'CHANGE_LICENSES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('08dc877fe2664ed995b39f97e253254a', 'b9646757e98e486da7d730025f5245f8', 'CHANGE_SECURITY_VULNERABILITIES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('7b87527087c24a3c9ba81a55d7da7c0c', 'b9646757e98e486da7d730025f5245f8', 'LEGAL_REVIEWER');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('e31b018313034e65848b02ac51c97d99', 'b9646757e98e486da7d730025f5245f8', 'CREATE_PULL_REQUESTS');
-- Owner role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1c8d6f420845466bbc1eb5eaf6d4baa2', '1cddabf7fdaa47d6833454af10e0a3ef', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1c2587ba144341fd9d937dd36c850f5a', '1cddabf7fdaa47d6833454af10e0a3ef', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('c10918766e564505835b1f9edfd8655d', '1cddabf7fdaa47d6833454af10e0a3ef', 'EDIT_ACCESS_CONTROL');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('fe3a0e01fa4543386d74aadf7fbaddc1', '1cddabf7fdaa47d6833454af10e0a3ef', 'MANAGE_PROPRIETARY');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('465f023ab44040deb2c5a3b7c3dd3c77', '1cddabf7fdaa47d6833454af10e0a3ef', 'EVALUATE_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('33fbb19ce93e4420a0ebcd846b0705d5', '1cddabf7fdaa47d6833454af10e0a3ef', 'EVALUATE_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1bbcd0fe101449f09c3682fe6837267e', '1cddabf7fdaa47d6833454af10e0a3ef', 'VIEW_ROLES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1bbcd0fe101449f09c3682fe6837267f', '1cddabf7fdaa47d6833454af10e0a3ef', 'ADD_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('938168536594435aaae3e58668b7c46c', '1cddabf7fdaa47d6833454af10e0a3ef', 'WAIVE_POLICY_VIOLATIONS');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('40340d38f51745829f914139ee66764d', '1cddabf7fdaa47d6833454af10e0a3ef', 'CHANGE_LICENSES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1c65f82a496540a3a6a369875cd988a8', '1cddabf7fdaa47d6833454af10e0a3ef', 'CHANGE_SECURITY_VULNERABILITIES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('629fe173516645fda0121b2a6602ed0f', '1cddabf7fdaa47d6833454af10e0a3ef', 'LEGAL_REVIEWER');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('442e70c98fb6473db43562af7174005b', '1cddabf7fdaa47d6833454af10e0a3ef', 'CREATE_PULL_REQUESTS');
-- Developer role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1a6a3ba60818476781a6a2cb9adcb7f6', '1da70fae1fd54d6cb7999871ebdb9a36', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('61159b674eb94cdcb00ebdec77a47373', '1da70fae1fd54d6cb7999871ebdb9a36', 'EVALUATE_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('38c2855e278244b6ad3fad92c942b33e', '1da70fae1fd54d6cb7999871ebdb9a36', 'CREATE_PULL_REQUESTS');
-- Application Evaluator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('ea7bb57e93e241acbf8da5ebcb5b0074', '2cb71b3468d649789163ea2e212b541e', 'EVALUATE_APPLICATION');
-- Component Evaluator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('f198535bdf2549d38417534e38ae3cda', '90c7c98683b4471cb77a916744540bcc', 'EVALUATE_COMPONENT');
-- Legal Reviewer role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('6c521879906a4fdf8d27c652787243b4', '0df46317c031440795007f4ce9c7f002', 'LEGAL_REVIEWER');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('9c3d3a466bed410fa8d8c8801f3a0c13', '0df46317c031440795007f4ce9c7f002', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('4209ad3cdcfd474b865c51c0d664ea2a', '0df46317c031440795007f4ce9c7f002', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('16ef4730b20f42e6bcfc14afcd42cd6d', '0df46317c031440795007f4ce9c7f002', 'CHANGE_LICENSES');

CREATE TABLE membership_mapping (
  membership_mapping_id varchar(50) NOT NULL,
  context_id varchar(50) NOT NULL, -- either 'global' or id of org/app
  role_id varchar(50) NOT NULL,
  member_name varchar(200) NOT NULL,
  member_type varchar(20) NOT NULL,
  CONSTRAINT membership_mapping_pk PRIMARY KEY (membership_mapping_id),
  CONSTRAINT membership_mapping_uk UNIQUE (context_id, role_id, member_name, member_type),
  CONSTRAINT membership_mapping_role_fk FOREIGN KEY (role_id) REFERENCES role(role_id)
);
CREATE INDEX membership_mapping_member_name_idx ON membership_mapping(member_name);
CREATE INDEX membership_mapping_role_id_member_type_member_name_idx ON membership_mapping (role_id, member_type, member_name);
INSERT INTO membership_mapping (membership_mapping_id, context_id, role_id, member_name, member_type) VALUES ('1d5d75c5a86742549bbf3767a985c6ee', 'global', '1b92fae3e55a411793a091fb821c422d', 'admin', 'USER');
INSERT INTO membership_mapping (membership_mapping_id, context_id, role_id, member_name, member_type) VALUES ('c20a1df68fa948b787f3d1962411fb50', 'global', 'b9646757e98e486da7d730025f5245f8', 'admin', 'USER');

CREATE TABLE ldap_server (
  ldap_server_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  priority int NOT NULL,
  CONSTRAINT ldap_server_pk PRIMARY KEY (ldap_server_id),
  CONSTRAINT ldap_server_name_uk UNIQUE (name_lowercase_no_whitespace),
  CONSTRAINT ldap_server_priority_uk UNIQUE (priority)
);

CREATE TABLE ldap_connection (
  ldap_connection_id varchar(50) NOT NULL,
  ldap_server_id varchar(50) NOT NULL,
  protocol varchar(5) NOT NULL,
  hostname varchar(255) NOT NULL,
  port int NOT NULL,
  search_base varchar(255),
  referral_ignored boolean DEFAULT false NOT NULL,
  authentication_method varchar(10) NOT NULL,
  sasl_realm varchar(255),
  system_username varchar(255),
  system_password varchar(255),
  connection_timeout smallint, -- in seconds
  retry_delay smallint, -- in seconds
  CONSTRAINT ldap_connection_pk PRIMARY KEY (ldap_connection_id),
  CONSTRAINT ldap_connection_server_fk FOREIGN KEY (ldap_server_id) REFERENCES ldap_server(ldap_server_id),
  CONSTRAINT ldap_connection_server_id_uk UNIQUE (ldap_server_id)
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

  dynamic_group_search_enabled boolean DEFAULT true NOT NULL,

  CONSTRAINT ldap_usermapping_pk PRIMARY KEY (ldap_usermapping_id),
  CONSTRAINT ldap_usermapping_server_fk FOREIGN KEY (ldap_server_id) REFERENCES ldap_server(ldap_server_id),
  CONSTRAINT ldap_usermapping_server_id_uk UNIQUE (ldap_server_id)
);

CREATE TABLE policy_monitoring (
  policy_monitoring_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  stage_type_id varchar(50) NOT NULL,
  CONSTRAINT policy_monitoring_pk PRIMARY KEY (policy_monitoring_id),
  CONSTRAINT policy_monitoring_uk UNIQUE (owner_id, stage_type_id)
);

CREATE TABLE tag (
  tag_id varchar(50) NOT NULL,
  organization_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  description varchar(255) NOT NULL,
  color varchar(20) NOT NULL,
  CONSTRAINT tag_pk PRIMARY KEY (tag_id),
  CONSTRAINT tag_uk UNIQUE (organization_id, name_lowercase_no_whitespace),
  CONSTRAINT tag_organization_fk FOREIGN KEY (organization_id) REFERENCES organization(organization_id)
);

CREATE TABLE application_tag (
  application_tag_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  tag_id varchar(50) NOT NULL,
  CONSTRAINT application_tag_pk PRIMARY KEY (application_tag_id),
  CONSTRAINT application_tag_uk UNIQUE (application_id, tag_id),
  CONSTRAINT application_tag_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE,
  CONSTRAINT application_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag(tag_id)
);

CREATE TABLE policy_tag (
  policy_tag_id varchar(50) NOT NULL,
  policy_id varchar(50) NOT NULL,
  tag_id varchar(50) NOT NULL,
  CONSTRAINT policy_tag_pk PRIMARY KEY (policy_tag_id),
  CONSTRAINT policy_tag_uk UNIQUE (policy_id, tag_id),
  CONSTRAINT policy_tag_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id),
  CONSTRAINT policy_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag(tag_id)
);

CREATE TABLE policy_evaluation (
  policy_evaluation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  scan_id varchar(50) NOT NULL,
  reevaluation bool DEFAULT false NOT NULL,
  for_monitoring bool DEFAULT false NOT NULL,
  for_obsolete_scan bool DEFAULT false NOT NULL,
  time timestamp NOT NULL,
  commit_hash varchar(128),
  initiator varchar(60) NOT NULL,
  scan_trigger_type varchar(50) NOT NULL,
  client_scan_type varchar(50) NULL,
  branch_name varchar(512) NULL,
  CONSTRAINT policy_evaluation_pk PRIMARY KEY (policy_evaluation_id),
  CONSTRAINT policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE
);
CREATE INDEX policy_evaluation_scan_id_idx ON policy_evaluation(scan_id);
CREATE INDEX policy_evaluation_time_idx ON policy_evaluation(time);
CREATE INDEX policy_evaluation_app_monitoring_stage_idx ON policy_evaluation(application_id, for_monitoring, stage_type_id);
CREATE INDEX policy_evaluation_app_time_idx ON policy_evaluation(application_id, time);
CREATE INDEX policy_evaluation_commit_hash_idx ON policy_evaluation(commit_hash);
CREATE INDEX policy_evaluation_reeval_time_idx ON policy_evaluation (reevaluation, stage_type_id, application_id, time DESC);

CREATE TABLE policy_violation_constraint_facts (
   policy_violation_constraint_facts_id VARCHAR(20) NOT NULL,
   constraint_facts_json text NOT NULL,
   CONSTRAINT policy_violation_constraint_facts_pk PRIMARY KEY (policy_violation_constraint_facts_id)
);

CREATE TABLE policy_violation (
  policy_violation_id varchar(50) NOT NULL,

  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,

  -- summary of the policy that caused the violation
  policy_id varchar(50) NOT NULL, -- no foreign key constraint to policy, policies can be deleted at any time
  policy_name varchar(60) NOT NULL,
  threat_level smallint NOT NULL,
  threat_category varchar(20) NOT NULL,

  -- identification of the component that caused the violation
  hash varchar(20),
  component_id_format varchar(50),
  component_id_coordinates_json varchar(1000),
  filename varchar(1000),

  -- record of the most recent policy constraints/conditions that were violated
  constraint_facts_json text,

  -- the most recent action during the violation's lifetime
  action_type_id varchar(20),

  -- timestamps recording the state and transitions thereof for the violation
  open_time timestamp NOT NULL,    -- when the violation first occurred
  waive_time timestamp NULL,       -- when the violation was waived
  legacy_violation_time timestamp NULL, -- when there is a legacy policy violation
  fix_time timestamp NULL,         -- when the violation disappeared entirely

  -- details of the waiver that suppressed this violation
  policy_waiver_id varchar(50) NULL,  -- no foreign key constraint to policy_waiver, waivers can be deleted at any time
  policy_waiver_comment varchar(1000) NULL,

  -- whether the violation was ever encountered during a non-re-evaluation, supports notifications for ordinary evaluations
  seen_by_primary_evaluation bool NOT NULL,
  -- whether the violation was ever encountered during policy monitoring, supports separate notifications for policy monitoring
  seen_by_monitoring_evaluation bool NOT NULL,

  -- whether the legacy violation was applied by a new evaluation or a re-evaluation of a report
  legacy_violation_applied bool NOT NULL DEFAULT false,

  reachability_status varchar(20) NULL,

  auto_policy_waiver_id varchar(50) NULL,

  constraint_facts_id varchar(20),

  is_remediated_by_version_change boolean DEFAULT NULL,

  CONSTRAINT policy_violation_pk PRIMARY KEY (policy_violation_id),
  CONSTRAINT policy_violation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE,
  CONSTRAINT policy_violation_constraint_facts_id_fk FOREIGN KEY (constraint_facts_id)
      REFERENCES policy_violation_constraint_facts(policy_violation_constraint_facts_id)
);
CREATE INDEX policy_violation_app_fix_time_stage_idx ON policy_violation(application_id, fix_time, stage_type_id);
CREATE INDEX policy_violation_policy_app_idx ON policy_violation(policy_id, application_id);
CREATE INDEX policy_violation_hash_idx ON policy_violation(hash);
CREATE INDEX policy_violation_open_time_idx ON policy_violation (open_time);

CREATE TABLE dashboard_filter (
  dashboard_filter_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL, -- The internal name of the User
  username_lowercase varchar(60) NOT NULL,
  realm_id varchar(50) NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  based_on_filter_name varchar(60),
  acknowledged boolean DEFAULT false NOT NULL,
  filter_json text NOT NULL, -- The dashboard filter stored in json format
  CONSTRAINT dashboard_filter_pk PRIMARY KEY (dashboard_filter_id),
  CONSTRAINT dashboard_filter_uk UNIQUE (username_lowercase, realm_id, name_lowercase_no_whitespace)
);

CREATE TABLE application_component (
  application_component_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  time timestamp NOT NULL,
  hash varchar(20) NOT NULL,
  component_id_format varchar(50),
  component_id_coordinates_json varchar(1000), -- the component identifier coordinates stored in json format
  match_state_id varchar(20) NOT NULL,
  identification_source_id varchar(20) NOT NULL,
  proprietary bool DEFAULT false NOT NULL,
  pathnames text, -- the paths to the component that caused the policy violation, paths are new line delimited
  CONSTRAINT application_component_pk PRIMARY KEY (application_component_id),
  CONSTRAINT application_component_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE,
  CONSTRAINT application_component_uk UNIQUE (application_id, stage_type_id, hash)
);
CREATE INDEX application_component_hash_idx ON application_component(hash);
CREATE INDEX application_component_time_idx ON application_component(time);

CREATE TABLE aggregate_file (
  aggregate_file_id varchar(50) NOT NULL,
  application_component_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  pathnames text,
  CONSTRAINT aggregate_file_pk PRIMARY KEY (aggregate_file_id),
  CONSTRAINT aggregate_file_application_component_fk FOREIGN KEY (application_component_id) REFERENCES application_component(application_component_id) ON DELETE CASCADE,
  CONSTRAINT aggregate_file_uk UNIQUE (application_component_id, hash)
);
CREATE INDEX aggregate_file_application_component_id_idx ON aggregate_file(application_component_id);

CREATE TABLE last_policy_evaluation (
  policy_evaluation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  CONSTRAINT last_policy_evaluation_PK PRIMARY KEY (policy_evaluation_id),
  CONSTRAINT last_policy_evaluation_uk UNIQUE (application_id, stage_type_id),
  CONSTRAINT last_policy_evaluation_eval_fk FOREIGN KEY (policy_evaluation_id)
    REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE,
  CONSTRAINT last_policy_evaluation_app_fk FOREIGN KEY (application_id)
    REFERENCES application(application_id) ON DELETE CASCADE
);

CREATE TABLE user_viewed_product_notification (
  user_viewed_product_notification_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL, -- The internal name of the User
  username_lowercase varchar(60) NOT NULL,
  realm_id varchar(50) NULL,
  notification_id varchar(50) NOT NULL,
  CONSTRAINT notification_viewed_pk PRIMARY KEY (user_viewed_product_notification_id),
  CONSTRAINT notification_viewed_uk UNIQUE (notification_id, username_lowercase, realm_id)
);

CREATE TABLE repository_manager (
  repository_manager_id varchar(50) NOT NULL,
  instance_id varchar(50) NOT NULL,
  user_agent varchar(300),
  -- The repository manager can be marked as configured without a configure time if it existed in the db before these fields were added.
  configured boolean DEFAULT true NOT NULL,
  configure_time timestamp NULL,
  name varchar(200) default NULL,
  name_lowercase_no_whitespace varchar(200) default NULL,
  product_name varchar(50) NULL, -- For ex, "Nexus", "JFrog Artifactory"
  product_version varchar(100) NULL, -- For ex, "3.59.0"
  base_url varchar(2048) NULL,
  related_organization_id varchar(50) NULL,
  CONSTRAINT repository_manager_pk PRIMARY KEY (repository_manager_id),
  CONSTRAINT repository_manager_uk UNIQUE (instance_id),
  CONSTRAINT repository_manager_name_uk UNIQUE (name_lowercase_no_whitespace),
  CONSTRAINT repository_manager_related_organization_fk FOREIGN KEY (related_organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL
);

CREATE TABLE repository (
  repository_id varchar(50) NOT NULL,
  repository_manager_id varchar(50) NOT NULL,
  public_id varchar(500) NOT NULL,
  repository_type varchar(10) DEFAULT 'proxy' NOT NULL, -- proxy or hosted
  audit_enabled bool DEFAULT true NOT NULL,
  quarantine_enabled bool DEFAULT false NOT NULL,
  policy_compliant_component_selection_enabled boolean DEFAULT false NOT NULL,
  namespace_confusion_protection_enabled boolean DEFAULT false NOT NULL,
  format varchar(50),
  last_manual_configure_time timestamp DEFAULT NULL,
  related_organization_id varchar(50) NULL,
  monitoring_enabled boolean DEFAULT false NOT NULL,
  CONSTRAINT repository_pk PRIMARY KEY (repository_id),
  CONSTRAINT repository_uk UNIQUE (repository_manager_id, public_id),
  CONSTRAINT repository_repository_manager_fk FOREIGN KEY (repository_manager_id) REFERENCES repository_manager(repository_manager_id),
  CONSTRAINT repository_related_organization_fk FOREIGN KEY (related_organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL
);

CREATE TABLE repository_component (
  repository_component_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  pathname varchar(1000) NOT NULL,
  time timestamp NOT NULL,
  hash varchar(20) NOT NULL,
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000), -- the component identifier coordinates stored in json format
  display_name varchar(1000),
  match_state_id varchar(20) NOT NULL,
  identification_source_id varchar(20) NOT NULL,
  last_evaluation_time timestamp NOT NULL,
  quarantine_time timestamp,
  unquarantine_time timestamp,
  analyzer_features_json varchar(1000), -- the analyzer features stored in json format
  auto_unquarantined boolean,
  component_id varchar(255),
  last_evaluation_stage varchar(50),
  CONSTRAINT repository_component_pk PRIMARY KEY (repository_component_id),
  CONSTRAINT repository_component_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  CONSTRAINT repository_component_uk UNIQUE (repository_id, pathname)
);
CREATE INDEX repository_component_hash_idx ON repository_component(hash);
CREATE INDEX repository_component_repository_unquarantine_idx ON repository_component(repository_id, unquarantine_time);
CREATE INDEX repository_component_quarantine_idx ON repository_component(repository_id, quarantine_time);
CREATE INDEX repository_component_release_quarantine_idx ON repository_component (quarantine_time, unquarantine_time, auto_unquarantined);
CREATE INDEX repository_component_component_coordinates_idx ON repository_component (component_id_format, component_id_coordinates_json);
CREATE INDEX repository_component_last_evaluation_time_idx ON repository_component(last_evaluation_time);

CREATE TABLE repository_policy_violation (
  repository_policy_violation_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  pathname varchar(1000) NOT NULL,
  time timestamp NOT NULL,
  policy_id varchar(50) NOT NULL,
  policy_name varchar(60) NOT NULL, -- the policy name as it was when the policy violation was generated
  threat_level smallint NOT NULL,
  threat_category varchar(20) NOT NULL,
  hash varchar(20),
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000), -- the component identifier coordinates (that caused the policy violation) stored in json format
  constraint_facts_json text, -- the constraint facts (that caused the policy violation) stored in json format
  action_type_id varchar(20),
  waived bool DEFAULT false NOT NULL,
  active bool DEFAULT true NOT NULL, -- Whether this violation is still active. If false, then the component was removed from the repository or a more recent evaluation was performed for this component.
  policy_waiver_id varchar(50) NULL,        -- no foreign key constraint to policy_waiver, waivers can be deleted at any time
  policy_waiver_comment varchar(1000) NULL,
  waive_time timestamp NULL,                -- when the violation was waived
  constraint_facts_id varchar(20),
  component_id varchar(255),

  CONSTRAINT repository_policy_violation_pk PRIMARY KEY (repository_policy_violation_id),
  CONSTRAINT repository_policy_violation_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  CONSTRAINT repository_policy_violation_constraint_facts_id_fk FOREIGN KEY (constraint_facts_id)
      REFERENCES policy_violation_constraint_facts(policy_violation_constraint_facts_id)
);
CREATE INDEX repository_policy_violation_pathname_idx ON repository_policy_violation(pathname);
CREATE INDEX repository_policy_violation_repository_id_idx ON repository_policy_violation(repository_id);

CREATE TABLE proprietary_component_name_pattern (
  proprietary_component_name_pattern_id varchar(50) NOT NULL,

  format varchar(50) NOT NULL,
  namespace_pattern varchar(300) NOT NULL,
  name_pattern varchar(300) NOT NULL,

  repository_id varchar(50) NOT NULL,
  enabled boolean DEFAULT true NOT NULL,

  CONSTRAINT proprietary_component_name_pattern_pk PRIMARY KEY (proprietary_component_name_pattern_id),
  CONSTRAINT proprietary_component_name_pattern_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  -- to make this uniqueness constraint work as desired, note that all included columns are not nullable, using empty strings instead if needed
  CONSTRAINT proprietary_component_name_pattern_uk UNIQUE (format, namespace_pattern, name_pattern, repository_id)
);
CREATE INDEX proprietary_component_name_pattern_repo_idx ON proprietary_component_name_pattern(repository_id);

CREATE TABLE proprietary_config (
  proprietary_config_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  packages_json text NULL,
  regexes_json text NULL,
  CONSTRAINT proprietary_config_pk PRIMARY KEY (proprietary_config_id),
  CONSTRAINT proprietary_config_owner_uk UNIQUE (owner_id)
);

CREATE TABLE webhook (
  webhook_id varchar(50) NOT NULL,
  url varchar(2048) NOT NULL,
  description varchar(2048),
  secret_key varchar(512),
  context varchar(20),
  CONSTRAINT webhook_pk PRIMARY KEY (webhook_id)
);

CREATE TABLE webhook_event_type (
  webhook_id  varchar(50) NOT NULL,
  event_type varchar(50) NOT NULL,
  CONSTRAINT webhook_event_type_pk PRIMARY KEY (webhook_id, event_type),
  CONSTRAINT webhook_event_type_fk FOREIGN KEY (webhook_id) REFERENCES webhook(webhook_id)
);

CREATE TABLE system_notice (
  system_notice_id varchar(50) NOT NULL,
  message varchar(500) NOT NULL,
  enabled boolean NOT NULL,
  CONSTRAINT system_notice_pk PRIMARY KEY (system_notice_id)
);
-- Add  default system notice
INSERT INTO system_notice (system_notice_id, message, enabled) VALUES ('system-notice', '', false);

CREATE TABLE system_configuration_property (
  system_configuration_property_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  value varchar(4000) NOT NULL,
  CONSTRAINT system_configuration_property_pk PRIMARY KEY (system_configuration_property_id),
  CONSTRAINT system_configuration_property_name_uk UNIQUE (name)
);
-- Add  default value for SUCCESS_METRICS_ENABLED (true)
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('39ae05576fcf474da6771b9f879759f7', 'SUCCESS_METRICS_ENABLED', 'true');
-- Add default values for automatic application configuration settings
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('3a927fda2992470e9fc479376702c6b5', 'AUTOMATIC_APPLICATION_CREATION_ENABLED', 'false');
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('3ba8d0f1601946efb376fd841a149bb0', 'AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID', '');
-- Add default values for automatic source control configuration settings
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('f488d35a40d24ba589ba14280c40fe04', 'AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED', 'false');
-- Since 1.100
-- advanced search is turned on by default for new installations.
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('917095b878f8cc8ab116d2647df4d597', 'ADVANCED_SEARCH_ENABLED', 'true');
-- Since 1.119
-- Security vulnerability source policy condition is disabled by default
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('c3722a88a76d463f8eb07420e11477c1', 'SECURITY_VULNERABILITY_SOURCE_CONDITION_DISABLED', 'true');
-- Since 1.136
-- Anonymous access to the Quarantined Component View is enabled by default
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('9f5c3b813bd9488293447c54bff4d660', 'QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS', 'true');
-- Since 1.159
-- Waived component upgrade inspection runs at midnight by default
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('dc866d71378a41ce9da2526904bd88e9', 'waivedComponentUpgradeInspectionHour', '1');
-- Since 1.161
-- ALP Observed License Detection is enable by default
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('531170d2fc694631a05f4d8ab61e53b9', 'alpObservedLicenseDetectionEnabled', 'true');
-- Since 1.186
-- Policy violations enabled for SBOM Manager by default
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('64716cfdb4b741e793bdd7fbc44bc93p', 'sbomPolicies', 'true');


CREATE TABLE data_retention_policy (
  data_retention_policy_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  context_id varchar(30) NOT NULL,
  purging_enabled boolean NOT NULL,
  max_count smallint,
  max_age_in_days smallint,
  CONSTRAINT data_retention_policy_pk PRIMARY KEY (data_retention_policy_id),
  CONSTRAINT data_retention_policy_uk UNIQUE (owner_id, context_id)
);
-- Add  default retention policies for root organization
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a0', 'ROOT_ORGANIZATION_ID', 'develop', true, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a7', 'ROOT_ORGANIZATION_ID', 'source', true, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a1', 'ROOT_ORGANIZATION_ID', 'build', true, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a2', 'ROOT_ORGANIZATION_ID', 'stage-release', true, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a3', 'ROOT_ORGANIZATION_ID', 'release', true, 3650);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a4', 'ROOT_ORGANIZATION_ID', 'operate', true, 3650);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a5', 'ROOT_ORGANIZATION_ID', 'continuous-monitoring', true, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a6', 'ROOT_ORGANIZATION_ID', 'success-metrics', true, 365);

-- Since 1.201
-- GitHub App registration and installation data
CREATE TABLE IF NOT EXISTS github_app (
  github_app_id VARCHAR(50) NOT NULL,
  owner_id VARCHAR(50) NOT NULL,
  app_id INTEGER NOT NULL,
  slug VARCHAR(256),
  client_id VARCHAR(256) NOT NULL,
  client_secret VARCHAR(512) NOT NULL,
  private_key TEXT NOT NULL,
  installation_id BIGINT,
  github_organization_name VARCHAR(255) NOT NULL,
  last_updated_at TIMESTAMP NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT false,
  CONSTRAINT github_app_pk PRIMARY KEY (github_app_id),
  CONSTRAINT github_app_app_id_uk UNIQUE (app_id),
  CONSTRAINT github_app_installation_id_uk UNIQUE (installation_id)
);

-- Since 1.202
-- GitHub App installation state for OAuth + PKCE flow
CREATE TABLE IF NOT EXISTS github_app_installation_state
(
    github_app_installation_state_id VARCHAR(50) NOT NULL,
    state_token                      VARCHAR(255) NOT NULL,
    github_app_id                    VARCHAR(50) NOT NULL,
    expires_at                       TIMESTAMP NOT NULL,
    created_at                       TIMESTAMP NOT NULL,
    CONSTRAINT github_app_installation_state_pk PRIMARY KEY (github_app_installation_state_id),
    CONSTRAINT github_app_installation_state_state_token_uk UNIQUE (state_token),
    CONSTRAINT fk_installation_state_github_app FOREIGN KEY (github_app_id) REFERENCES github_app(github_app_id)
);

-- Since 1.202
-- GitHub App registration state for manifest flow
CREATE TABLE IF NOT EXISTS github_app_registration_state
(
    github_app_registration_state_id VARCHAR(50) NOT NULL,
    state_token                      VARCHAR(255) NOT NULL,
    owner_id                         VARCHAR(50) NOT NULL,
    github_organization_name         VARCHAR(255) NULL,
    expires_at                       TIMESTAMP NOT NULL,
    created_at                       TIMESTAMP NOT NULL,
    CONSTRAINT github_app_registration_state_pk PRIMARY KEY (github_app_registration_state_id),
    CONSTRAINT github_app_registration_state_state_token_uk UNIQUE (state_token)
);

-- source control repository data associated with an organization or application (owner)
CREATE TABLE source_control (
  source_control_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  repository_url varchar(2048),
  normalized_repository_url varchar(2048),
  repository_ssh_url varchar(2048) NULL,
  username varchar(256),
  token varchar(512),
  provider varchar(20),
  base_branch varchar(243),
  ssh_enabled boolean DEFAULT false,
  remediation_pull_requests_enabled boolean,
  status_checks_enabled boolean,
  pull_request_commenting_enabled boolean,
  source_control_evaluations_enabled boolean,
  source_control_scan_target varchar(1000),
  pull_request_poll_time timestamp NULL,
  pull_request_error_count INT NOT NULL DEFAULT 0,
  commit_status_enabled boolean,
  manual_pull_requests_enabled boolean,
  inner_source_automated_updates_enabled boolean,
  close_pr_on_failed_checks_enabled boolean,
  close_pr_after_days_open_enabled boolean,
  close_pr_after_days INTEGER NULL,
  authentication_type VARCHAR(20) DEFAULT 'PAT',
  non_golden_pull_requests_enabled boolean,
  CONSTRAINT source_control_pk PRIMARY KEY (source_control_id),
  CONSTRAINT source_control_owner_id_uk UNIQUE (owner_id)
);
CREATE INDEX source_control_normalized_repository_url_idx ON source_control(normalized_repository_url);
CREATE INDEX source_control_pull_request_poll_time_idx ON source_control(pull_request_poll_time);

-- Since 1.140
CREATE TABLE source_control_configuration (
  source_control_configuration_id varchar(50) NOT NULL,
  clone_directory varchar(1000) NOT NULL,
  git_implementation varchar(20),
  pr_comment_purge_window int,
  pr_event_purge_window int,
  git_executable varchar(1000),
  git_timeout_seconds int NOT NULL DEFAULT 0,
  commit_username varchar(256),
  commit_email varchar(256),
  use_username_in_repository_clone_url boolean NOT NULL DEFAULT false,
  default_branch_monitoring_start_time varchar(5),
  default_branch_monitoring_interval_hours int NOT NULL DEFAULT 24,
  pull_request_monitoring_interval_seconds int NOT NULL DEFAULT 60,
  gpg_signing_key varchar(1024),
  gpg_passphrase varchar(1024),
  CONSTRAINT source_control_configuration_pk PRIMARY KEY (source_control_configuration_id)
);

CREATE TABLE IF NOT EXISTS schema_version (
  data_store_id varchar(32) NOT NULL,
  schema_version int NOT NULL
);
INSERT INTO schema_version (data_store_id , schema_version) VALUES ('insight_brain_ods', -1);

CREATE TABLE migration_tracker (
    migration_tracker_id varchar(100) NOT NULL,
    version int NULL,
    configuration varchar(1000) NULL,
    CONSTRAINT migration_tracker_pk PRIMARY KEY (migration_tracker_id)
);
INSERT INTO migration_tracker(migration_tracker_id) VALUES('root-organization');
INSERT INTO migration_tracker(migration_tracker_id, version) VALUES('policy-drools-code', 2);
INSERT INTO migration_tracker(migration_tracker_id, version) VALUES('policy-json', 1);
INSERT INTO migration_tracker(migration_tracker_id) VALUES('ignored-repository-components');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('inactive-repository-violations');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('mail-config');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('proxy-server-configuration');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('source-control-file-storage');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('reverse-proxy-authentication-config');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('base-url-config');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('jira-config');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('source-control-config');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('simple-config');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('obsoletescanfiles-cleaned');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('repository-component-display-name');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('PolicyViolationConstraintFactsJsonAsyncDbMigration');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('DisplayNameForFileCoordinateAsyncDbMigration');
INSERT INTO migration_tracker(migration_tracker_id) VALUES('PolicyViolationIndexAsyncDbMigration');

CREATE TABLE search_index_change (
  search_index_change_id varchar(50) NOT NULL,
  change_type varchar(100) NOT NULL,
  change_data varchar(2000) NOT NULL,
  CONSTRAINT search_index_change_pk PRIMARY KEY (search_index_change_id)
);
INSERT INTO search_index_change (search_index_change_id, change_type, change_data) VALUES ('2cb96c0b02724c38a6e0bdedeec45d8f', 'ORGANIZATION', 'ROOT_ORGANIZATION_ID');

-- Since 1.72.0
CREATE TABLE saml_configuration (
  saml_configuration_id varchar(50) NOT NULL,
  configuration_json text NOT NULL, -- the SAML configuration stored in json format
  keystore bytea NOT NULL,
  keystore_password_obfuscated varchar(200) NOT NULL,
  CONSTRAINT saml_configuration_pk PRIMARY KEY (saml_configuration_id)
);

-- Since 1.75.0
CREATE TABLE user_token (
  user_token_id varchar(50) NOT NULL,
  username varchar(200) NOT NULL,
  user_code varchar(128) NOT NULL,
  pass_code varchar(128) NOT NULL,
  realm_id varchar(50) NOT NULL,
  create_time timestamp NOT NULL,
  last_access_time timestamp NULL,
  CONSTRAINT user_token_pk PRIMARY KEY (user_token_id),
  CONSTRAINT user_token_uk UNIQUE (username, realm_id),
  CONSTRAINT user_token_user_code_uk UNIQUE (user_code)
);

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

-- Since 1.86
CREATE TABLE source_control_pull_request_comment
(
  source_control_pull_request_comment_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  component_hash varchar(20),
  pathname varchar(1000) NULL,
  pull_request_id int NOT NULL,
  pull_request_comment_id bigint NOT NULL,
  pull_request_comment_version int default NULL,
  source_policy_evaluation_id varchar(50) NOT NULL,
  target_policy_evaluation_id varchar(50) NOT NULL,
  create_time timestamp NOT NULL,
  update_time timestamp,
  content_hash varchar(40),
  CONSTRAINT source_control_pull_request_comment_pk PRIMARY KEY (source_control_pull_request_comment_id),
  CONSTRAINT source_control_pull_request_comment_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE,
  CONSTRAINT source_control_pull_request_source_policy_eval_fk FOREIGN KEY (source_policy_evaluation_id)
    REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE,
  CONSTRAINT source_control_pull_request_target_policy_eval_fk FOREIGN KEY (target_policy_evaluation_id)
    REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE,
  CONSTRAINT source_control_pull_request_comment_uk UNIQUE (application_id, component_hash, pull_request_id, pathname)
);

-- Since 1.86
CREATE TABLE source_control_default_branch_commit_history (
  source_control_default_branch_commit_history_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  commit_hash varchar(128) NOT NULL,
  commit_time timestamp NOT NULL,
  policy_evaluation_id varchar(50),
  create_time timestamp NOT NULL,
  update_time timestamp,
  CONSTRAINT source_control_default_branch_commit_history_pk PRIMARY KEY (source_control_default_branch_commit_history_id),
  CONSTRAINT source_control_default_branch_commit_history_application_fk FOREIGN KEY (application_id)
    REFERENCES application (application_id) ON DELETE CASCADE,
  CONSTRAINT source_control_default_branch_commit_history_policy_eval_fk FOREIGN KEY (policy_evaluation_id)
    REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE,
  CONSTRAINT source_control_default_branch_commit_history_uk UNIQUE (application_id, commit_hash)
);

-- Since 1.95
CREATE TABLE source_control_event (
  source_control_event_id varchar(50) NOT NULL,
  instance_id varchar(50),
  application_id varchar(50) NOT NULL,
  event_type varchar(50) NOT NULL,
  event_priority int NOT NULL DEFAULT 2,
  event_status varchar(50) NOT NULL,
  event_status_details varchar(2048),
  event_error_details text NULL,
  commit_hash varchar(128),
  base_commit_hash varchar(128),
  policy_evaluation_id varchar(50),
  policy_evaluation_outcome varchar(20),
  critical_component_count int NOT NULL DEFAULT 0,
  severe_component_count int NOT NULL DEFAULT 0,
  moderate_component_count int NOT NULL DEFAULT 0,
  scan_id varchar(50),
  stage_type_id varchar(30),
  component_id_format varchar(50),
  component_id_coordinates_json varchar(1000),
  branch_name varchar(512),
  scan_targets_json text NULL, -- list of scan targets stored in json format
  base_branch_name varchar(512),
  remediation_version varchar(100),
  pull_request_contents text,
  pull_request_number int,
  scm_username varchar(255),
  initiator varchar(60),
  create_time timestamp NOT NULL,
  start_time timestamp,
  complete_time timestamp,
  status_id varchar(50),
  user_agent varchar(255),
  scan_trigger_type varchar(50),
  is_golden_pull_request boolean,
  CONSTRAINT source_control_event_pk PRIMARY KEY (source_control_event_id),
  CONSTRAINT source_control_event_application_fk FOREIGN KEY (application_id)
    REFERENCES application (application_id) ON DELETE CASCADE,
  CONSTRAINT source_control_event_policy_evaluation_fk FOREIGN KEY (policy_evaluation_id)
    REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE
);
CREATE INDEX source_control_event_instance_id_idx ON source_control_event(instance_id);
CREATE INDEX source_control_event_create_time_idx ON source_control_event(create_time);
CREATE INDEX source_control_event_event_status_idx ON source_control_event(event_status);

CREATE TABLE reevaluate_cascade_request
(
  reevaluate_cascade_request_id varchar(50) NOT NULL,
  component_reference_hash varchar(50) NOT NULL,
  created_at timestamp DEFAULT now() NOT NULL,
  created_by_username varchar(255) NOT NULL,
  status varchar(50) DEFAULT 'PENDING' NOT NULL,
  CONSTRAINT reevaluate_cascade_request_pk PRIMARY KEY (reevaluate_cascade_request_id)
);

CREATE TABLE reevaluate_cascade_progress
(
  reevaluate_cascade_progress_id varchar(50) NOT NULL,
  reevaluate_cascade_request_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  repository_component_id varchar(50) NOT NULL,
  quarantined boolean,
  status varchar(20) NOT NULL,
  CONSTRAINT reevaluate_cascade_progress_pk PRIMARY KEY (reevaluate_cascade_progress_id),
  CONSTRAINT reevaluate_cascade_request_id_fk FOREIGN KEY (reevaluate_cascade_request_id) REFERENCES reevaluate_cascade_request(reevaluate_cascade_request_id) ON DELETE CASCADE,
  CONSTRAINT repository_id_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id) ON DELETE CASCADE,
  CONSTRAINT repository_component_id_fk FOREIGN KEY (repository_component_id) REFERENCES repository_component(repository_component_id) ON DELETE CASCADE
);

-- Since 1.114
CREATE TABLE source_control_pull_request
(
  source_control_pull_request_id varchar(50) NOT NULL,
  repository_url varchar(2048) NOT NULL,
  pull_request_id int NOT NULL,
  head_commit_hash varchar(128) NOT NULL,
  base_commit_hash varchar(128),
  branch_name varchar(512) NOT NULL,
  base_branch_name varchar(512),
  create_time timestamp NOT NULL,
  last_check_time timestamp NOT NULL,
  last_detected_update_time timestamp NOT NULL,
  state varchar(50),
  source varchar(50),
  CONSTRAINT source_control_pull_request_pk PRIMARY KEY (source_control_pull_request_id),
  -- The "source_control_pull_request_uk" name is taken by a constraint incorrectly named in schema_incremental_0171
  CONSTRAINT source_control_pull_request_uk1 UNIQUE (repository_url, pull_request_id)
);
CREATE INDEX source_control_pull_request_last_detected_update_time_idx ON source_control_pull_request (last_detected_update_time);
CREATE INDEX source_control_pull_request_source_idx ON source_control_pull_request(source);

-- Since 1.96
CREATE TABLE product_license (
  product_license_id varchar(50) NOT NULL,
  license_key varchar(8192) NOT NULL,
  license_details varchar(8192),
  CONSTRAINT product_license_pk PRIMARY KEY (product_license_id)
);

-- Since 1.98
CREATE TABLE persisted_policy_evaluation_polling_result (
  persisted_policy_evaluation_polling_result_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  status_id varchar(50) NOT NULL,
  policy_evaluation_polling_result_json text NOT NULL,
  create_time timestamp NOT NULL,
  CONSTRAINT persisted_policy_evaluation_polling_result_pk PRIMARY KEY (persisted_policy_evaluation_polling_result_id),
  CONSTRAINT persisted_policy_evaluation_polling_result_uk UNIQUE (application_id, status_id)
);
CREATE INDEX persisted_policy_evaluation_polling_result_create_time_idx ON persisted_policy_evaluation_polling_result(create_time);

-- Since 1.98
CREATE TABLE persisted_user_session (
  persisted_user_session_id varchar(50) NOT NULL,
  session_json text NOT NULL,
  CONSTRAINT persisted_user_session_pk PRIMARY KEY (persisted_user_session_id)
);

-- Since 1.98
CREATE TABLE persisted_promote_scan_result (
  persisted_promote_scan_result_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  status varchar(50) NOT NULL,
  scan_id varchar(50) NULL,
  error_message varchar(1000) NULL,
  create_time timestamp NOT NULL,
  CONSTRAINT persisted_promote_scan_result_pk PRIMARY KEY (persisted_promote_scan_result_id)
);
CREATE INDEX persisted_promote_scan_result_create_time_idx ON persisted_promote_scan_result(create_time);

CREATE TABLE inner_source_application (
  -- Represents the fact that an IQ application produces an InnerSource package
  -- with the specified versionless purl.  See `inner_source_version` for the package
  -- version in each IQ stage.
  inner_source_application_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  package_url varchar(1000) NOT NULL,
  CONSTRAINT inner_source_application_pk PRIMARY KEY (inner_source_application_id),
  CONSTRAINT inner_source_application_uk UNIQUE (package_url),
  CONSTRAINT inner_source_application_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id) ON DELETE CASCADE
);

CREATE TABLE inner_source_version (
  -- Represents the latest version of an InnerSource application produced at the given stage.
  inner_source_version_id varchar(50) NOT NULL,
  inner_source_application_id varchar(50) NOT NULL,
  latest_version varchar(200) NOT NULL,
  stage_type_id varchar(50),
  CONSTRAINT inner_source_version_pk PRIMARY KEY (inner_source_version_id),
  CONSTRAINT inner_source_version_uk UNIQUE (inner_source_application_id, stage_type_id),
  CONSTRAINT inner_source_version_inner_source_application_fk FOREIGN KEY (inner_source_application_id) REFERENCES inner_source_application (inner_source_application_id) ON DELETE CASCADE
);

-- Since 1.98
CREATE TABLE persisted_scan_ticket (
  persisted_scan_ticket_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  scan_id varchar(50),
  state_id varchar(50) NOT NULL,
  error_id varchar(50),
  create_time timestamp NOT NULL,
  CONSTRAINT persisted_scan_ticket_pk PRIMARY KEY (persisted_scan_ticket_id)
);
CREATE INDEX persisted_scan_ticket_create_time_idx ON persisted_scan_ticket(create_time);

-- Since 1.98
CREATE TABLE repository_migration (
  repository_migration_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  state varchar(50) NOT NULL,
  CONSTRAINT repository_migration_pk PRIMARY KEY (repository_migration_id),
  CONSTRAINT repository_migration_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  CONSTRAINT repository_id_uk UNIQUE (repository_id)
);

-- Since 1.100
CREATE TABLE perpetual_lock (
  perpetual_lock_id VARCHAR(1100) NOT NULL,
  owner VARCHAR(50),
  expiration_time timestamp,
  category varchar(50) NOT NULL DEFAULT 'source-control',
  CONSTRAINT perpetual_lock_id_pk PRIMARY KEY (perpetual_lock_id)
);

-- Since 1.104
CREATE TABLE application_component_license (
  application_component_license_id varchar(50) NOT NULL,
  application_component_id varchar(50) NOT NULL,
  effective_license_id varchar(1000) NOT NULL,
  CONSTRAINT application_component_license_pk PRIMARY KEY (application_component_license_id),
  CONSTRAINT application_component_license_application_component_fk FOREIGN KEY (application_component_id) REFERENCES application_component(application_component_id) ON DELETE CASCADE,
  CONSTRAINT application_component_license_uk UNIQUE (application_component_id, effective_license_id)
);
CREATE INDEX application_component_license_effective_license_id_idx ON application_component_license(effective_license_id);

-- Since 1.105
CREATE TABLE component_copyright (
  component_copyright_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  legal_content_hash varchar(64) NOT NULL,
  last_updated_by_username varchar(256) NOT NULL,
  last_updated_at timestamp NOT NULL,
  CONSTRAINT component_copyright_pk PRIMARY KEY (component_copyright_id),
  CONSTRAINT component_copyright_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json)
);
CREATE INDEX component_copyright_owner_component_idx ON component_copyright(owner_id, component_id_format, component_id_coordinates_json);

-- Since 1.105
CREATE TABLE copyright_override (
  copyright_override_id varchar(50) NOT NULL,
  original_content_hash varchar(64),
  content_hash varchar(64) NOT NULL,
  content varchar(1000) NOT NULL,
  status varchar(20) NOT NULL,
  component_copyright_id varchar(50) NOT NULL,
  CONSTRAINT copyright_override_pk PRIMARY KEY (copyright_override_id),
  CONSTRAINT copyright_override_component_copyright_fk FOREIGN KEY (component_copyright_id) REFERENCES component_copyright(component_copyright_id)
);
CREATE INDEX copyright_override_component_copyright_id_idx ON copyright_override(component_copyright_id);

-- Since 1.105
CREATE TABLE component_legal_file (
  component_legal_file_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  type varchar(20) NOT NULL,
  legal_content_hash varchar(64) NOT NULL,
  last_updated_by_username varchar(256) NOT NULL,
  last_updated_at timestamp NOT NULL,
  CONSTRAINT component_legal_file_pk PRIMARY KEY (component_legal_file_id),
  CONSTRAINT component_legal_file_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json, type)
);
CREATE INDEX component_legal_file_owner_component_type_idx ON component_legal_file(owner_id, component_id_format, component_id_coordinates_json, type);

-- Since 1.105
CREATE TABLE legal_file_override (
  legal_file_override_id varchar(50) NOT NULL,
  original_content_hash varchar(64),
  content_hash varchar(64) NOT NULL,
  content text NOT NULL,
  status varchar(20) NOT NULL,
  component_legal_file_id varchar(50) NOT NULL,
  CONSTRAINT legal_file_override_pk PRIMARY KEY (legal_file_override_id),
  CONSTRAINT legal_file_override_component_legal_file_fk FOREIGN KEY (component_legal_file_id) REFERENCES component_legal_file(component_legal_file_id)
);
CREATE INDEX legal_file_override_component_legal_file_id_idx ON legal_file_override(component_legal_file_id);

-- Since 1.105
CREATE TABLE component_obligation (
  component_obligation_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  obligation_name varchar(256) NOT NULL,
  comment varchar(1000),
  status varchar(20) NOT NULL,
  legal_content_hash varchar(64) NOT NULL,
  last_updated_by_username varchar(256) NOT NULL,
  last_updated_at timestamp NOT NULL,
  CONSTRAINT component_obligation_pk PRIMARY KEY (component_obligation_id),
  CONSTRAINT component_obligation_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json, obligation_name)
);
CREATE INDEX component_obligation_owner_component_obligation_idx ON component_obligation(owner_id, component_id_format, component_id_coordinates_json, obligation_name);

-- Since 1.105
CREATE TABLE component_obligation_attribution (
  component_obligation_attribution_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  obligation_name varchar(256),
  content varchar(1000) NOT NULL,
  legal_content_hash varchar(64) NOT NULL,
  last_updated_by_username varchar(256) NOT NULL,
  last_updated_at timestamp NOT NULL,
  CONSTRAINT component_obligation_attribution_pk PRIMARY KEY (component_obligation_attribution_id)
);
CREATE INDEX component_obligation_attribution_owner_component_obligation_idx ON component_obligation_attribution(owner_id, component_id_format, component_id_coordinates_json, obligation_name);

-- Since 1.105
CREATE TABLE user_filter (
  user_filter_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL, -- The internal name of the User
  username_lowercase varchar(60) NOT NULL,
  realm_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  filter_json text NOT NULL, -- The filter stored in json format
  based_on_filter_name varchar(60),
  filter_type varchar(100) NOT NULL,
  CONSTRAINT user_filter_pk PRIMARY KEY (user_filter_id),
  CONSTRAINT user_filter_uk UNIQUE (username_lowercase, realm_id, name_lowercase_no_whitespace, filter_type)
);

-- Since 1.107
CREATE TABLE auto_unquarantine_policy_condition_type (
  condition_type_id varchar(100) NOT NULL, -- stores the id from ConditionType
  CONSTRAINT auto_unquarantine_policy_condition_type_pk PRIMARY KEY (condition_type_id)
);

-- Since 1.120
CREATE TABLE attribution_report_template (
  attribution_report_template_id varchar(50) NOT NULL PRIMARY KEY,
  template_name varchar(250) NOT NULL,
  document_title varchar(250) NOT NULL,
  document_header varchar(500),
  document_footer varchar(500),
  include_table_of_contents boolean DEFAULT TRUE,
  include_standard_license_texts boolean DEFAULT TRUE,
  include_appendix boolean DEFAULT TRUE,
  include_inner_source boolean DEFAULT FALSE,
  include_sonatype_special_licenses boolean DEFAULT FALSE,
  last_updated_at timestamp NOT NULL,
  CONSTRAINT template_name_uk UNIQUE (template_name)
);

-- Since 1.125
CREATE TABLE quarantined_component_access (
  quarantined_component_access_id varchar(50) NOT NULL PRIMARY KEY,
  repository_id varchar(50) NOT NULL,
  repository_component_id varchar(50) NOT NULL,
  generate_time timestamp NOT NULL,
  CONSTRAINT quarantined_component_access_repository_fk FOREIGN KEY (repository_id) REFERENCES repository (repository_id),
  CONSTRAINT quarantined_component_access_repository_component_fk FOREIGN KEY (repository_component_id) REFERENCES repository_component (repository_component_id)
);
CREATE INDEX quarantined_component_access_repository_id_idx ON quarantined_component_access(repository_id);
CREATE INDEX quarantined_component_access_repository_component_id_idx ON quarantined_component_access(repository_component_id);

-- Since 1.127
CREATE TABLE repository_client_configuration
(
    repository_client_configuration_id varchar(50) NOT NULL,
    connection_timeout                 smallint, -- in seconds
    socket_timeout                     smallint, -- in seconds
    CONSTRAINT repository_client_configuration_pk PRIMARY KEY (repository_client_configuration_id)
);

-- Since 1.127
CREATE TABLE repository_connection
(
    repository_connection_id varchar(50)   NOT NULL,
    owner_id                 varchar(50)   NOT NULL,
    base_url                 varchar(2048) NOT NULL,
    format                   varchar(50)   NOT NULL DEFAULT 'GENERIC',
    username                 varchar(255),
    password                 varchar(255),
    CONSTRAINT repository_connection_pk PRIMARY KEY (repository_connection_id),
    CONSTRAINT repository_connection_url_uk UNIQUE (owner_id, format)
);

-- Since 1.133
CREATE TABLE component_source_link (
  component_source_link_id varchar(50) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  owner_id varchar(50) NOT NULL,
  last_updated_by_username varchar(256) NOT NULL,
  last_updated_at timestamp NOT NULL,
  CONSTRAINT component_source_link_pk PRIMARY KEY (component_source_link_id),
  CONSTRAINT component_source_link_uk UNIQUE (owner_id, component_id_format, component_id_coordinates_json)
);
CREATE INDEX component_source_link_owner_component_idx ON component_source_link(owner_id, component_id_format, component_id_coordinates_json);

-- Since 1.133
CREATE TABLE source_link_override (
  source_link_override_id varchar(50) NOT NULL,
  content varchar(1000) NOT NULL,
  original_content varchar(1000) NOT NULL,
  status varchar(20) NOT NULL,
  component_source_link_id varchar(50) NOT NULL,
  CONSTRAINT source_link_override_pk PRIMARY KEY (source_link_override_id),
  CONSTRAINT source_link_override_component_source_link_fk FOREIGN KEY (component_source_link_id) REFERENCES component_source_link(component_source_link_id)
);
CREATE INDEX source_link_override_component_source_link_id_idx ON source_link_override(component_source_link_id);

-- Since 1.133
CREATE TABLE saml_user (
  saml_user_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL,
  first_name varchar(100) NULL,
  last_name varchar(100) NULL,
  email varchar(255) NULL,
  groups text NULL,
  CONSTRAINT saml_user_pk PRIMARY KEY (saml_user_id),
  CONSTRAINT saml_user_username_uk UNIQUE (username)
);

-- Since 1.157
CREATE TABLE saml_group(
  saml_group_id varchar(50) NOT NULL,
  name text NOT NULL,
  CONSTRAINT saml_group_pk PRIMARY KEY (saml_group_id)
);

-- Since 1.157
CREATE TABLE saml_user_group (
  saml_user_group_id varchar(50) NOT NULL,
  saml_user_id varchar(50) NOT NULL,
  saml_group_id varchar(50) NOT NULL,
  CONSTRAINT saml_user_group_pk PRIMARY KEY (saml_user_group_id),
  CONSTRAINT saml_user_group_uk UNIQUE (saml_user_id, saml_group_id),
  CONSTRAINT saml_user_group_user_fk FOREIGN KEY (saml_user_id) REFERENCES saml_user(saml_user_id),
  CONSTRAINT saml_user_group_group_fk FOREIGN KEY (saml_group_id) REFERENCES saml_group(saml_group_id)
);

-- Since 1.134
CREATE TABLE crowd_configuration (
  crowd_configuration_id varchar(50) NOT NULL,
  server_url varchar(2048) NOT NULL,
  application_name varchar(255) NOT NULL,
  application_password varchar(255) NOT NULL,
  CONSTRAINT crowd_configuration_pk PRIMARY KEY (crowd_configuration_id)
);

-- Since 1.136
CREATE TABLE artifactory_connection (
  artifactory_connection_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  base_url varchar(2048) NOT NULL,
  username varchar(255),
  password text,
  CONSTRAINT artifactory_connection_pk PRIMARY KEY (artifactory_connection_id)
);

-- Since 1.137
CREATE TABLE repository_identified_component (
  hash varchar(64) NOT NULL,
  component_id_format varchar(10) NOT NULL,
  component_id_coordinates_json varchar(1000) NOT NULL,
  create_time timestamp NOT NULL,
  last_access_time timestamp NOT NULL,
  CONSTRAINT hash_pk PRIMARY KEY (hash)
);

-- Since 1.138
CREATE TABLE reverse_proxy_authentication_configuration (
  reverse_proxy_authentication_configuration_id varchar(50) NOT NULL,
  enabled boolean NOT NULL,
  username_header varchar(255) NOT NULL,
  csrf_protection_disabled boolean NOT NULL,
  logout_url varchar(2048),
  CONSTRAINT reverse_proxy_authentication_configuration_pk PRIMARY KEY (reverse_proxy_authentication_configuration_id)
);

-- Since 1.139
CREATE TABLE jira_configuration (
  jira_configuration_id varchar(50) NOT NULL,
  url varchar(2048) NOT NULL,
  username varchar(255),
  password varchar(2000),
  custom_fields_json varchar(8192),
  CONSTRAINT jira_configuration_pk PRIMARY KEY (jira_configuration_id)
);

-- Since 1.146
CREATE TABLE source_control_pull_request_result (
  source_control_pull_request_result_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  pull_request_result_json text NOT NULL,
  CONSTRAINT source_control_pull_request_result_pk PRIMARY KEY (source_control_pull_request_result_id),
  CONSTRAINT source_control_pull_request_result_application_fk FOREIGN KEY (application_id)
    REFERENCES application (application_id) ON DELETE CASCADE
);
CREATE INDEX source_control_pull_request_result_application_id_idx ON source_control_pull_request_result(application_id);

-- Since 1.146
CREATE TABLE vulnerability_group (
  vulnerability_group_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  CONSTRAINT vulnerability_group_pk PRIMARY KEY (vulnerability_group_id),
  CONSTRAINT vulnerability_group_uk UNIQUE (owner_id, name)
);

-- Since 1.146
CREATE TABLE vulnerability_group_vulnerability (
  vulnerability_group_vulnerability_id varchar(50) NOT NULL,
  vulnerability_group_id varchar(50) NOT NULL,
  vulnerability_refid varchar(100) NOT NULL,
  CONSTRAINT vulnerability_group_vulnerability_pk PRIMARY KEY (vulnerability_group_vulnerability_id),
  CONSTRAINT vulnerability_group_vulnerability_fk FOREIGN KEY (vulnerability_group_id) REFERENCES vulnerability_group (vulnerability_group_id),
  CONSTRAINT vulnerability_group_vulnerability_uk UNIQUE (vulnerability_group_id, vulnerability_refid)
);

-- Since 1.160
CREATE TABLE vulnerability_custom_remediation
(
  vulnerability_custom_remediation_id VARCHAR(50) NOT NULL,
  owner_id VARCHAR(50) NOT NULL,
  refid VARCHAR(20) NOT NULL,
  component_id_format VARCHAR(10),
  component_id_coordinates_json VARCHAR(1000),
  remediation VARCHAR(3000) NOT NULL,
  last_updated_by_username VARCHAR(256) NOT NULL,
  last_updated_at TIMESTAMP NOT NULL,
  CONSTRAINT vulnerability_custom_remediation_pk PRIMARY KEY (vulnerability_custom_remediation_id),
  CONSTRAINT vulnerability_custom_remediation_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);

-- Since 1.160
CREATE TABLE vulnerability_custom_remediation_tag
(
  vulnerability_custom_remediation_tag_id VARCHAR(50) NOT NULL,
  vulnerability_custom_remediation_id VARCHAR(50) NOT NULL,
  tag_id VARCHAR(50) NOT NULL,
  CONSTRAINT vulnerability_custom_remediation_tag_id PRIMARY KEY (vulnerability_custom_remediation_tag_id),
  CONSTRAINT vulnerability_custom_remediation_tag_remediation_fk FOREIGN KEY (vulnerability_custom_remediation_id) REFERENCES vulnerability_custom_remediation (vulnerability_custom_remediation_id),
  CONSTRAINT vulnerability_custom_remediation_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
  CONSTRAINT vulnerability_custom_remediation_tag_uk UNIQUE (vulnerability_custom_remediation_id, tag_id)
);

-- Since 1.160
CREATE TABLE vulnerability_custom_cwe
(
  vulnerability_custom_cwe_id VARCHAR(50) NOT NULL,
  owner_id VARCHAR(50) NOT NULL,
  refid VARCHAR(20) NOT NULL,
  component_id_format VARCHAR(10),
  component_id_coordinates_json VARCHAR(1000),
  cwe VARCHAR(50) NOT NULL,
  last_updated_by_username VARCHAR(256) NOT NULL,
  last_updated_at TIMESTAMP NOT NULL,
  CONSTRAINT vulnerability_custom_cwe_pk PRIMARY KEY (vulnerability_custom_cwe_id),
  CONSTRAINT vulnerability_custom_cwe_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);

-- Since 1.160
CREATE TABLE vulnerability_custom_cwe_tag
(
  vulnerability_custom_cwe_tag_id VARCHAR(50) NOT NULL,
  vulnerability_custom_cwe_id VARCHAR(50) NOT NULL,
  tag_id VARCHAR(50) NOT NULL,
  CONSTRAINT vulnerability_custom_cwe_tag_id PRIMARY KEY (vulnerability_custom_cwe_tag_id),
  CONSTRAINT vulnerability_custom_cwe_tag_cwe_fk FOREIGN KEY (vulnerability_custom_cwe_id) REFERENCES vulnerability_custom_cwe (vulnerability_custom_cwe_id),
  CONSTRAINT vulnerability_custom_cwe_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
  CONSTRAINT vulnerability_custom_cwe_tag_uk UNIQUE (vulnerability_custom_cwe_id, tag_id)
);

-- Since 1.160
CREATE TABLE vulnerability_custom_cvss_vector
(
  vulnerability_custom_cvss_vector_id VARCHAR(50) NOT NULL,
  owner_id VARCHAR(50) NOT NULL,
  refid VARCHAR(20) NOT NULL,
  component_id_format VARCHAR(10),
  component_id_coordinates_json VARCHAR(1000),
  vector VARCHAR(200) NOT NULL,
  last_updated_by_username VARCHAR(256) NOT NULL,
  last_updated_at TIMESTAMP NOT NULL,
  CONSTRAINT vulnerability_custom_cvss_vector_pk PRIMARY KEY (vulnerability_custom_cvss_vector_id),
  CONSTRAINT vulnerability_custom_cvss_vector_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);

-- Since 1.160
CREATE TABLE vulnerability_custom_cvss_vector_tag
(
  vulnerability_custom_cvss_vector_tag_id VARCHAR(50) NOT NULL,
  vulnerability_custom_cvss_vector_id VARCHAR(50) NOT NULL,
  tag_id VARCHAR(50) NOT NULL,
  CONSTRAINT vulnerability_custom_cvss_vector_tag_id PRIMARY KEY (vulnerability_custom_cvss_vector_tag_id),
  CONSTRAINT vulnerability_custom_cvss_vector_tag_cvss_fk FOREIGN KEY (vulnerability_custom_cvss_vector_id) REFERENCES vulnerability_custom_cvss_vector (vulnerability_custom_cvss_vector_id),
  CONSTRAINT vulnerability_custom_cvss_vector_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
  CONSTRAINT vulnerability_custom_cvss_vector_tag_uk UNIQUE (vulnerability_custom_cvss_vector_id, tag_id)
);

-- Since 1.160
CREATE TABLE vulnerability_custom_cvss_severity
(
  vulnerability_custom_cvss_severity_id VARCHAR(50) NOT NULL,
  owner_id VARCHAR(50) NOT NULL,
  refid VARCHAR(20) NOT NULL,
  component_id_format VARCHAR(10),
  component_id_coordinates_json VARCHAR(1000),
  severity FLOAT NOT NULL,
  last_updated_by_username VARCHAR(256) NOT NULL,
  last_updated_at TIMESTAMP NOT NULL,
  CONSTRAINT vulnerability_custom_cvss_severity_pk PRIMARY KEY (vulnerability_custom_cvss_severity_id),
  CONSTRAINT vulnerability_custom_cvss_severity_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);

-- Since 1.160
CREATE TABLE vulnerability_custom_cvss_severity_tag
(
  vulnerability_custom_cvss_severity_tag_id VARCHAR(50) NOT NULL,
  vulnerability_custom_cvss_severity_id VARCHAR(50) NOT NULL,
  tag_id VARCHAR(50) NOT NULL,
  CONSTRAINT vulnerability_custom_cvss_severity_tag_id PRIMARY KEY (vulnerability_custom_cvss_severity_tag_id),
  CONSTRAINT vulnerability_custom_cvss_severity_tag_cvss_fk FOREIGN KEY (vulnerability_custom_cvss_severity_id) REFERENCES vulnerability_custom_cvss_severity (vulnerability_custom_cvss_severity_id),
  CONSTRAINT vulnerability_custom_cvss_severity_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
  CONSTRAINT vulnerability_custom_cvss_severity_tag_uk UNIQUE (vulnerability_custom_cvss_severity_id, tag_id)
);

-- Since 1.162
CREATE TABLE source_control_organization_import_event
(
    source_control_organization_import_event_id VARCHAR(50)   NOT NULL,
    organization_id                             varchar(50)   NOT NULL,
    source_control_host_url                     VARCHAR(2048) NOT NULL,
    desired_sub_organization_count              INT           NOT NULL,
    import_limit                                INT           NOT NULL,
    import_status                               VARCHAR(20)   NOT NULL,
    import_success_count                        INT           NOT NULL,
    import_failure_count                        INT           NOT NULL,
    start_time                                  timestamp     NOT NULL,
    last_updated_time                           timestamp     NOT NULL,
    import_errors                               TEXT,
    CONSTRAINT source_control_organization_import_event_id PRIMARY KEY (source_control_organization_import_event_id),
    CONSTRAINT source_control_organization_import_event_organization_fk FOREIGN KEY (organization_id) REFERENCES organization (organization_id)
);

-- Since 1.162
CREATE TABLE deleted_tenant
(
  tenant_slug                varchar(61) NOT NULL,
  created                    timestamp   default CURRENT_TIMESTAMP(6),
  last_updated               timestamp   default NULL,
  delete_completed_date      timestamp   default NULL,
  CONSTRAINT deleted_tenant_pk PRIMARY KEY (tenant_slug)
);

-- Since 1.162
CREATE TABLE IF NOT EXISTS tenant_metadata
(
  tenant_metadata_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  application_name varchar(100) NOT NULL,
  connection_id varchar(50) NOT NULL,
  connection_name varchar(100) NOT NULL,
  encryption_key_name varchar(512),
  organization_id varchar(50),
  organization_name varchar(100),
  CONSTRAINT tenant_metadata_pk PRIMARY KEY (tenant_metadata_id)
);

-- since 1.162
-- SDEV-228 This table records the last timestamp of a user using IDE to initiate a policy evaluation
CREATE TABLE user_ide_policy_evaluation (
  user_ide_policy_evaluation_id varchar(50) NOT NULL,
  username varchar(200) NOT NULL,
  last_evaluation_time timestamp NOT NULL,
  CONSTRAINT user_ide_policy_evaluation_pk PRIMARY KEY (user_ide_policy_evaluation_id),
  CONSTRAINT username_uk UNIQUE (username)
);

-- Since 1.169
CREATE TABLE application_count_history
(
  application_count_history_id          varchar(50) NOT NULL,
  application_count                     integer     NOT NULL,
  updated_date                          timestamp   NOT NULL,
  scm_feedback_enabled_count            integer     NOT NULL,
  policy_action_failures_by_app_count   integer     NOT NULL DEFAULT 0,
  waivers_count                         integer     NOT NULL DEFAULT 0,
  mean_time_to_remediate_ms             bigint      NOT NULL DEFAULT 0,
  CONSTRAINT application_count_history_pk PRIMARY KEY (application_count_history_id)
);

CREATE INDEX application_count_history_updated_date ON application_count_history (updated_date);

INSERT INTO application_count_history (application_count_history_id,
                                       application_count,
                                       scm_feedback_enabled_count,
                                       policy_action_failures_by_app_count,
                                       waivers_count,
                                       mean_time_to_remediate_ms,
                                       updated_date)
VALUES ('initialization',
       0,
       0,
       0,
       0,
       0,
       NOW());

-- since 1.172
CREATE TABLE sast_scm_scan_context
(
  sast_scm_scan_context_id                    varchar(50) NOT NULL,
  branch_name                                 varchar(512) NOT NULL,
  commit_hash                                 varchar(128) NOT NULL,
  created_at                                  timestamp NOT NULL,
  CONSTRAINT sast_scm_scan_context_pk PRIMARY KEY (sast_scm_scan_context_id)
);
CREATE INDEX sast_scm_scan_context_branch_name_idx ON sast_scm_scan_context (branch_name);
CREATE INDEX sast_scm_scan_context_commit_hash_idx ON sast_scm_scan_context (commit_hash);

CREATE TABLE sast_scan
(
    sast_scan_id                                varchar(50) NOT NULL,
    application_id                              varchar(50) NOT NULL,
    created_at                                  timestamp NOT NULL,
    sast_scm_scan_context_id varchar(50) NULL,
    CONSTRAINT sast_scan_pk PRIMARY KEY (sast_scan_id),
    CONSTRAINT sast_scan_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
    CONSTRAINT sast_scm_scan_context_fk FOREIGN KEY (sast_scm_scan_context_id) REFERENCES sast_scm_scan_context(sast_scm_scan_context_id) ON DELETE SET NULL
);
CREATE INDEX sast_scan_sast_scm_scan_context_id_idx ON sast_scan (sast_scm_scan_context_id);

CREATE TABLE sast_finding
(
    sast_finding_id                varchar(50) NOT NULL,
    sast_scan_id                   varchar(50) NOT NULL,
    coordinate                     varchar(2000) NOT NULL,
    line_number                    integer,
    cwe                            varchar(50) NOT NULL,
    severity                       smallint NOT NULL,
    confidence                     smallint NOT NULL,
    rule_name                      varchar(200) NOT NULL,
    description                    text NOT NULL,
    CONSTRAINT sast_finding_pk PRIMARY KEY (sast_finding_id),
    CONSTRAINT sast_finding_sast_scan_fk FOREIGN KEY (sast_scan_id) REFERENCES sast_scan(sast_scan_id) ON DELETE CASCADE
);

CREATE TABLE sast_remediation
(
    sast_remediation_id            varchar(50) NOT NULL,
    sast_finding_id                varchar(50) NOT NULL,
    content                        text NOT NULL,
    CONSTRAINT sast_remediation_pk PRIMARY KEY (sast_remediation_id),
    CONSTRAINT sast_remediation_sast_finding_fk FOREIGN KEY (sast_finding_id) REFERENCES sast_finding(sast_finding_id) ON DELETE CASCADE
);

-- since 1.170
CREATE TABLE source_control_user (
  source_control_user_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  email varchar(255) NOT NULL,
  CONSTRAINT source_control_user_pk PRIMARY KEY (source_control_user_id),
  CONSTRAINT email_application_id_uk UNIQUE (application_id, email),
  CONSTRAINT source_control_user_application_fk FOREIGN KEY (application_id)
    REFERENCES application(application_id) ON DELETE CASCADE
);

-- since 1.170
CREATE TABLE source_control_user_activity (
  source_control_user_activity_id varchar(50) NOT NULL,
  source_control_user_id varchar(50) NOT NULL,
  commit_year_month timestamp NOT NULL,
  is_sent_to_telemetry boolean DEFAULT false NOT NULL,
  CONSTRAINT source_control_user_activity_pk PRIMARY KEY (source_control_user_activity_id),
  CONSTRAINT source_control_user_activity_fk FOREIGN KEY (source_control_user_id)
    REFERENCES source_control_user(source_control_user_id) ON DELETE CASCADE,
  CONSTRAINT userid_commit_id_uk UNIQUE (commit_year_month, source_control_user_id)
);

-- Since 1.172
CREATE TABLE call_flow_analysis_config (
   call_flow_analysis_config_id varchar(50) NOT NULL,
   enabled boolean NOT NULL,
   namespaces_json text NULL,
   algorithm varchar(255),
   thread_count int,
   owner_id varchar(50) NOT NULL,
   CONSTRAINT call_flow_analysis_config_id_pk PRIMARY KEY (call_flow_analysis_config_id),
   CONSTRAINT call_flow_analysis_config_owner_uk UNIQUE (owner_id)
);

-- since 1.172
-- SaaS Compatible
CREATE TABLE sast_pull_request_comment
(
  sast_pull_request_comment_id                varchar(50) NOT NULL,
  sast_scan_id                                varchar(50) NOT NULL,
  pull_request_url                            varchar(1000) NOT NULL,
  created_at                                  timestamp NOT NULL,
  last_updated_at                             timestamp NOT NULL,
  commit_hash                                 varchar(128) NOT NULL,
  content_hash                                varchar(128) NOT NULL,
  pull_request_comment_id                     varchar(50) NOT NULL,
  pull_request_comment_version                int DEFAULT 0 NOT NULL,

  CONSTRAINT sast_pull_request_comment_pk PRIMARY KEY (sast_pull_request_comment_id),
  CONSTRAINT sast_pull_request_comment_fk FOREIGN KEY (sast_scan_id) REFERENCES sast_scan(sast_scan_id) ON DELETE CASCADE,
  CONSTRAINT sast_pull_request_comment_sast_scan_id_uk UNIQUE (sast_scan_id),
  CONSTRAINT sast_pull_request_comment_pull_request_url_uk UNIQUE (pull_request_url)
);

CREATE INDEX sast_pull_request_comment_pull_request_url_idx ON sast_pull_request_comment (pull_request_url);
CREATE INDEX sast_pull_request_comment_sast_scan_id_idx ON sast_pull_request_comment (sast_scan_id);

-- since 1.176

-- Quick lookup of all ancestor organizations for a given organization. As this data is technically redundant,
-- the implementation of this table went through a number of iterations before settling on this simple approach.
--
-- Initially, it was a VIEW. This had two downsides: performance, and separate H2 and Postgres implementations (a
-- recursive query in Postgres, and a Java-based custom database function in H2).
--
-- Then, to improve performance, H2 was switched to a table kept up-to-date with TRIGGERs, and Postgres was switched
-- to a MATERIALIZED VIEW refreshed using TRIGGERs. However, it was found that there were race conditions in the
-- execution of the Postgres triggers when multiple organizations were being created concurrently.
--
-- To address the race condition, Postgres too was switched to a regular table kept up-to-date using TRIGGERs. At
-- that point however it was realized that things could be simplified a great deal if the database-specific triggers
-- were removed and the table kept up-to-date at the DAO level instead.
CREATE TABLE organization_ancestor (
  organization_ancestor_id VARCHAR(50) NOT NULL,
  organization_id VARCHAR(50) NOT NULL,
  ancestor_id VARCHAR(50) NOT NULL,
  ancestor_distance INTEGER NOT NULL,
  CONSTRAINT organization_ancestor_pk PRIMARY KEY (organization_ancestor_id),
  CONSTRAINT organization_ancestor_uk UNIQUE (organization_id, ancestor_id),
  CONSTRAINT organization_ancestor_ancestor_id_fk FOREIGN KEY (ancestor_id) REFERENCES organization (organization_id)
);

CREATE INDEX organization_ancestor_ancestor_id_idx ON organization_ancestor (ancestor_id);

-- As of the creation of this script, this table only exists for the purpose of the ancestor views defined below.
-- It was very tempting to instead define repository_container_ancestor using a hardcoded VALUES clause, but
-- that caused the postgres query planner to completely ignore all indexes when querying the owner_ancestor view.
-- To work efficiently, all of these views apparently need to ultimately query real tables.
CREATE TABLE repository_container (
  repository_container_id VARCHAR(50) NOT NULL,
  organization_id VARCHAR(50) NOT NULL,
  related_organization_id  VARCHAR(50) NULL,
  CONSTRAINT repository_container_pk PRIMARY KEY (repository_container_id),
  CONSTRAINT repository_container_related_organization_fk FOREIGN KEY (related_organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL
);

INSERT INTO repository_container VALUES ('REPOSITORY_CONTAINER_ID', 'ROOT_ORGANIZATION_ID', NULL);

-- The following five VIEWs build on organization_ancestor to allow querying of all ancestors of any type of owner
-- object.
CREATE VIEW application_ancestor (application_id, ancestor_id, ancestor_type, ancestor_distance) AS
SELECT
  application_id,
  application_id AS ancestor_id,
  'APPLICATION' AS ancestor_type,
  0 AS ancestor_distance
FROM
  application
UNION ALL
SELECT
  a.application_id,
  oa.ancestor_id,
  'ORGANIZATION' AS ancestor_type,
  oa.ancestor_distance + 1
FROM
  application a
  INNER JOIN organization_ancestor oa ON oa.organization_id = a.organization_id;

CREATE VIEW repository_container_ancestor (repository_container_id, ancestor_id, ancestor_type, ancestor_distance) AS
SELECT
  repository_container_id,
  repository_container_id AS ancestor_id,
  'REPOSITORY_CONTAINER' AS ancestor_type,
  0 AS ancestor_distance
FROM
  repository_container
UNION ALL
SELECT
  rc.repository_container_id,
  oa.ancestor_id,
  'ORGANIZATION' AS ancestor_type,
  oa.ancestor_distance + 1
FROM
  repository_container rc
  INNER JOIN organization_ancestor oa ON oa.organization_id = rc.organization_id;

CREATE VIEW repository_manager_ancestor (repository_manager_id, ancestor_id, ancestor_type, ancestor_distance) AS
SELECT
  repository_manager_id,
  repository_manager_id AS ancestor_id,
  'REPOSITORY_MANAGER' AS ancestor_type,
  0 AS ancestor_distance
FROM
  repository_manager
UNION ALL
SELECT
  rm.repository_manager_id,
  rca.ancestor_id,
  rca.ancestor_type,
  rca.ancestor_distance + 1
FROM
  repository_manager rm,
  repository_container_ancestor rca;

CREATE VIEW repository_ancestor (repository_id, ancestor_id, ancestor_type, ancestor_distance) AS
SELECT
  repository_id,
  repository_id AS ancestor_id,
  'REPOSITORY' AS ancestor_type,
  0 AS ancestor_distance
FROM
  repository
UNION ALL
SELECT
  r.repository_id,
  rma.ancestor_id,
  rma.ancestor_type,
  rma.ancestor_distance + 1
FROM
  repository r
  INNER JOIN repository_manager_ancestor rma ON rma.repository_manager_id = r.repository_manager_id;

CREATE VIEW owner_ancestor (owner_id, ancestor_id, ancestor_type, ancestor_distance, owner_type) AS
SELECT organization_id, ancestor_id, 'ORGANIZATION', ancestor_distance, 'ORGANIZATION' FROM organization_ancestor
UNION ALL
SELECT *, 'APPLICATION' FROM application_ancestor
UNION ALL
SELECT *, 'REPOSITORY_CONTAINER' FROM repository_container_ancestor
UNION ALL
SELECT *, 'REPOSITORY_MANAGER' FROM repository_manager_ancestor
UNION ALL
SELECT *, 'REPOSITORY' FROM repository_ancestor;

-- To help with performance of the above views
CREATE INDEX application_organization_idx ON application (organization_id);
CREATE INDEX repository_repository_manager_idx ON repository (repository_manager_id);

-- Note: for upgrades this is done by OrganizationAncestorInitialPopulationMigrator, and the record mapping
-- the root org to itself will have a random UUID rather than ROOT_ORGANIZATION_ANCESTOR_ID
INSERT INTO organization_ancestor (organization_ancestor_id, organization_id, ancestor_id, ancestor_distance)
VALUES ('ROOT_ORGANIZATION_ANCESTOR_ID', 'ROOT_ORGANIZATION_ID', 'ROOT_ORGANIZATION_ID', 0);


-- Since 1.177
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS oauth2_configuration
(
  idp_issuer               varchar(255) NOT NULL,
  idp_jwks_url             varchar(255),
  idp_jws_algorithm        varchar(255) NOT NULL,
  idp_jwks                 text,
  username_claim           varchar(255),
  first_name_claim         varchar(255),
  last_name_claim          varchar(255),
  email_claim              varchar(255),
  groups_claim             varchar(255),
  exact_match_claims_json  text,
  CONSTRAINT oauth_configuration_pk PRIMARY KEY (idp_issuer)
);

-- Since 1.177
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS oidc_configuration
(
  idp_issuer                        varchar(255) NOT NULL,
  client_id                         varchar(255) NOT NULL,
  client_secret                     varchar(255) NOT NULL,
  idp_authorization_url             varchar(255) NOT NULL,
  idp_token_url                     varchar(255) NOT NULL,
  authorization_custom_params_json  text,
  token_request_custom_params_json  text,
  CONSTRAINT oidc_configuration_pk PRIMARY KEY (idp_issuer)
);

-- since 1.177
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS oauth2_user
(
  oauth2_user_id varchar(255) NOT NULL,
  username varchar(255) NOT NULL,
  first_name varchar(255) NULL,
  last_name varchar(255) NULL,
  email varchar(255) NULL,
  groups_json text NULL,
  CONSTRAINT oauth2_user_pk PRIMARY KEY (oauth2_user_id),
  CONSTRAINT oauth2_user_username_uk UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS oauth2_group
(
  oauth2_group_id varchar(255) NOT NULL,
  name text NOT NULL,
  CONSTRAINT oauth2_group_pk PRIMARY KEY (oauth2_group_id)
);

CREATE TABLE IF NOT EXISTS oauth2_user_group
(
  oauth2_user_group_id varchar(255) NOT NULL,
  oauth2_user_id varchar(255) NOT NULL,
  oauth2_group_id varchar(255) NOT NULL,
  CONSTRAINT oauth2_user_group_pk PRIMARY KEY (oauth2_user_group_id),
  CONSTRAINT oauth2_user_group_uk UNIQUE (oauth2_user_id, oauth2_group_id),
  CONSTRAINT oauth2_user_group_user_fk FOREIGN KEY (oauth2_user_id) REFERENCES oauth2_user(oauth2_user_id),
  CONSTRAINT oauth2_user_group_group_fk FOREIGN KEY (oauth2_group_id) REFERENCES oauth2_group(oauth2_group_id)
);

-- since 1.178
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS development_prioritization (
  development_prioritization_id varchar(50) NOT NULL,
  scan_id varchar(50) NOT NULL,
  created_at timestamp DEFAULT now() NOT NULL,
  updated_at timestamp DEFAULT now() NOT NULL,
  CONSTRAINT development_prioritization_pk PRIMARY KEY (development_prioritization_id),
  CONSTRAINT development_prioritization_scan_id_uk UNIQUE (scan_id)
);

CREATE TABLE IF NOT EXISTS development_prioritization_component_info (
  development_prioritization_component_info_id varchar(50) NOT NULL,
  development_prioritization_id varchar(50) NOT NULL,
  scan_id varchar(50) NOT NULL,
  component_hash varchar(20) NOT NULL,
  remediation_type varchar(50), -- maps to enum ApiVersionChangeOptionType
  remediation_version varchar(100),
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  source_status varchar(20) NULL,
  build_status varchar(20) NULL,
  stage_release_status varchar(20) NULL,
  release_status varchar(20) NULL,
  CONSTRAINT development_prioritization_component_info_pk PRIMARY KEY (development_prioritization_component_info_id),
  CONSTRAINT development_prioritization_component_scan_id_hash_uk UNIQUE (scan_id, component_hash),
  CONSTRAINT development_prioritization_component_info_parent_fk FOREIGN KEY (development_prioritization_id) REFERENCES development_prioritization(development_prioritization_id)
);

CREATE INDEX development_prioritization_component_scan_id_hash_idx ON development_prioritization_component_info(scan_id, component_hash);

CREATE TABLE IF NOT EXISTS policy_waiver_reason (
  waiver_reason_id varchar(50) NOT NULL,
  type varchar(50) NOT NULL DEFAULT 'system',
  reason_text varchar(60) NOT NULL,
  sort_order INTEGER NULL,
  CONSTRAINT policy_waiver_reason_pk PRIMARY KEY (waiver_reason_id),
  CONSTRAINT policy_waiver_reason_uk UNIQUE (reason_text)
);
INSERT INTO policy_waiver_reason VALUES ('9b704ef5bc064fc29d7fe08a251ee9a6', 'system', 'Acknowledged violation', 10);
INSERT INTO policy_waiver_reason VALUES ('ab704ef5bc064fc29d7fe08a251ee9aa', 'system', 'Evaluating component', 15);
INSERT INTO policy_waiver_reason VALUES ('42069f58114f4df8b435a40a415d2835', 'system', 'Mitigated externally', 20);
INSERT INTO policy_waiver_reason VALUES ('39984de3d6e64f508df82b4cbfd72f70', 'system', 'No upgrade path', 30);
INSERT INTO policy_waiver_reason VALUES ('f6990a32cd8d4ea78853ca829d948927', 'system', 'Not exploitable', 40);
INSERT INTO policy_waiver_reason VALUES ('19bbf1a7d591497698ab3172461d971a', 'system', 'Not reachable', 50);
INSERT INTO policy_waiver_reason VALUES ('3446e70e60e04676a90131f3dea9bdb5', 'system', 'Researching', 60);
INSERT INTO policy_waiver_reason VALUES ('c991ef95866d4903ad0c6c217ac47c07', 'system', 'Other', 70);

-- since 1.183
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS  auto_policy_waiver (
    auto_policy_waiver_id VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50) NOT NULL,
    threat_level SMALLINT NOT NULL,
    reachable BOOLEAN,
    path_forward BOOLEAN,
    creator_id VARCHAR(60) NOT NULL,
    creator_name varchar(210) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    scopes_operator_any BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT auto_policy_waiver_pk
    PRIMARY KEY (auto_policy_waiver_id)
    );

CREATE INDEX auto_policy_waiver_owner_id_idx on auto_policy_waiver(owner_id);

CREATE TABLE IF NOT EXISTS auto_policy_waiver_revocation (
    auto_policy_waiver_revocation_id VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50) NOT NULL,
    creator_id VARCHAR(60) NOT NULL,
    creator_name VARCHAR(210) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    auto_policy_waiver_id VARCHAR(50) NOT NULL,
    hash varchar(20) NULL,
    associated_package_url VARCHAR(1000) NULL,
    scan_id varchar(50) NOT NULL,
    component_match_strategy varchar(30) NULL,
    policy_violation_id VARCHAR(50) NULL,
    threat_level SMALLINT NULL,
    policy_name VARCHAR(60) NULL,
    component_display_name VARCHAR(200) NULL,
    vulnerability_identifiers VARCHAR(200) NULL,
    policy_id VARCHAR(50) NULL,
    constraint_facts_json text NULL,
    CONSTRAINT auto_policy_waiver_revocation_pk
    PRIMARY KEY (auto_policy_waiver_revocation_id),
    CONSTRAINT auto_policy_waiver_revocation_auto_waiver_fk
    FOREIGN KEY (auto_policy_waiver_id) REFERENCES auto_policy_waiver(auto_policy_waiver_id)
);

CREATE INDEX auto_policy_waiver_revocation_owner_id_idx on auto_policy_waiver_revocation(owner_id);

-- since 1.184
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS  malware_defense_metrics (
    malware_defense_metrics_id VARCHAR(50) NOT NULL,
    format VARCHAR(50) NOT NULL,
    malicious_component_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT malware_defense_metrics_pk PRIMARY KEY (malware_defense_metrics_id),
    CONSTRAINT malware_defense_metrics_uk UNIQUE (format)
    );

CREATE TABLE IF NOT EXISTS scm_user_mappings (
  scm_user_mappings_id VARCHAR(50) NOT NULL,
  role_id VARCHAR(50) NULL,
  organization_id VARCHAR(50) NOT NULL,
  mappings_json TEXT NOT NULL,
  CONSTRAINT scm_user_mappings_pk PRIMARY KEY (scm_user_mappings_id),
  CONSTRAINT scm_user_mappings_organization_uk UNIQUE (organization_id)
);

-- since 1.187
CREATE TABLE IF NOT EXISTS historical_telemetry_state (
  historical_telemetry_state_id VARCHAR(50) NOT NULL,

  batch_size INTEGER NOT NULL DEFAULT 1000,
  min_free_memory_mb INTEGER NOT NULL DEFAULT 10,
  cutoff_date DATE NOT NULL,

  created TIMESTAMP NOT NULL,
  start_time TIMESTAMP NULL,
  last_updated TIMESTAMP NULL,

  -- these 'last_record' fields are used to track the last record processed by the telemetry job
  last_record_time TIMESTAMP NULL,
  last_record_key VARCHAR(50) NULL,

  status VARCHAR(15) NOT NULL,

  -- since 1.203, EI-440: retry tracking
  retry_count INTEGER NOT NULL DEFAULT 0,
  last_retry_time TIMESTAMP NULL,

  CONSTRAINT historical_telemetry_state_pk PRIMARY KEY (historical_telemetry_state_id)
);

-- since 1.189
CREATE TABLE IF NOT EXISTS component_change_detection_configuration (
  component_change_detection_configuration_id VARCHAR(50) NOT NULL,
  version VARCHAR(50) NOT NULL,
  purl VARCHAR(1000) NOT NULL,
  component_hash VARCHAR(50),
  comparison_hash VARCHAR(50),
  added_time TIMESTAMP NOT NULL,
  CONSTRAINT component_change_detection_configuration_pk PRIMARY KEY (component_change_detection_configuration_id),
  CONSTRAINT component_change_detection_configuration_uk UNIQUE (purl)
);

CREATE INDEX component_change_detection_configuration_purl_idx ON component_change_detection_configuration(purl);
CREATE INDEX component_change_detection_configuration_added_time_idx ON component_change_detection_configuration(added_time);

-- since 1.189
CREATE TABLE IF NOT EXISTS component_change_detection_event (
  component_change_detection_event_id VARCHAR(50) NOT NULL,
  purl VARCHAR(1000) NOT NULL,
  component_evaluation_data TEXT NOT NULL,
  added_time TIMESTAMP NOT NULL,
  CONSTRAINT component_change_detection_event_pk PRIMARY KEY (component_change_detection_event_id)
);

CREATE INDEX component_change_detection_event_purl_idx ON component_change_detection_event(purl);
CREATE INDEX component_change_detection_event_added_time_idx ON component_change_detection_event(added_time);

-- since 1.190
CREATE TABLE IF NOT EXISTS cluster_identification (
  cluster_identification_id VARCHAR(50) NOT NULL,

  assigned_cluster_id VARCHAR(128),
  assigned_telemetry_id VARCHAR(15) NOT NULL,
  tamper_code VARCHAR(50) NOT NULL,

  base_url_hash VARCHAR(50) NOT NULL,
  last_calculated_cluster_id VARCHAR(128),

  created TIMESTAMP NOT NULL,
  last_updated TIMESTAMP,

  CONSTRAINT cluster_identification_pk PRIMARY KEY (cluster_identification_id)
  );

-- since 1.191
CREATE TABLE cpe_matching_configuration (
  cpe_matching_configuration_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  cpe_enabled boolean,
  allow_override boolean,
  CONSTRAINT cpe_matching_configuration_pk PRIMARY KEY (cpe_matching_configuration_id),
  CONSTRAINT cpe_matching_configuration_owner_id_uk UNIQUE (owner_id)
);
CREATE INDEX cpe_matching_configuration_owner_id_idx ON cpe_matching_configuration(owner_id);

-- Since 1.191
CREATE TABLE zscaler_configuration (
                                    zscaler_configuration_id varchar(50) NOT NULL,
                                    hostname varchar(255) NOT NULL,
                                    apikey varchar(255) NOT NULL,
                                    username varchar(255),
                                    password varchar(255),
                                    CONSTRAINT zscaler_configuration_pk PRIMARY KEY (zscaler_configuration_id)
);

-- Since 1.192
CREATE TABLE zscaler_metrics (
    zscaler_metrics_id varchar(50) NOT NULL,

    maven_urls_from_hds INTEGER,
    npm_urls_from_hds INTEGER,
    pypi_urls_from_hds INTEGER,
    nuget_urls_from_hds INTEGER,

    maven_urls_to_zscaler INTEGER,
    npm_urls_to_zscaler INTEGER,
    pypi_urls_to_zscaler INTEGER,
    nuget_urls_to_zscaler INTEGER,

    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT zscaler_metrics_pk PRIMARY KEY (zscaler_metrics_id)
);

CREATE TABLE zscaler_format (
    zscaler_format_id varchar(50) NOT NULL,
    zscaler_configuration_id varchar(50) NOT NULL,
    format VARCHAR(50) NOT NULL,
    enabled bool NOT NULL,
    CONSTRAINT zscaler_format_pk PRIMARY KEY (zscaler_format_id),
    CONSTRAINT zscaler_format_uk UNIQUE (format),
    CONSTRAINT zscaler_format_configuration_fk FOREIGN KEY (zscaler_configuration_id)
        REFERENCES zscaler_configuration(zscaler_configuration_id) ON DELETE CASCADE

);

-- Since 1.193
INSERT INTO cpe_matching_configuration (cpe_matching_configuration_id, owner_id, cpe_enabled, allow_override) VALUES ('de05215c34f944b7b77ca85f6e096797', 'ROOT_ORGANIZATION_ID', true, true);

-- Since 1.197
CREATE TABLE enterprise_reporting_filter (
  enterprise_reporting_filter_id varchar(50) NOT NULL,
  filter_name varchar(60) NOT NULL,
  filter_json text NOT NULL,
  user_id varchar(255) NOT NULL,
  CONSTRAINT enterprise_reporting_filter_uk UNIQUE (filter_name, user_id),
  CONSTRAINT enterprise_reporting_filter_pk PRIMARY KEY (enterprise_reporting_filter_id)
);

CREATE TABLE enterprise_reporting_default_filter (
  enterprise_reporting_filter_id varchar(50) NOT NULL,
  user_id varchar(255) NOT NULL,
  CONSTRAINT enterprise_reporting_default_filter_user_pk PRIMARY KEY (user_id),
  CONSTRAINT enterprise_reporting_default_filter_id_fk FOREIGN KEY (enterprise_reporting_filter_id)
      REFERENCES enterprise_reporting_filter(enterprise_reporting_filter_id) ON DELETE CASCADE
);

-- since 1.201
CREATE TABLE ci_integrations_config (
  ci_integrations_config_id VARCHAR(50) NOT NULL,
  configuration_json TEXT,
  owner_id VARCHAR(50) NOT NULL,
  owner_type VARCHAR(20) NOT NULL,
  create_time TIMESTAMP NOT NULL,
  update_time TIMESTAMP NOT NULL,
  CONSTRAINT ci_integrations_config_pk PRIMARY KEY (ci_integrations_config_id),
  CONSTRAINT ci_integrations_config_owner_uk UNIQUE (owner_id, owner_type)
);

CREATE TABLE version_evaluation_window (
  version_evaluation_window_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  context_id varchar(30) NOT NULL,
  max_versions smallint,
  max_age_in_days smallint,
  CONSTRAINT version_evaluation_window_pk PRIMARY KEY (version_evaluation_window_id),
  CONSTRAINT version_evaluation_window_uk UNIQUE (owner_id, context_id)
);

CREATE TABLE key_value (
  key varchar(50) NOT NULL,
  value varchar(4000) NOT NULL,
  CONSTRAINT key_value_pk PRIMARY KEY (key)
);

CREATE TABLE evaluation_queue (
  create_time timestamp NOT NULL,
  update_time timestamp NOT NULL,
  priority int NOT NULL,
  evaluation_queue_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  version varchar(1100) NOT NULL,
  worker_id varchar(50),
  CONSTRAINT evaluation_queue_pk PRIMARY KEY (evaluation_queue_id),
  CONSTRAINT evaluation_queue_uk UNIQUE (application_id, stage_type_id, version),
  CONSTRAINT evaluation_queue_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE
);
CREATE INDEX evaluation_queue_priority_idx ON evaluation_queue(priority);

CREATE TABLE scan_health_config (
  scan_health_config_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  owner_type varchar(20) NOT NULL,
  configuration_json TEXT NOT NULL,
  create_time timestamp NOT NULL,
  update_time timestamp NOT NULL,
  CONSTRAINT scan_health_config_pk PRIMARY KEY (scan_health_config_id),
  CONSTRAINT scan_health_config_owner_uk UNIQUE (owner_id, owner_type)
);
CREATE INDEX scan_health_config_owner_idx ON scan_health_config(owner_id);

CREATE TABLE hosted_component_scan_queue (
  id varchar(255) NOT NULL,
  component_id varchar(255) NOT NULL,
  scan_file_id varchar(255) NOT NULL,
  status varchar(50) NOT NULL,
  priority integer NOT NULL DEFAULT 5,
  acquired_at timestamp,
  error_message varchar(2000),
  repository_id varchar(255) NOT NULL,
  retry_count integer NOT NULL DEFAULT 0,
  purl varchar(2000),
  policy_evaluation_stage varchar(50),
  CONSTRAINT hosted_component_scan_queue_pk PRIMARY KEY (id)
);
CREATE INDEX hosted_component_scan_queue_component_id_idx ON hosted_component_scan_queue(component_id);
CREATE INDEX hosted_component_scan_queue_status_idx ON hosted_component_scan_queue(status);
CREATE INDEX hosted_component_scan_queue_priority_idx ON hosted_component_scan_queue(priority);
CREATE INDEX hosted_component_scan_queue_status_priority_idx ON hosted_component_scan_queue(status, priority DESC);
CREATE INDEX hosted_component_scan_queue_acquired_at_idx ON hosted_component_scan_queue(acquired_at);
CREATE INDEX hosted_component_scan_queue_repository_id_idx ON hosted_component_scan_queue(repository_id);
