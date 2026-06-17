-- H2 post-init: indexes that differ from the PostgreSQL variant.
-- The continuous_monitoring_queue flow-priority index for H2 is a plain composite
-- because H2 does not support partial indexes (WHERE clause is ignored).

CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_priority_idx
  ON continuous_monitoring_queue (flow_type, priority DESC);
