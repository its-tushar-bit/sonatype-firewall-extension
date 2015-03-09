-- Since 1.13.0
SET SCHEMA insight_brain_ods;

CREATE TABLE policy_violation_new (
  policy_violation_id varchar(50) NOT NULL,
  policy_evaluation_id varchar(50) NOT NULL,
  time datetime NOT NULL,
  policy_id varchar(50) NOT NULL,
  policy_name varchar(60) NOT NULL, -- the policy name as it was when the policy violation was generated
  threat_level smallint(2) NOT NULL,
  threat_category varchar(20) NOT NULL,
  hash varchar(20),
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000), -- the component identifier coordinates (that caused the policy violation) stored in json format
  constraint_facts_json CLOB NOT NULL, -- the constraint facts (that caused the policy violation) stored in json format
  pathnames CLOB, -- the paths to the component that caused the policy violation, paths are new line delimited
  action_type_id varchar(20),
  notifications CLOB, -- email addresses notified for this policy violation, delimited by new lines
  waived bool DEFAULT false NOT NULL
);

INSERT INTO policy_violation_new
SELECT policy_violation_id, policy_evaluation_id, time, policy_id, policy_name, threat_level, threat_category, hash,
       'maven' as component_id_format,
       '{"artifactId":"' || STRINGENCODE(artifact_id) || '","groupId":"' || STRINGENCODE(group_id) || '","version":"' || STRINGENCODE(version) || '"}' as component_id_coordinates_json,
       constraint_facts_json, pathnames, action_type_id, notifications, waived
  FROM policy_violation WHERE group_id IS NOT NULL;

INSERT INTO policy_violation_new
SELECT policy_violation_id, policy_evaluation_id, time, policy_id, policy_name, threat_level, threat_category, hash,
       NULL as component_id_format,
       NULL as component_id_coordinates_json,
       constraint_facts_json, pathnames, action_type_id, notifications, waived
  FROM policy_violation WHERE group_id IS NULL;


-- Remove references
ALTER TABLE waived_policy_violation DROP CONSTRAINT waived_policy_violation_violation_fk;
ALTER TABLE first_occurrence_policy_violation DROP CONSTRAINT first_occurrence_violation_violation_fk;

DROP TABLE policy_violation;

-- Move table
ALTER TABLE policy_violation_new RENAME TO policy_violation;

-- add constraints
ALTER TABLE policy_violation ADD CONSTRAINT policy_violation_pk PRIMARY KEY (policy_violation_id);
ALTER TABLE policy_violation ADD CONSTRAINT policy_violation_evaluation_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id);

CREATE INDEX policy_violation_time_idx ON policy_violation(time);
CREATE INDEX policy_violation_policy_id_idx ON policy_violation(policy_id);
CREATE INDEX policy_violation_hash_idx ON policy_violation(hash);

-- add references
ALTER TABLE first_occurrence_policy_violation ADD CONSTRAINT first_occurrence_violation_violation_fk FOREIGN KEY (policy_violation_id) REFERENCES policy_violation(policy_violation_id);
ALTER TABLE waived_policy_violation ADD CONSTRAINT waived_policy_violation_violation_fk FOREIGN KEY (policy_violation_id) REFERENCES policy_violation(policy_violation_id);
