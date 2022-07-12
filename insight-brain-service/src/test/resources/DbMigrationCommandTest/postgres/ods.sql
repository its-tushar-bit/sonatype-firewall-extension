--
-- DO NOT CHANGE THIS FILE!
-- 
-- This is a dump of the database schema at the time PostgreSQL support was added.
-- Given this as baseline, new migration scripts must be able to upgrade the PostgreSQL database.
--

CREATE SCHEMA insight_brain_ods;
SET SCHEMA 'insight_brain_ods';

-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE schema_info (
  schema_info_id varchar(50) NOT NULL,
  drools_code_version int NOT NULL,
  policy_json_version int NOT NULL,
  CONSTRAINT schema_info_pk PRIMARY KEY (schema_info_id)
);
INSERT INTO schema_info (schema_info_id, drools_code_version, policy_json_version) VALUES ('1', 2, 1);

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
  CONSTRAINT policy_waiver_pk PRIMARY KEY (policy_waiver_id),
  CONSTRAINT policy_waiver_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id)
);

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
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('06af8c357eb04568bd73b246440ea063', 'b9646757e98e486da7d730025f5245f8', 'MANAGE_PROPRIETARY');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('6da52edbbc554b3ab4dd502e30a80acd', 'b9646757e98e486da7d730025f5245f8', 'EVALUATE_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('e07e0e487c074a4fa95a1abde2f68aed', 'b9646757e98e486da7d730025f5245f8', 'EVALUATE_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('00286ae0ccf5441391333f050c25170b', 'b9646757e98e486da7d730025f5245f8', 'VIEW_ROLES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('869815aecdc849a8ade21ffc5ccc41ea', 'b9646757e98e486da7d730025f5245f8', 'EDIT_ROLES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('f3fe74d34577471c936c332293c5ba0a', 'b9646757e98e486da7d730025f5245f8', 'CLAIM_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('f3fe74d34577471c936c332293c5ba0b', 'b9646757e98e486da7d730025f5245f8', 'ADD_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('043024fcd34c47a1bea907cc0c84955f', 'b9646757e98e486da7d730025f5245f8', 'MANAGE_AUTOMATIC_APPLICATION_CREATION');
-- Owner role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1c8d6f420845466bbc1eb5eaf6d4baa2', '1cddabf7fdaa47d6833454af10e0a3ef', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1c2587ba144341fd9d937dd36c850f5a', '1cddabf7fdaa47d6833454af10e0a3ef', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('fe3a0e01fa4543386d74aadf7fbaddc1', '1cddabf7fdaa47d6833454af10e0a3ef', 'MANAGE_PROPRIETARY');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('465f023ab44040deb2c5a3b7c3dd3c77', '1cddabf7fdaa47d6833454af10e0a3ef', 'EVALUATE_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('33fbb19ce93e4420a0ebcd846b0705d5', '1cddabf7fdaa47d6833454af10e0a3ef', 'EVALUATE_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1bbcd0fe101449f09c3682fe6837267e', '1cddabf7fdaa47d6833454af10e0a3ef', 'VIEW_ROLES');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1bbcd0fe101449f09c3682fe6837267f', '1cddabf7fdaa47d6833454af10e0a3ef', 'ADD_APPLICATION');
-- Developer role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1a6a3ba60818476781a6a2cb9adcb7f6', '1da70fae1fd54d6cb7999871ebdb9a36', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('61159b674eb94cdcb00ebdec77a47373', '1da70fae1fd54d6cb7999871ebdb9a36', 'EVALUATE_COMPONENT');
-- Application Evaluator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('ea7bb57e93e241acbf8da5ebcb5b0074', '2cb71b3468d649789163ea2e212b541e', 'EVALUATE_APPLICATION');
-- Component Evaluator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('f198535bdf2549d38417534e38ae3cda', '90c7c98683b4471cb77a916744540bcc', 'EVALUATE_COMPONENT');

CREATE TABLE membership_mapping (
  membership_mapping_id varchar(50) NOT NULL,
  context_id varchar(50) NOT NULL, -- either 'global' or id of org/app
  role_id varchar(50) NOT NULL,
  member_name varchar(60) NOT NULL,
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
  CONSTRAINT policy_evaluation_pk PRIMARY KEY (policy_evaluation_id),
  CONSTRAINT policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);
CREATE INDEX policy_evaluation_scan_id_idx ON policy_evaluation(scan_id);
CREATE INDEX policy_evaluation_time_idx ON policy_evaluation(time);
CREATE INDEX policy_evaluation_app_monitoring_stage_idx ON policy_evaluation(application_id, for_monitoring, stage_type_id);

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
  component_id_format varchar(10),
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
  username varchar(60) NOT NULL, -- The internal name of the User (CLM User or LDAP user)
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  based_on_filter_name varchar(60),
  acknowledged boolean DEFAULT false NOT NULL,
  filter_json text NOT NULL, -- The dashboard filter stored in json format
  CONSTRAINT dashboard_filter_pk PRIMARY KEY (dashboard_filter_id),
  CONSTRAINT dashboard_filter_uk UNIQUE (username, name_lowercase_no_whitespace)
);

CREATE TABLE application_component (
  application_component_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  time timestamp NOT NULL,
  hash varchar(20) NOT NULL,
  component_id_format varchar(10),
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
  username varchar(60) NOT NULL, -- The internal name of the User (CLM User or LDAP user)
  notification_id varchar(50) NOT NULL,
  CONSTRAINT notification_viewed_pk PRIMARY KEY (user_viewed_product_notification_id),
  CONSTRAINT notification_viewed_uk UNIQUE (notification_id, username)
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
  CONSTRAINT repository_component_pk PRIMARY KEY (repository_component_id),
  CONSTRAINT repository_component_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  CONSTRAINT repository_component_uk UNIQUE (repository_id, pathname)
);
CREATE INDEX repository_component_hash_idx ON repository_component(hash);
CREATE INDEX repository_component_repository_unquarantine_idx ON repository_component(repository_id, unquarantine_time);

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
  CONSTRAINT repository_policy_violation_pk PRIMARY KEY (repository_policy_violation_id),
  CONSTRAINT repository_policy_violation_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id)
);
CREATE INDEX repository_policy_violation_pathname_idx ON repository_policy_violation(pathname);

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
-- Add default value for no proxy hosts
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('2b00042f7481c7b056c4b410d28f33cf', 'PROXY_EXCLUDE_HOSTS', '');

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
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a1', 'ROOT_ORGANIZATION_ID', 'build', true, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a2', 'ROOT_ORGANIZATION_ID', 'stage-release', true, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a3', 'ROOT_ORGANIZATION_ID', 'release', true, 3650);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a4', 'ROOT_ORGANIZATION_ID', 'operate', true, 3650);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a5', 'ROOT_ORGANIZATION_ID', 'continuous-monitoring', true, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a6', 'ROOT_ORGANIZATION_ID', 'success-metrics', true, 365);

CREATE TABLE schema_version (
  schema_version int NOT NULL
);
INSERT INTO schema_version (schema_version) VALUES (130);
