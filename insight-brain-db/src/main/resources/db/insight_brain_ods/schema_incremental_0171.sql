-- since 1.91
-- need to fix the order of creation of the constraints so that the auto-generated unique indexes are correct;
-- as is, the application FK constraint will share the unique constraint's index since the FK constraint was
-- created AFTER the unique constraint that uses that FK column;  will eventually cause an error with
-- the H2 -> postgres migration because the original unique index was not dropped when the unique constraint was
-- dropped and re-added

ALTER TABLE source_control_pull_request_comment
DROP CONSTRAINT source_control_pull_request_comment_app_fk;

-- need to drop this one temporarily so that the FK doesn't join it's unique index (and we're going to rename it anyway)
ALTER TABLE source_control_pull_request_comment
DROP CONSTRAINT source_control_pull_request_app_component_pull_request_uk;

-- need to explicitly drop the H2 generated index in the event the customer updated from IQ 86, 87 or 88 to 89 or 90
DROP INDEX IF EXISTS source_control_pull_request_application_pull_request_uk_INDEX_6;

-- the unique index will have dropped now as well
-- now recreate the constraints in the order that will cause the FK constraint to get its own index
ALTER TABLE source_control_pull_request_comment
ADD CONSTRAINT source_control_pull_request_comment_app_fk
  FOREIGN KEY (application_id) REFERENCES application(application_id);

ALTER TABLE source_control_pull_request_comment
ADD CONSTRAINT source_control_pull_request_uk UNIQUE (application_id, component_hash, pull_request_id);

-- repeat the same steps for the following table
ALTER TABLE source_control_default_branch_commit_history
DROP CONSTRAINT source_control_default_branch_commit_history_application_fk;

ALTER TABLE source_control_default_branch_commit_history
DROP CONSTRAINT source_control_default_branch_commit_history_app_commit_uk;

-- the unique index will have dropped now as well
-- now recreate the constraints in the order that will cause the FK constraint to get its own index
ALTER TABLE source_control_default_branch_commit_history
ADD CONSTRAINT source_control_default_branch_commit_history_application_fk
  FOREIGN KEY (application_id) REFERENCES application (application_id);

ALTER TABLE source_control_default_branch_commit_history
ADD CONSTRAINT source_control_default_branch_commit_history_uk UNIQUE (application_id, commit_hash);
