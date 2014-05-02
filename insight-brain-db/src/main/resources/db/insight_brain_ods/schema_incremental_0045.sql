-- Since 1.11
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_violation
  ADD COLUMN time datetime NULL;
UPDATE policy_violation pv SET pv.time=SELECT pe.time FROM policy_evaluation pe WHERE pe.policy_evaluation_id=pv.policy_evaluation_id;
ALTER TABLE policy_violation
  ALTER COLUMN time datetime NOT NULL;
CREATE INDEX policy_violation_time_idx ON policy_violation(time);

ALTER TABLE newest_policy_violation
  DROP COLUMN time;
