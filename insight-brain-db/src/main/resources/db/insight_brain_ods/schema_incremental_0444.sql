-- SaaS Compatible
-- CLM-39312/CLM-39315: Create hosted_component_scan_queue table and add component_id traceability columns
-- Purpose: Queue and track scan jobs for hosted repository components with priority-based
-- processing, configurable retries (retry_count), and status/error tracking.
-- Also adds component_id to repository_component and repository_policy_violation to enable
-- traceability back to the originating NXRM component.

CREATE TABLE IF NOT EXISTS hosted_component_scan_queue (
  id varchar(255) NOT NULL,
  component_id varchar(255) NOT NULL,
  scan_file_id varchar(255) NOT NULL,
  status varchar(50) NOT NULL,
  priority integer NOT NULL DEFAULT 5,
  acquired_at timestamp,
  error_message varchar(2000),
  repository_id varchar(255) NOT NULL,
  retry_count integer NOT NULL DEFAULT 0,
  purl varchar(2000),
  policy_evaluation_stage varchar(50),
  CONSTRAINT hosted_component_scan_queue_pk PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS hosted_component_scan_queue_component_id_idx ON hosted_component_scan_queue(component_id);
CREATE INDEX IF NOT EXISTS hosted_component_scan_queue_status_idx ON hosted_component_scan_queue(status);
CREATE INDEX IF NOT EXISTS hosted_component_scan_queue_priority_idx ON hosted_component_scan_queue(priority);
CREATE INDEX IF NOT EXISTS hosted_component_scan_queue_status_priority_idx ON hosted_component_scan_queue(status, priority DESC);
CREATE INDEX IF NOT EXISTS hosted_component_scan_queue_acquired_at_idx ON hosted_component_scan_queue(acquired_at);
CREATE INDEX IF NOT EXISTS hosted_component_scan_queue_repository_id_idx ON hosted_component_scan_queue(repository_id);

ALTER TABLE repository_component ADD COLUMN IF NOT EXISTS component_id varchar(255);
ALTER TABLE repository_policy_violation ADD COLUMN IF NOT EXISTS component_id varchar(255);

-- since 1.203
-- CLM-39313: Add monitoring_enabled column to repository table for hosted repository evaluation
ALTER TABLE repository ADD COLUMN IF NOT EXISTS monitoring_enabled BOOLEAN DEFAULT FALSE NOT NULL;

-- CLM-39314: Add index on repository_component.last_evaluation_time for hosted repository evaluation queries
-- Purpose: Improve performance of queries that find components needing re-evaluation based on last_evaluation_time
CREATE INDEX IF NOT EXISTS repository_component_last_evaluation_time_idx ON repository_component(last_evaluation_time);

-- CLM-39317: Add last_evaluation_stage to repository_component for hosted repository CM
-- Purpose: Track the stage used in the last policy evaluation so CM always re-evaluates on the same stage
ALTER TABLE repository_component ADD COLUMN IF NOT EXISTS last_evaluation_stage varchar(50);
