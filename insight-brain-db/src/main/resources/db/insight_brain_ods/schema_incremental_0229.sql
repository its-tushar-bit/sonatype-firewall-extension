-- Since 1.121
ALTER TABLE source_control RENAME COLUMN enable_pull_requests TO remediation_pull_requests_enabled;
ALTER TABLE source_control RENAME COLUMN enable_status_checks TO status_checks_enabled;
ALTER TABLE source_control ADD COLUMN pull_request_commenting_enabled bool;
ALTER TABLE source_control ADD COLUMN source_control_scans_enabled bool;
ALTER TABLE source_control ADD COLUMN source_control_scan_target varchar(1000);
