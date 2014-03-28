SET SCHEMA insight_brain_ods;

ALTER TABLE policy_violation
  ADD CONSTRAINT policy_violation_uk UNIQUE KEY (policy_evaluation_id, policy_id, group_id, artifact_id, version, hash);
  
ALTER TABLE policy_violation
  ADD COLUMN policy_name varchar(60) NULL;
  
UPDATE policy_violation pv SET pv.policy_name=SELECT p.name FROM policy p WHERE p.policy_id=pv.policy_id;

ALTER TABLE policy_violation
  ALTER COLUMN policy_name varchar(60) NOT NULL;
  