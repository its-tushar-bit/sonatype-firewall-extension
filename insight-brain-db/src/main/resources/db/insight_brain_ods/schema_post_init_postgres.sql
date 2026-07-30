CREATE INDEX IF NOT EXISTS policy_violation_app_stage_open_unfixed_idx
  ON policy_violation (owner_id, waive_time, stage_type_id, open_time DESC, threat_level DESC, policy_violation_id)
  WHERE fix_time IS NULL;

CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_create_pending_idx
  ON continuous_monitoring_queue (flow_type, create_time ASC, id ASC) WHERE status = 'PENDING';

-- CLM-42077: supports the SLO violation feed all-states, per application-and-stage query ordered by
-- update time (greatest of open/waive/fix/legacy) ascending with policy_violation_id as a tiebreaker. Created
-- CONCURRENTLY on existing installs via SloViolationIndexAsyncDbMigration.
CREATE INDEX IF NOT EXISTS policy_violation_app_stage_updated_idx ON policy_violation (
  owner_id,
  stage_type_id,
  GREATEST(COALESCE(open_time, TIMESTAMP '1970-01-01 00:00:00'),
           COALESCE(waive_time, TIMESTAMP '1970-01-01 00:00:00'),
           COALESCE(fix_time, TIMESTAMP '1970-01-01 00:00:00'),
           COALESCE(legacy_violation_time, TIMESTAMP '1970-01-01 00:00:00')),
  policy_violation_id
);
