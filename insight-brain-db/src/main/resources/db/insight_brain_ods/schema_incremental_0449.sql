-- Since 1.204
-- SaaS Compatible
-- CLM-39926: PR auditability and traceability columns.
-- Purpose: Add execution-time auth context and outcome to source_control_event so that every
-- pull request creation can be traced to which auth was actually used and what happened, plus
-- a sourceControlEventId join key on source_control_pull_request linking the success row back
-- to the originating workflow row. All columns are nullable; no backfill is required.

ALTER TABLE source_control_event ADD COLUMN IF NOT EXISTS authentication_type varchar(32);
ALTER TABLE source_control_event ADD COLUMN IF NOT EXISTS auth_owner_id       varchar(50);
ALTER TABLE source_control_event ADD COLUMN IF NOT EXISTS github_app_id       varchar(50);
ALTER TABLE source_control_event ADD COLUMN IF NOT EXISTS installation_id     varchar(64);
ALTER TABLE source_control_event ADD COLUMN IF NOT EXISTS outcome             varchar(16);
ALTER TABLE source_control_event ADD COLUMN IF NOT EXISTS failure_reason      varchar(64);

-- Intentionally no FK to source_control_event: event rows are deleted via application ON DELETE CASCADE;
-- stale event IDs are tolerated here (audit-only join, never enforced at write time).
ALTER TABLE source_control_pull_request ADD COLUMN IF NOT EXISTS source_control_event_id varchar(50);
ALTER TABLE source_control_pull_request ADD COLUMN IF NOT EXISTS authentication_type     varchar(32);
ALTER TABLE source_control_pull_request ADD COLUMN IF NOT EXISTS auth_owner_id           varchar(50);
ALTER TABLE source_control_pull_request ADD COLUMN IF NOT EXISTS github_app_id           varchar(50);
ALTER TABLE source_control_pull_request ADD COLUMN IF NOT EXISTS installation_id         varchar(64);
