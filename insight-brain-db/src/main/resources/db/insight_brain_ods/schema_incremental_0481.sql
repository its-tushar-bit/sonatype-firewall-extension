-- SaaS Compatible
-- CLM-43997: Index health and rebuild control plane.
-- New durable tables for generations/jobs/health/estate + outbox hardening columns.
-- Empty tables + singleton CURRENT seeds only; no bulk DML on large tables.
-- One-generation-per-role for SERVING/BUILDING is not constrained anywhere yet: nothing writes to
-- search_index_generation at this point, so there is no enforcement to describe. H2 lacks partial
-- unique indexes and both dialects share this schema, so whichever promotion path lands first has to
-- enforce it in the DAO rather than here.

-- Blue/green generation metadata (filesystem path or OpenSearch index name).
CREATE TABLE search_index_generation (
  search_index_generation_id varchar(50) NOT NULL,
  backend varchar(30) NOT NULL,
  role varchar(20) NOT NULL,
  schema_version integer NOT NULL,
  storage_ref varchar(500) NOT NULL,
  doc_count bigint,
  created_at timestamp NOT NULL,
  serving_since timestamp,
  retired_at timestamp,
  created_by_job_id varchar(50),
  CONSTRAINT search_index_generation_pk PRIMARY KEY (search_index_generation_id)
);
CREATE INDEX search_index_generation_role_idx ON search_index_generation(role);
CREATE INDEX search_index_generation_created_by_job_idx ON search_index_generation(created_by_job_id);
CREATE INDEX search_index_generation_serving_since_idx ON search_index_generation(serving_since);

-- Index / Maintenance jobs.
-- active_slot holds a constant while a job is PENDING/RUNNING/CANCELLING and is null once terminal, so
-- the UNIQUE constraint permits exactly one active job. Both dialects exclude nulls from uniqueness,
-- which a filtered unique index would express directly but H2 does not support.
CREATE TABLE search_index_job (
  search_index_job_id varchar(50) NOT NULL,
  job_type varchar(40) NOT NULL,
  trigger varchar(40) NOT NULL,
  status varchar(30) NOT NULL,
  active_slot varchar(10),
  progress_percent smallint NOT NULL DEFAULT 0,
  phase varchar(80),
  eta_finish_at timestamp,
  started_at timestamp,
  finished_at timestamp,
  cancel_requested_at timestamp,
  building_generation_id varchar(50),
  serving_generation_id_at_start varchar(50),
  recommended_op varchar(40),
  error_code varchar(80),
  error_message varchar(2000),
  created_by_user_id varchar(200),
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  CONSTRAINT search_index_job_pk PRIMARY KEY (search_index_job_id),
  CONSTRAINT search_index_job_active_slot_uk UNIQUE (active_slot)
);
CREATE INDEX search_index_job_status_started_idx ON search_index_job(status, started_at);
CREATE INDEX search_index_job_finished_idx ON search_index_job(finished_at);
CREATE INDEX search_index_job_type_status_idx ON search_index_job(job_type, status);
CREATE INDEX search_index_job_building_gen_idx ON search_index_job(building_generation_id);

-- Structured activity for wizard/job logs (not raw outbox).
CREATE TABLE search_index_job_event (
  search_index_job_event_id varchar(50) NOT NULL,
  search_index_job_id varchar(50) NOT NULL,
  seq bigint NOT NULL,
  severity varchar(20) NOT NULL,
  event_code varchar(80) NOT NULL,
  message varchar(2000) NOT NULL,
  created_at timestamp NOT NULL,
  CONSTRAINT search_index_job_event_pk PRIMARY KEY (search_index_job_event_id),
  CONSTRAINT search_index_job_event_job_seq_uk UNIQUE (search_index_job_id, seq)
);
CREATE INDEX search_index_job_event_job_created_idx ON search_index_job_event(search_index_job_id, created_at);
CREATE INDEX search_index_job_event_created_idx ON search_index_job_event(created_at);

-- Singleton CURRENT health row. Queue depth and lag columns are refreshed from the outbox rather than
-- incremented per insert; the failed tally is a history of events and has no table to count back from.
CREATE TABLE search_index_health (
  search_index_health_id varchar(50) NOT NULL,
  health_status varchar(20) NOT NULL,
  recommended_op varchar(40),
  queue_lag_seconds bigint NOT NULL DEFAULT 0,
  pending_change_count bigint NOT NULL DEFAULT 0,
  failed_change_count bigint NOT NULL DEFAULT 0,
  failed_change_window_start timestamp,
  oldest_pending_created_at timestamp,
  serving_generation_id varchar(50),
  active_job_id varchar(50),
  noux_unlock_state varchar(30) NOT NULL,
  last_successful_cutover_at timestamp,
  last_cleanup_at timestamp,
  updated_at timestamp NOT NULL,
  CONSTRAINT search_index_health_pk PRIMARY KEY (search_index_health_id)
);

INSERT INTO search_index_health (
  search_index_health_id, health_status, recommended_op, queue_lag_seconds, pending_change_count,
  failed_change_count, noux_unlock_state, updated_at
) VALUES (
  'CURRENT', 'HEALTHY', 'NONE', 0, 0, 0, 'NOT_STARTED', CURRENT_TIMESTAMP
);

-- Estate sizing / ETA snapshot for Analyze (refreshed on demand / periodic — not per page load).
CREATE TABLE search_index_estate_snapshot (
  search_index_estate_snapshot_id varchar(50) NOT NULL,
  application_count bigint NOT NULL DEFAULT 0,
  violation_count bigint NOT NULL DEFAULT 0,
  component_count bigint,
  eta_low_minutes integer,
  eta_high_minutes integer,
  advanced_search_enabled boolean NOT NULL DEFAULT FALSE,
  captured_at timestamp NOT NULL,
  CONSTRAINT search_index_estate_snapshot_pk PRIMARY KEY (search_index_estate_snapshot_id)
);

INSERT INTO search_index_estate_snapshot (
  search_index_estate_snapshot_id, application_count, violation_count,
  advanced_search_enabled, captured_at
) VALUES (
  'CURRENT', 0, 0, FALSE, CURRENT_TIMESTAMP
);

-- Harden outbox for lag / failure signals. New columns appended (nullable for existing rows).
-- New inserts set created_at/status. Queue depth and lag are derived from this table on read rather
-- than tallied on a shared row, which is what status/created_at is indexed for.
ALTER TABLE search_index_change ADD COLUMN created_at timestamp;
ALTER TABLE search_index_change ADD COLUMN status varchar(20);
ALTER TABLE search_index_change ADD COLUMN attempt_count integer;
ALTER TABLE search_index_change ADD COLUMN last_error varchar(2000);
ALTER TABLE search_index_change ADD COLUMN available_at timestamp;
CREATE INDEX search_index_change_status_created_idx ON search_index_change(status, created_at);
