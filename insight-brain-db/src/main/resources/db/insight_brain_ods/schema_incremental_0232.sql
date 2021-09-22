-- Since 1.125
ALTER TABLE source_control_event ADD COLUMN base_commit_hash varchar(128);
ALTER TABLE source_control_event ADD COLUMN base_branch_name varchar(512);

ALTER TABLE source_control_pull_request ADD COLUMN base_commit_hash varchar(128);
ALTER TABLE source_control_pull_request ADD COLUMN base_branch_name varchar(512);
