-- SaaS Compatible
-- CLM-40971: CM Queue v1.1 hardening — index optimisations on continuous_monitoring_queue.
--
-- C2: Drop low-cardinality status-only index on continuous_monitoring_queue. status has only 3
--     distinct values (PENDING / IN_PROGRESS / FAILED) so the planner won't pick it; it just
--     adds write overhead.
-- C3: Recreate the (flow_type, priority DESC) WHERE status='PENDING' partial index to also
--     include create_time so the consumer's acquirePending ORDER BY (priority DESC, create_time
--     ASC) is fully index-served (no sort pass).
--
-- continuous_monitoring_queue is small (rows are short-lived: drained on success or terminal
-- failure), so plain CREATE INDEX inside the migration lock is safe. The repository-table
-- partial index for the eligibility query lives in RepositoryHostedMonitoringIndexAsyncDbMigration
-- because repository can grow large per tenant and CREATE INDEX CONCURRENTLY is forbidden in
-- migration scripts (deadlocks with ClusterLockManager — see db/CLAUDE.md).

DROP INDEX IF EXISTS continuous_monitoring_queue_status_idx;
DROP INDEX IF EXISTS continuous_monitoring_queue_flow_priority_pending_idx;

CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_priority_create_pending_idx
  ON continuous_monitoring_queue(flow_type, priority DESC, create_time ASC) WHERE status = 'PENDING';
