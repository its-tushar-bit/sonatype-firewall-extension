-- Since 1.45.0
DROP TABLE first_occurrence_policy_violation;
DROP TABLE waived_policy_violation;
DROP TABLE policy_violation;

ALTER TABLE new_policy_violation RENAME TO policy_violation;

CREATE INDEX policy_violation_app_fix_time_stage_idx ON policy_violation(application_id, fix_time, stage_type_id);
CREATE INDEX policy_violation_policy_app_idx ON policy_violation(policy_id, application_id);
CREATE INDEX policy_violation_hash_idx ON policy_violation(hash);
