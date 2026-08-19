-- excerpt of schema version 114 with the tables relevant for the migrator

CREATE SCHEMA insight_brain_ods;
SET SCHEMA insight_brain_ods;

CREATE TABLE organization (
  organization_id varchar(50) NOT NULL,
  parent_organization_id varchar(50) NULL,
  name varchar(100) NOT NULL,
  name_lowercase_no_whitespace varchar(100) NOT NULL,
  CONSTRAINT organization_pk PRIMARY KEY (organization_id),
  CONSTRAINT organization_name_uk UNIQUE KEY (name_lowercase_no_whitespace),
  CONSTRAINT organization_parent_organization_fk FOREIGN KEY (parent_organization_id) REFERENCES organization(organization_id)
);
INSERT INTO organization (organization_id, parent_organization_id, name, name_lowercase_no_whitespace) VALUES('ROOT_ORGANIZATION_ID', null, 'Root Organization', 'rootorganization');

CREATE TABLE application (
  application_id varchar(50) NOT NULL,
  public_id varchar(200) NOT NULL,
  public_id_lowercase varchar(200) NOT NULL,
  name varchar(100) NOT NULL,
  name_lowercase_no_whitespace varchar(100) NOT NULL,
  organization_id varchar(50) NOT NULL,
  contact_internal_name varchar(60) NULL,
  CONSTRAINT application_pk PRIMARY KEY (application_id),
  CONSTRAINT application_uk UNIQUE KEY (public_id_lowercase),
  CONSTRAINT application_name_uk UNIQUE KEY (name_lowercase_no_whitespace),
  CONSTRAINT application_organization_fk FOREIGN KEY (organization_id) REFERENCES organization(organization_id)
);

CREATE TABLE policy_evaluation (
  policy_evaluation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  scan_id varchar(50) NOT NULL,
  reevaluation bool DEFAULT false NOT NULL,
  for_monitoring bool DEFAULT false NOT NULL,
  for_obsolete_scan bool DEFAULT false NOT NULL,
  time datetime NOT NULL,
  CONSTRAINT policy_evaluation_pk PRIMARY KEY (policy_evaluation_id),
  CONSTRAINT policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);
CREATE INDEX policy_evaluation_scan_id_idx ON policy_evaluation(scan_id);
CREATE INDEX policy_evaluation_time_idx ON policy_evaluation(time);

CREATE TABLE policy_violation (
  policy_violation_id varchar(50) NOT NULL,
  policy_evaluation_id varchar(50) NOT NULL,
  time datetime NOT NULL,
  policy_id varchar(50) NOT NULL,
  policy_name varchar(60) NOT NULL,
  threat_level smallint(2) NOT NULL,
  threat_category varchar(20) NOT NULL,
  hash varchar(20),
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000),
  constraint_facts_json CLOB NOT NULL,
  pathnames CLOB,
  action_type_id varchar(20),
  notifications CLOB,
  waived bool DEFAULT false NOT NULL,
  CONSTRAINT policy_violation_pk PRIMARY KEY (policy_violation_id),
  CONSTRAINT policy_violation_evaluation_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id)
);
CREATE INDEX policy_violation_time_idx ON policy_violation(time);
CREATE INDEX policy_violation_policy_id_idx ON policy_violation(policy_id);
CREATE INDEX policy_violation_hash_idx ON policy_violation(hash);

CREATE TABLE first_occurrence_policy_violation (
  policy_violation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  CONSTRAINT first_occurrence_policy_violation_pk PRIMARY KEY (policy_violation_id),
  CONSTRAINT first_occurrence_violation_violation_fk FOREIGN KEY (policy_violation_id) REFERENCES policy_violation(policy_violation_id),
  CONSTRAINT first_occurrence_violation_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);

CREATE TABLE waived_policy_violation (
  policy_violation_id varchar(50) NOT NULL,
  policy_waiver_id varchar(50) NOT NULL,
  comment varchar(1000) NULL,
  CONSTRAINT waived_policy_violation_pk PRIMARY KEY (policy_violation_id),
  CONSTRAINT waived_policy_violation_violation_fk FOREIGN KEY (policy_violation_id) REFERENCES policy_violation(policy_violation_id)
);
