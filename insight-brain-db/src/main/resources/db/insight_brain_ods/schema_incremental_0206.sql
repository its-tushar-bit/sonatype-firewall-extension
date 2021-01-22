-- Since 1.105
ALTER TABLE source_control_event
DROP CONSTRAINT IF EXISTS source_control_event_target_policy_evaluation_fk;

ALTER TABLE source_control_event
DROP COLUMN IF EXISTS target_policy_evaluation_id;
