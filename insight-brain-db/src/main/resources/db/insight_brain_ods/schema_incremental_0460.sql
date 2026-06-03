-- SaaS Compatible
-- Tracks relay events processed by IQ so duplicates are silently dropped before mapping/publishing.
-- Primary dedup is by event_id (UUID from the relay); a secondary index on
-- (application_public_id, pull_request_number, commit_hash) covers the relay/legacy-polling cutover
-- where a logically equivalent event arrives with a different UUID. Rows are pruned by
-- RelayEventLogCleanupTask; the default retention is 7 days.

CREATE TABLE IF NOT EXISTS relay_event_log (
  relay_event_log_id varchar(50) NOT NULL,
  event_id varchar(255) NOT NULL,
  application_public_id varchar(200),
  pull_request_number int,
  commit_hash varchar(255),
  event_type varchar(64),
  processed_at timestamp NOT NULL,
  CONSTRAINT relay_event_log_pk PRIMARY KEY (relay_event_log_id),
  CONSTRAINT relay_event_log_event_id_uk UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS relay_event_log_secondary_idx
  ON relay_event_log(application_public_id, pull_request_number, commit_hash);
CREATE INDEX IF NOT EXISTS relay_event_log_processed_at_idx ON relay_event_log(processed_at);
