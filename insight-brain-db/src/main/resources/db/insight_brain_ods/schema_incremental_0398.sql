-- since 1.193
-- SaaS Compatible

ALTER TABLE source_control_default_branch_commit_history DROP CONSTRAINT source_control_default_branch_commit_history_application_fk;
ALTER TABLE source_control_default_branch_commit_history ADD CONSTRAINT source_control_default_branch_commit_history_application_fk
  FOREIGN KEY (application_id) REFERENCES application (application_id) ON DELETE CASCADE;

ALTER TABLE source_control_event DROP CONSTRAINT source_control_event_application_fk;
ALTER TABLE source_control_event ADD CONSTRAINT source_control_event_application_fk FOREIGN KEY (application_id)
  REFERENCES application (application_id) ON DELETE CASCADE;

ALTER TABLE source_control_pull_request_result DROP CONSTRAINT source_control_pull_request_result_application_fk;
ALTER TABLE source_control_pull_request_result ADD CONSTRAINT source_control_pull_request_result_application_fk
  FOREIGN KEY (application_id) REFERENCES application (application_id) ON DELETE CASCADE;

ALTER TABLE source_control_user_activity DROP CONSTRAINT source_control_user_activity_fk;
ALTER TABLE source_control_user_activity ADD CONSTRAINT source_control_user_activity_fk
  FOREIGN KEY (source_control_user_id) REFERENCES source_control_user(source_control_user_id) ON DELETE CASCADE;

DELETE FROM source_control_user
  WHERE application_id NOT IN (SELECT application_id FROM application);
ALTER TABLE source_control_user ADD CONSTRAINT source_control_user_application_fk FOREIGN KEY (application_id)
  REFERENCES application(application_id) ON DELETE CASCADE;
