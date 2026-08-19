-- Since 1.86
ALTER TABLE source_control
  ADD COLUMN pull_request_cutoff_time timestamp NULL;

ALTER TABLE source_control
  ADD COLUMN pull_request_error_count INT NOT NULL DEFAULT 0;
