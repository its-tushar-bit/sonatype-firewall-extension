-- SaaS Compatible
-- Realign the continuous_monitoring_queue partial index with the consumer's ORDER BY now that
-- per-row priority has been removed. The acquire query is
-- WHERE flow_type = ? AND status = 'PENDING' ORDER BY create_time ASC, id ASC LIMIT ?
-- so the new index leads with create_time ASC (priority is now constant) and adds id as the
-- stable tiebreaker.
--
-- continuous_monitoring_queue is a hot but bounded queue — PENDING rows live only until the
-- consumer drains them — so the partial-index size stays small in steady state and this index
-- swap is safe to run inline rather than via AsyncDbMigration. The replaced index is dropped in
-- the same migration; under MVCC concurrent readers continue to use the old index until commit.
--
-- SaaS-deploy safety: this migration runs in an ECS init container with a finite health-check
-- grace window (see insight-brain-db/src/main/resources/db/CLAUDE.md). The DROP + CREATE is
-- bounded by the partial-index size, which is bounded by the consumer drain rate. Even with a
-- maximum-realistic backlog (e.g. consumer paused for hours before a deploy), the partial index
-- covers only PENDING rows for one flow per tenant — orders of magnitude smaller than the full
-- table — so the DDL completes in milliseconds, well inside the grace window.
DROP INDEX IF EXISTS continuous_monitoring_queue_flow_priority_create_pending_idx;

CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_create_pending_idx
  ON continuous_monitoring_queue (flow_type, create_time ASC, id ASC) WHERE status = 'PENDING';
