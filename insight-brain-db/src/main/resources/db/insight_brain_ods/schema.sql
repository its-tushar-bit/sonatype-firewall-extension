-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE organization (
  organization_id varchar(50) NOT NULL,
  parent_organization_id varchar(50) NULL,
  name varchar(200) NOT NULL,
  name_lowercase_no_whitespace varchar(200) NOT NULL,
  policy_violation_grandfathering_enabled boolean,
  allow_policy_violation_grandfathering_override boolean DEFAULT true NOT NULL, -- Whether policy violation grandfathering can be overridden by children (orgs and apps).
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
  policy_violation_grandfathering_enabled boolean,
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
  policy_violation_grandfathering_allowed boolean NOT NULL,
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
  comment varchar(1000) NULL,
  create_time timestamp NOT NULL,
  expiry_time timestamp default NULL,
  CONSTRAINT policy_waiver_pk PRIMARY KEY (policy_waiver_id),
  CONSTRAINT policy_waiver_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id)
);
CREATE INDEX policy_waiver_owner_id_idx ON policy_waiver(owner_id);

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
-- Developer role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1a6a3ba60818476781a6a2cb9adcb7f6', '1da70fae1fd54d6cb7999871ebdb9a36', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('61159b674eb94cdcb00ebdec77a47373', '1da70fae1fd54d6cb7999871ebdb9a36', 'EVALUATE_COMPONENT');
-- Application Evaluator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('ea7bb57e93e241acbf8da5ebcb5b0074', '2cb71b3468d649789163ea2e212b541e', 'EVALUATE_APPLICATION');
-- Component Evaluator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('f198535bdf2549d38417534e38ae3cda', '90c7c98683b4471cb77a916744540bcc', 'EVALUATE_COMPONENT');
-- Legal Reviewer role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('6c521879906a4fdf8d27c652787243b4', '0df46317c031440795007f4ce9c7f002', 'LEGAL_REVIEWER');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('9c3d3a466bed410fa8d8c8801f3a0c13', '0df46317c031440795007f4ce9c7f002', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('4209ad3cdcfd474b865c51c0d664ea2a', '0df46317c031440795007f4ce9c7f002', 'READ');

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
  CONSTRAINT policy_monitoring_uk UNIQUE (owner_id)
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
  CONSTRAINT application_tag_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
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
  CONSTRAINT policy_evaluation_pk PRIMARY KEY (policy_evaluation_id),
  CONSTRAINT policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);
CREATE INDEX policy_evaluation_scan_id_idx ON policy_evaluation(scan_id);
CREATE INDEX policy_evaluation_time_idx ON policy_evaluation(time);
CREATE INDEX policy_evaluation_app_monitoring_stage_idx ON policy_evaluation(application_id, for_monitoring, stage_type_id);
CREATE INDEX policy_evaluation_commit_hash_idx ON policy_evaluation(commit_hash);

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
  constraint_facts_json text NOT NULL,

  -- the most recent action during the violation's lifetime
  action_type_id varchar(20),

  -- timestamps recording the state and transitions thereof for the violation
  open_time timestamp NOT NULL,    -- when the violation first occurred
  waive_time timestamp NULL,       -- when the violation was waived
  grandfather_time timestamp NULL, -- when the violation was grandfathered
  fix_time timestamp NULL,         -- when the violation disappeared entirely

  -- details of the waiver that suppressed this violation
  policy_waiver_id varchar(50) NULL,  -- no foreign key constraint to policy_waiver, waivers can be deleted at any time
  policy_waiver_comment varchar(1000) NULL,

  -- whether the violation was ever encountered during a non-re-evaluation, supports notifications for ordinary evaluations
  seen_by_primary_evaluation bool NOT NULL,
  -- whether the violation was ever encountered during policy monitoring, supports separate notifications for policy monitoring
  seen_by_monitoring_evaluation bool NOT NULL,

  CONSTRAINT policy_violation_pk PRIMARY KEY (policy_violation_id),
  CONSTRAINT policy_violation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);
CREATE INDEX policy_violation_app_fix_time_stage_idx ON policy_violation(application_id, fix_time, stage_type_id);
CREATE INDEX policy_violation_policy_app_idx ON policy_violation(policy_id, application_id);
CREATE INDEX policy_violation_hash_idx ON policy_violation(hash);

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
  CONSTRAINT application_component_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
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
  CONSTRAINT aggregate_file_application_component_fk FOREIGN KEY (application_component_id) REFERENCES application_component(application_component_id),
  CONSTRAINT aggregate_file_uk UNIQUE (application_component_id, hash)
);
CREATE INDEX aggregate_file_application_component_id_idx ON aggregate_file(application_component_id);

CREATE TABLE last_policy_evaluation (
  policy_evaluation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  CONSTRAINT last_policy_evaluation_PK PRIMARY KEY (policy_evaluation_id),
  CONSTRAINT last_policy_evaluation_uk UNIQUE (application_id, stage_type_id),
  CONSTRAINT last_policy_evaluation_eval_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id),
  CONSTRAINT last_policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
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
  CONSTRAINT repository_manager_pk PRIMARY KEY (repository_manager_id),
  CONSTRAINT repository_manager_uk UNIQUE (instance_id)
);

CREATE TABLE repository (
  repository_id varchar(50) NOT NULL,
  repository_manager_id varchar(50) NOT NULL,
  public_id varchar(500) NOT NULL,
  enabled bool DEFAULT true NOT NULL,
  quarantine_enabled bool DEFAULT false NOT NULL,
  format varchar(50),
  CONSTRAINT repository_pk PRIMARY KEY (repository_id),
  CONSTRAINT repository_uk UNIQUE (repository_manager_id, public_id),
  CONSTRAINT repository_repository_manager_fk FOREIGN KEY (repository_manager_id) REFERENCES repository_manager(repository_manager_id)
);

CREATE TABLE repository_component (
  repository_component_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  pathname varchar(1000) NOT NULL,
  time timestamp NOT NULL,
  hash varchar(20) NOT NULL,
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000), -- the component identifier coordinates stored in json format
  match_state_id varchar(20) NOT NULL,
  identification_source_id varchar(20) NOT NULL,
  last_evaluation_time timestamp NOT NULL,
  quarantine_time timestamp,
  unquarantine_time timestamp,
  analyzer_features_json varchar(1000), -- the analyzer features stored in json format
  auto_unquarantined boolean,
  CONSTRAINT repository_component_pk PRIMARY KEY (repository_component_id),
  CONSTRAINT repository_component_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  CONSTRAINT repository_component_uk UNIQUE (repository_id, pathname)
);
CREATE INDEX repository_component_hash_idx ON repository_component(hash);
CREATE INDEX repository_component_repository_unquarantine_idx ON repository_component(repository_id, unquarantine_time);
CREATE INDEX repository_component_quarantine_idx ON repository_component(repository_id, quarantine_time);
CREATE INDEX repository_component_release_quarantine_idx ON repository_component (quarantine_time, unquarantine_time, auto_unquarantined);


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
  constraint_facts_json text NOT NULL, -- the constraint facts (that caused the policy violation) stored in json format
  action_type_id varchar(20),
  waived bool DEFAULT false NOT NULL,
  active bool DEFAULT true NOT NULL, -- Whether this violation is still active. If false, then the component was removed from the repository or a more recent evaluation was performed for this component.
  policy_waiver_id varchar(50) NULL,        -- no foreign key constraint to policy_waiver, waivers can be deleted at any time
  policy_waiver_comment varchar(1000) NULL,
  waive_time timestamp NULL,                -- when the violation was waived

  CONSTRAINT repository_policy_violation_pk PRIMARY KEY (repository_policy_violation_id),
  CONSTRAINT repository_policy_violation_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id)
);
CREATE INDEX repository_policy_violation_pathname_idx ON repository_policy_violation(pathname);

CREATE TABLE proprietary_component_name_pattern (
  proprietary_component_name_pattern_id varchar(50) NOT NULL,

  format varchar(50) NOT NULL,
  namespace_pattern varchar(200) NOT NULL,
  name_pattern varchar(300) NOT NULL,

  repository_manager_instance_id varchar(50) NOT NULL,
  repository_public_id varchar(500) NOT NULL,

  CONSTRAINT proprietary_component_name_pattern_pk PRIMARY KEY (proprietary_component_name_pattern_id),
  -- to make this uniqueness constraint work as desired, note that all included columns are not nullable, using empty strings instead if needed
  CONSTRAINT proprietary_component_name_pattern_uk UNIQUE (format, namespace_pattern, name_pattern, repository_manager_instance_id, repository_public_id)
);
CREATE INDEX proprietary_component_name_pattern_repo_idx ON proprietary_component_name_pattern(repository_manager_instance_id, repository_public_id);

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
  value varchar(500) NOT NULL,
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

-- source control repository data associated with an organization or application (owner)
CREATE TABLE source_control (
  source_control_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  repository_url varchar(2048),
  username varchar(256),
  token varchar(512),
  provider varchar(20),
  base_branch varchar(243),
  enable_pull_requests boolean,
  enable_status_checks boolean,
  pull_request_poll_time timestamp NULL,
  pull_request_error_count INT NOT NULL DEFAULT 0,
  CONSTRAINT source_control_pk PRIMARY KEY (source_control_id),
  CONSTRAINT source_control_owner_id_uk UNIQUE (owner_id)
);

CREATE TABLE schema_version (
  schema_version int NOT NULL
);
INSERT INTO schema_version (schema_version) VALUES (-1);

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

CREATE TABLE search_index_change (
  search_index_change_id varchar(50) NOT NULL,
  change_type varchar(100) NOT NULL,
  change_data varchar(2000) NOT NULL,
  CONSTRAINT search_index_change_pk PRIMARY KEY (search_index_change_id)
);

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
  pull_request_id int NOT NULL,
  pull_request_comment_id int NOT NULL,
  pull_request_comment_version int default NULL,
  source_policy_evaluation_id varchar(50) NOT NULL,
  target_policy_evaluation_id varchar(50) NOT NULL,
  create_time timestamp NOT NULL,
  update_time timestamp,
  content_hash varchar(40),
  CONSTRAINT source_control_pull_request_comment_pk PRIMARY KEY (source_control_pull_request_comment_id),
  CONSTRAINT source_control_pull_request_comment_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT source_control_pull_request_source_policy_eval_fk FOREIGN KEY (source_policy_evaluation_id)
      REFERENCES policy_evaluation(policy_evaluation_id),
  CONSTRAINT source_control_pull_request_target_policy_eval_fk FOREIGN KEY (target_policy_evaluation_id)
      REFERENCES policy_evaluation(policy_evaluation_id),
  CONSTRAINT source_control_pull_request_comment_uk UNIQUE (application_id, component_hash, pull_request_id)
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
  CONSTRAINT source_control_default_branch_commit_history_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id),
  CONSTRAINT source_control_default_branch_commit_history_policy_eval_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id),
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
  commit_hash varchar(128),
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
  CONSTRAINT source_control_event_pk PRIMARY KEY (source_control_event_id),
  CONSTRAINT source_control_event_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id),
  CONSTRAINT source_control_event_policy_evaluation_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation (policy_evaluation_id)
);
CREATE INDEX source_control_event_instance_id_idx ON source_control_event(instance_id);
CREATE INDEX source_control_event_create_time_idx ON source_control_event(create_time);
CREATE INDEX source_control_event_event_status_idx ON source_control_event(event_status);
CREATE INDEX source_control_event_application_id_idx ON source_control_event(application_id);

-- Since 1.96
CREATE TABLE product_license (
  product_license_id varchar(50) NOT NULL,
  license_key varchar(8192) NOT NULL,
  license_details varchar(8192),
  CONSTRAINT product_license_pk PRIMARY KEY (product_license_id)
);

-- Since 1.97
CREATE TABLE firewall_ignore_patterns (
  firewall_ignore_patterns_id varchar(50) NOT NULL,
  firewall_ignore_patterns_json text,
  CONSTRAINT firewall_ignore_patterns_pk PRIMARY KEY (firewall_ignore_patterns_id)
);
INSERT INTO firewall_ignore_patterns(firewall_ignore_patterns_id) VALUES ('firewall-ignore-patterns');

-- Since 1.97
CREATE TABLE lock (
  lock_id varchar(1100) NOT NULL,
  CONSTRAINT lock_pk PRIMARY KEY (lock_id)
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


CREATE TABLE inner_source_component (
  inner_source_component_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  package_url varchar(1000) NOT NULL,
  CONSTRAINT inner_source_component_pk PRIMARY KEY (inner_source_component_id),
  CONSTRAINT inner_source_component_uk UNIQUE (package_url),
  CONSTRAINT inner_source_component_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id)
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
  CONSTRAINT perpetual_lock_id_pk PRIMARY KEY (perpetual_lock_id)
);

-- Since 1.104
CREATE TABLE application_component_license (
  application_component_license_id varchar(50) NOT NULL,
  application_component_id varchar(50) NOT NULL,
  effective_license_id varchar(1000) NOT NULL,
  CONSTRAINT application_component_license_pk PRIMARY KEY (application_component_license_id),
  CONSTRAINT application_component_license_application_component_fk FOREIGN KEY (application_component_id) REFERENCES application_component(application_component_id),
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
