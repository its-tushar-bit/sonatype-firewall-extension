CREATE INDEX IF NOT EXISTS policy_violation_app_stage_open_unfixed_idx
  ON policy_violation (application_id, waive_time, stage_type_id, open_time DESC, threat_level DESC, policy_violation_id)
  WHERE fix_time IS NULL;

CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_priority_pending_idx
  ON continuous_monitoring_queue (flow_type, priority DESC) WHERE status = 'PENDING';
