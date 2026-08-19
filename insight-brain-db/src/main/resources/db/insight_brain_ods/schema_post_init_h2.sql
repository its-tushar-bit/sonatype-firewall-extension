-- H2 post-init: indexes that differ from the PostgreSQL variant.
-- The continuous_monitoring_queue acquire-supporting index for H2 is a plain composite
-- because H2 does not support partial indexes (WHERE clause is ignored).

CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_create_idx
  ON continuous_monitoring_queue (flow_type, create_time ASC, id ASC);

-- CLM-42077: the SLO violation feed is ordered by update time (greatest of open/waive/fix/legacy). That requires
-- an expression index, which H2 1.4.196 does not support. On H2 (embedded, dev/test) we therefore rely on the
-- base application/stage b-tree columns for lookup plus an in-memory sort to resolve the update-time ordering;
-- the equivalent expression index only exists on Postgres (see schema_post_init_postgres.sql).
