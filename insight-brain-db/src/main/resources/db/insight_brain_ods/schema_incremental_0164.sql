-- Since brain-next
ALTER TABLE source_control
  ADD COLUMN pull_request_cutoff_time timestamp NULL;

ALTER TABLE source_control
  ADD COLUMN pull_request_error_count INT NOT NULL DEFAULT 0;
