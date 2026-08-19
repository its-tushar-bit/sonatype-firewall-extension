-- SaaS Compatible
-- CLM-40971: CM Queue v1.1 hardening — index optimisations on continuous_monitoring_queue.
--
-- C2: Drop low-cardinality status-only index on continuous_monitoring_queue. status has only 3
--     distinct values (PENDING / IN_PROGRESS / FAILED) so the planner won't pick it; it just
--     adds write overhead.
-- C3: Recreate the (flow_type, priority DESC) composite index to also include create_time so
--     the consumer's acquirePending ORDER BY (priority DESC, create_time ASC) is fully
--     index-served (no sort pass).
--
-- The repository-table eligibility-filter index is created on Postgres only via
-- RepositoryHostedMonitoringIndexAsyncDbMigration; H2 dev/test fixtures are too small for the
-- planner to care, and H2 doesn't support partial indexes anyway.

DROP INDEX IF EXISTS continuous_monitoring_queue_status_idx;
DROP INDEX IF EXISTS continuous_monitoring_queue_flow_priority_idx;

CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_priority_create_idx
  ON continuous_monitoring_queue(flow_type, priority DESC, create_time ASC);
