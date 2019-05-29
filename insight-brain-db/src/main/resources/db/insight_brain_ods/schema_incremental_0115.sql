-- Since 1.45.0
ALTER TABLE policy_violation DROP CONSTRAINT policy_violation_pk;

CREATE TABLE new_policy_violation (
  policy_violation_id varchar(50) NOT NULL,

  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,

  -- summary of the policy that caused the violation
  policy_id varchar(50) NOT NULL, -- no foreign key constraint to policy, policies can be deleted at any time
  policy_name varchar(60) NOT NULL,
  threat_level smallint(2) NOT NULL,
  threat_category varchar(20) NOT NULL,

  -- identification of the component that caused the violation
  hash varchar(20),
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000),
  filename varchar(1000),

  -- record of the most recent policy constraints/conditions that were violated
  constraint_facts_json CLOB NOT NULL,

  -- the most recent action during the violation's lifetime
  action_type_id varchar(20),

  -- timestamps recording the state and transitions thereof for the violation
  open_time datetime NOT NULL, -- when the violation first occurred
  waive_time datetime NULL,    -- when the violation was waived
  fix_time datetime NULL,      -- when the violation disappeared entirely

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
