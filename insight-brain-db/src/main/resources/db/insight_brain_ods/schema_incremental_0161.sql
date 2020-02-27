-- Since 1.86
CREATE TABLE source_control_pull_request_comment
(
  source_control_pull_request_comment_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  pull_request_id int NOT NULL,
  pull_request_comment_id int NOT NULL,
  source_policy_evaluation_id varchar(50) NOT NULL,
  target_policy_evaluation_id varchar(50) NOT NULL,
  create_time timestamp NOT NULL,
  update_time timestamp,
  CONSTRAINT source_control_pull_request_comment_pk PRIMARY KEY (source_control_pull_request_comment_id),
  CONSTRAINT source_control_pull_request_application_pull_request_uk UNIQUE (application_id, pull_request_id),
  CONSTRAINT source_control_pull_request_comment_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT source_control_pull_request_source_policy_eval_fk FOREIGN KEY (source_policy_evaluation_id)
    REFERENCES policy_evaluation(policy_evaluation_id),
  CONSTRAINT source_control_pull_request_target_policy_eval_fk FOREIGN KEY (target_policy_evaluation_id)
    REFERENCES policy_evaluation(policy_evaluation_id)
);
