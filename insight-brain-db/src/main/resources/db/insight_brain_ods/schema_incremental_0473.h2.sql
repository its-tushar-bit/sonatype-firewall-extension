-- SaaS Compatible
-- H2 variant of the continuous_monitoring_queue index realignment. H2 does not support partial
-- indexes (the WHERE clause is silently ignored), so the H2 index is plain.
DROP INDEX IF EXISTS continuous_monitoring_queue_flow_priority_create_idx;

CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_create_idx
  ON continuous_monitoring_queue (flow_type, create_time ASC, id ASC);
