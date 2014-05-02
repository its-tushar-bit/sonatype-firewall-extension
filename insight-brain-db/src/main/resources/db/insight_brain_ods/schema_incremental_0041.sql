-- Since 1.11
SET SCHEMA insight_brain_ods;

CREATE TABLE policy_evaluation (
  policy_evaluation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  scan_id varchar(50) NOT NULL,
  reevaluation bool DEFAULT false NOT NULL,
  for_monitoring bool DEFAULT false NOT NULL,
  time datetime NOT NULL,
  CONSTRAINT policy_evaluation_pk PRIMARY KEY (policy_evaluation_id),
  CONSTRAINT policy_evaluation_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);
CREATE INDEX policy_evaluation_scan_id_idx ON policy_evaluation(scan_id);
CREATE INDEX policy_evaluation_time_idx ON policy_evaluation(time);

CREATE TABLE policy_violation (
  policy_violation_id varchar(50) NOT NULL,
  policy_evaluation_id varchar(50) NOT NULL,
  policy_id varchar(50) NOT NULL,
  threat_level smallint(2) NOT NULL,
  threat_category varchar(20) NOT NULL,
  hash varchar(20),
  group_id varchar(100),
  artifact_id varchar(100),
  version varchar(100),
  constraint_facts_json CLOB NOT NULL, -- the constraint facts (that caused the policy violation) stored in json format
  CONSTRAINT policy_violation_pk PRIMARY KEY (policy_violation_id),
  CONSTRAINT policy_violation_evaluation_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id),
  CONSTRAINT policy_violation_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id)
);
