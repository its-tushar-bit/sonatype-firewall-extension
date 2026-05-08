CREATE INDEX IF NOT EXISTS policy_violation_app_stage_open_unfixed_idx
  ON policy_violation (application_id, waive_time, stage_type_id, open_time DESC, threat_level DESC, policy_violation_id)
  WHERE fix_time IS NULL;
