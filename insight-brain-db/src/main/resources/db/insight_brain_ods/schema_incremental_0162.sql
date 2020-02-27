-- Since 1.86
CREATE TABLE source_control_default_branch_commit_history
(
  source_control_default_branch_commit_history_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  commit_hash varchar(128) NOT NULL,
  commit_time timestamp  NOT NULL,
  policy_evaluation_id varchar(50),
  create_time timestamp NOT NULL,
  update_time timestamp,
  CONSTRAINT source_control_default_branch_commit_history_pk PRIMARY KEY (source_control_default_branch_commit_history_id),
  CONSTRAINT source_control_default_branch_commit_history_app_commit_uk UNIQUE (application_id, commit_hash),
  CONSTRAINT source_control_default_branch_commit_history_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id),
  CONSTRAINT source_control_default_branch_commit_history_policy_eval_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation (policy_evaluation_id)
);
