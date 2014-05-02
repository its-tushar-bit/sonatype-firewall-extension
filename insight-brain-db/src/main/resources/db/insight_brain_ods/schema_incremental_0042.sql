-- Since 1.11
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_violation
  ADD COLUMN policy_name varchar(60) NULL;
  
UPDATE policy_violation pv SET pv.policy_name=SELECT p.name FROM policy p WHERE p.policy_id=pv.policy_id;

ALTER TABLE policy_violation
  ALTER COLUMN policy_name varchar(60) NOT NULL;
  