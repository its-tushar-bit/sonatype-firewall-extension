-- SaaS Compatible
-- Since 1.192
ALTER TABLE source_control_pull_request ADD COLUMN state varchar(50);
ALTER TABLE source_control_pull_request ADD COLUMN source varchar(50);
CREATE INDEX source_control_pull_request_source_idx ON source_control_pull_request(source);
