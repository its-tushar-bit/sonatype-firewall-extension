-- Since 1.97
-- these new/modified columns are to support automated pull request events
ALTER TABLE source_control_event ALTER COLUMN commit_hash DROP NOT NULL;
ALTER TABLE source_control_event ALTER COLUMN policy_evaluation_id DROP NOT NULL;

ALTER TABLE source_control_event ADD COLUMN component_id_format varchar(50);
ALTER TABLE source_control_event ADD COLUMN component_id_coordinates_json varchar(1000);
ALTER TABLE source_control_event ADD COLUMN scan_id varchar(50);
ALTER TABLE source_control_event ADD COLUMN stage_type_id varchar(30);
ALTER TABLE source_control_event ADD COLUMN remediation_version varchar(100);
ALTER TABLE source_control_event ADD COLUMN pull_request_contents text;
