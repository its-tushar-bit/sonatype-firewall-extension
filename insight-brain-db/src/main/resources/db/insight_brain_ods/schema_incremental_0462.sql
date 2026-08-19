-- SaaS Compatible
-- relay-dedup-event-type: extend the secondary dedup index to include `event_type`. Without
-- it, a close + reopen of the same PR with the same head SHA produce two events with the
-- same (application_public_id, pull_request_number, commit_hash, mode) tuple — different
-- event_types ('pull_request_closed' vs 'pull_request_opened'), but the secondary check
-- collapses them and silently drops the second one. A reopen is a logically distinct event
-- (drives a fresh scan workflow); preserving it requires event_type as part of the key.
--
-- The cutover-protection rationale for the original 4-column index still holds: relay-vs-
-- legacy duplicates of the same logical event share event_type (both produce
-- 'pull_request_opened' / 'push' / etc.), so adding event_type does not weaken cutover
-- dedup. The change only ADDS discrimination between distinct event_types on the same
-- secondary tuple.
--
-- The column already exists on the table (0460 created relay_event_log with event_type);
-- this migration only swaps the index.

DROP INDEX IF EXISTS relay_event_log_secondary_idx;

CREATE INDEX IF NOT EXISTS relay_event_log_secondary_idx
  ON relay_event_log(application_public_id, pull_request_number, commit_hash, mode, event_type);
