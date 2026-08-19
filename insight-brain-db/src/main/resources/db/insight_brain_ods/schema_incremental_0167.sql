-- Since 1.89
ALTER TABLE source_control_pull_request_comment
ADD COLUMN component_hash varchar(20);

ALTER TABLE source_control_pull_request_comment
DROP CONSTRAINT source_control_pull_request_application_pull_request_uk;

ALTER TABLE source_control_pull_request_comment
ADD CONSTRAINT source_control_pull_request_app_component_pull_request_uk
  UNIQUE (application_id, component_hash, pull_request_id);
