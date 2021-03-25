-- Since 1.108
UPDATE policy_evaluation SET scan_trigger_type='SOURCE_CONTROL_INTERNAL_ONBOARDING'
  WHERE scan_trigger_type='SOURCE_CONTROL_INTERNAL';
  
UPDATE source_control_event SET scan_trigger_type='SOURCE_CONTROL_INTERNAL_ONBOARDING'
  WHERE scan_trigger_type='SOURCE_CONTROL_INTERNAL';
