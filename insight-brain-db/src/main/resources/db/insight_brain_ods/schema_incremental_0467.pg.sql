-- SaaS Compatible
-- CLM-40039: Unified Continuous Monitoring Queue (Hosted Repo v1)
-- Adds the shared continuous_monitoring_queue table and the per-flow Hosted Repo
-- satellite table continuous_monitoring_hosted_repo_item. The core table is flow-agnostic;
-- each flow (Hosted Repo v1, SBOM, Lifecycle) adds its own satellite table at
-- migration time.
--
-- PostgreSQL dialect: table definitions plus partial index on PENDING rows only.
-- Most queue rows live briefly (drained on success or terminal failure), so a partial
-- index is much smaller than a full composite and the planner can use it for the
-- consumer's acquire path.

CREATE TABLE IF NOT EXISTS continuous_monitoring_queue (
  id varchar(50) NOT NULL,
  flow_type varchar(50) NOT NULL,
  priority bigint NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  retry_count integer NOT NULL DEFAULT 0,
  create_time timestamp NOT NULL,
  update_time timestamp NOT NULL,
  acquired_at timestamp,
  worker_id varchar(50),
  error_message varchar(500),
  CONSTRAINT continuous_monitoring_queue_pk PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_status_idx ON continuous_monitoring_queue(status);

CREATE TABLE IF NOT EXISTS continuous_monitoring_hosted_repo_item (
  queue_id varchar(50) NOT NULL,
  repository_id varchar(255) NOT NULL,
  component_hash varchar(255) NOT NULL,
  CONSTRAINT continuous_monitoring_hosted_repo_item_pk PRIMARY KEY (queue_id),
  CONSTRAINT continuous_monitoring_hosted_repo_item_fk FOREIGN KEY (queue_id) REFERENCES continuous_monitoring_queue(id) ON DELETE CASCADE,
  CONSTRAINT continuous_monitoring_hosted_repo_item_natural_uk UNIQUE (repository_id, component_hash)
);
CREATE INDEX IF NOT EXISTS continuous_monitoring_hosted_repo_item_repository_id_idx ON continuous_monitoring_hosted_repo_item(repository_id);

-- PostgreSQL partial index for the acquire path: only PENDING rows, smaller footprint.
CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_flow_priority_pending_idx
  ON continuous_monitoring_queue(flow_type, priority DESC) WHERE status = 'PENDING';

-- Index on acquired_at supports a future stale-by-age reclaim path (mirrors the existing
-- hosted_component_scan_queue_acquired_at_idx on hosted_component_scan_queue). Keeps the
-- lookup cheap once a stale-reclaim filter on acquired_at lands on top of the worker-scoped
-- resetInProgressToPending introduced in this PR.
CREATE INDEX IF NOT EXISTS continuous_monitoring_queue_acquired_at_idx
  ON continuous_monitoring_queue(acquired_at);
