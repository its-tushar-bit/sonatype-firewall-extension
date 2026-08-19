-- since 1.193
-- SaaS Compatible

BEGIN;
ALTER TABLE source_control_default_branch_commit_history DROP CONSTRAINT IF EXISTS source_control_default_branch_commit_history_application_fk;
ALTER TABLE source_control_default_branch_commit_history ADD CONSTRAINT source_control_default_branch_commit_history_application_fk
  FOREIGN KEY (application_id) REFERENCES application (application_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE source_control_event DROP CONSTRAINT IF EXISTS source_control_event_application_fk;
ALTER TABLE source_control_event ADD CONSTRAINT source_control_event_application_fk FOREIGN KEY (application_id)
  REFERENCES application (application_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE source_control_pull_request_result DROP CONSTRAINT IF EXISTS source_control_pull_request_result_application_fk;
ALTER TABLE source_control_pull_request_result ADD CONSTRAINT source_control_pull_request_result_application_fk
  FOREIGN KEY (application_id) REFERENCES application (application_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE source_control_user_activity DROP CONSTRAINT IF EXISTS source_control_user_activity_fk;
ALTER TABLE source_control_user_activity ADD CONSTRAINT source_control_user_activity_fk
  FOREIGN KEY (source_control_user_id) REFERENCES source_control_user(source_control_user_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE source_control_user DROP CONSTRAINT IF EXISTS source_control_user_application_fk;
DELETE FROM source_control_user
  WHERE NOT EXISTS (SELECT 1 FROM application a WHERE a.application_id = source_control_user.application_id);
ALTER TABLE source_control_user ADD CONSTRAINT source_control_user_application_fk FOREIGN KEY (application_id)
  REFERENCES application(application_id) ON DELETE CASCADE;
COMMIT;
