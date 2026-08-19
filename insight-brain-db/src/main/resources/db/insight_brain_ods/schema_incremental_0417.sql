-- Add missing ON DELETE CASCADE to source_control_pull_request_comment application foreign key
-- SaaS Compatible

BEGIN;
ALTER TABLE source_control_pull_request_comment DROP CONSTRAINT IF EXISTS source_control_pull_request_comment_app_fk;
ALTER TABLE source_control_pull_request_comment ADD CONSTRAINT source_control_pull_request_comment_app_fk 
  FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE;
COMMIT;
