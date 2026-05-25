-- SaaS Compatible
-- CLM-39870: Add hosted_deployment_block and hosted_deployment_block_violation tables to persist
-- synchronous hosted-repository enforcement decisions that resulted in a blocked deployment.
-- Each block attempt is its own row (no unique constraint on pathname) so developer retries
-- preserve history. A separate periodic cleanup task deletes rows older than the configured
-- retention TTL; CM never reads from these tables so phantom components cannot be re-scanned.

CREATE TABLE IF NOT EXISTS hosted_deployment_block (
  hosted_deployment_block_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  pathname varchar(1000) NOT NULL,
  hash varchar(100),
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000),
  display_name varchar(1000),
  policy_action varchar(20) NOT NULL,
  highest_threat_level integer NOT NULL,
  evaluation_url varchar(2000),
  correlation_id varchar(100),
  requested_by varchar(200),
  blocked_time timestamp NOT NULL,
  CONSTRAINT hosted_deployment_block_pk PRIMARY KEY (hosted_deployment_block_id),
  CONSTRAINT hosted_deployment_block_repository_fk FOREIGN KEY (repository_id)
    REFERENCES repository(repository_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS hosted_deployment_block_repository_time_idx
  ON hosted_deployment_block(repository_id, blocked_time);

CREATE INDEX IF NOT EXISTS hosted_deployment_block_cleanup_idx
  ON hosted_deployment_block(blocked_time);

CREATE TABLE IF NOT EXISTS hosted_deployment_block_violation (
  hosted_deployment_block_violation_id varchar(50) NOT NULL,
  hosted_deployment_block_id varchar(50) NOT NULL,
  policy_name varchar(200) NOT NULL,
  constraint_name varchar(200),
  reason varchar(2000),
  component_identifier varchar(500),
  CONSTRAINT hosted_deployment_block_violation_pk PRIMARY KEY (hosted_deployment_block_violation_id),
  CONSTRAINT hosted_deployment_block_violation_block_fk FOREIGN KEY (hosted_deployment_block_id)
    REFERENCES hosted_deployment_block(hosted_deployment_block_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS hosted_deployment_block_violation_block_idx
  ON hosted_deployment_block_violation(hosted_deployment_block_id);
