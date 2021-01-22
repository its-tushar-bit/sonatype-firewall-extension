-- Since 1.105
ALTER TABLE policy_evaluation ADD COLUMN trigger_type varchar(50) NULL;

UPDATE policy_evaluation SET trigger_type='REEVALUATION' WHERE reevaluation=true AND for_monitoring=false;
UPDATE policy_evaluation SET trigger_type='POLICY_MONITORING' WHERE for_monitoring=true;
UPDATE policy_evaluation SET trigger_type='UNKNOWN' WHERE trigger_type IS NULL;

ALTER TABLE policy_evaluation ALTER COLUMN trigger_type SET NOT NULL;

ALTER TABLE source_control_event ADD COLUMN policy_evaluation_trigger_type varchar(50);
