-- SaaS Compatible
-- relay-dedup-mode: Add `mode` column to relay_event_log so the secondary dedup tuple
-- (application_public_id, pull_request_number, commit_hash) is also discriminated by relay
-- mode (PAT vs GitHub App). Without this, customers who migrate between modes have open PRs
-- whose new events collide with rows from the prior mode and are silently dropped as
-- secondary duplicates, freezing the customer's view at the migration cutoff.
--
-- Existing rows are backfilled to 'pat' since GitHub App mode shipped after PAT mode and the
-- historical window covered by this table (default retention 7 days) is PAT-only. The column
-- is left nullable so this script remains idempotent and so any pre-existing in-flight rows
-- inserted concurrently with the migration are still legal.

ALTER TABLE relay_event_log ADD COLUMN IF NOT EXISTS mode varchar(16);

UPDATE relay_event_log SET mode = 'pat' WHERE mode IS NULL;

DROP INDEX IF EXISTS relay_event_log_secondary_idx;

CREATE INDEX IF NOT EXISTS relay_event_log_secondary_idx
  ON relay_event_log(application_public_id, pull_request_number, commit_hash, mode);
