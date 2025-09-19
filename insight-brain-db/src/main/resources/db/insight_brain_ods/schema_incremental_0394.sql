-- since 1.192
-- SaaS Compatible
BEGIN;
ALTER TABLE policy_evaluation DROP CONSTRAINT IF EXISTS policy_evaluation_app_fk;
ALTER TABLE policy_evaluation ADD CONSTRAINT policy_evaluation_app_fk FOREIGN KEY (application_id)
  REFERENCES application(application_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE last_policy_evaluation DROP CONSTRAINT IF EXISTS last_policy_evaluation_eval_fk;
ALTER TABLE last_policy_evaluation ADD CONSTRAINT last_policy_evaluation_eval_fk FOREIGN KEY (policy_evaluation_id)
  REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE last_policy_evaluation DROP CONSTRAINT IF EXISTS last_policy_evaluation_app_fk;
ALTER TABLE last_policy_evaluation ADD CONSTRAINT last_policy_evaluation_app_fk FOREIGN KEY (application_id)
  REFERENCES application(application_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE source_control_pull_request_comment DROP CONSTRAINT IF EXISTS source_control_pull_request_source_policy_eval_fk;
ALTER TABLE source_control_pull_request_comment ADD CONSTRAINT source_control_pull_request_source_policy_eval_fk
  FOREIGN KEY (source_policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE source_control_pull_request_comment DROP CONSTRAINT IF EXISTS source_control_pull_request_target_policy_eval_fk;
ALTER TABLE source_control_pull_request_comment ADD CONSTRAINT source_control_pull_request_target_policy_eval_fk
  FOREIGN KEY (target_policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE source_control_default_branch_commit_history DROP CONSTRAINT IF EXISTS source_control_default_branch_commit_history_policy_eval_fk;
ALTER TABLE source_control_default_branch_commit_history ADD CONSTRAINT source_control_default_branch_commit_history_policy_eval_fk
  FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE source_control_event DROP CONSTRAINT IF EXISTS source_control_event_policy_evaluation_fk;
ALTER TABLE source_control_event ADD CONSTRAINT source_control_event_policy_evaluation_fk
  FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation(policy_evaluation_id) ON DELETE CASCADE;
COMMIT;
