-- Since 1.108
ALTER TABLE policy_evaluation RENAME COLUMN trigger_type TO scan_trigger_type;
UPDATE policy_evaluation SET scan_trigger_type='UNKNOWN'
  WHERE scan_trigger_type='POLICY_MONITORING' OR scan_trigger_type='REEVALUATION';
  
ALTER TABLE source_control_event RENAME COLUMN policy_evaluation_trigger_type TO scan_trigger_type;
UPDATE source_control_event SET scan_trigger_type='UNKNOWN'
  WHERE scan_trigger_type='POLICY_MONITORING' OR scan_trigger_type='REEVALUATION';
