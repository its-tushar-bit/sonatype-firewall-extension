-- Since 1.72.0
-- add columns for configuring pull request feature
ALTER TABLE source_control ADD COLUMN base_branch varchar(243) NULL;
ALTER TABLE source_control ADD COLUMN enable_pull_requests boolean NULL;
ALTER TABLE source_control ADD COLUMN enable_status_checks boolean NULL;

-- backfill previous records
UPDATE source_control SET enable_pull_requests = false;
UPDATE source_control SET enable_status_checks = true;
