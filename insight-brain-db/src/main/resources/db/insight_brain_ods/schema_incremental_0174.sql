-- Since 1.95
CREATE TABLE source_control_event (
  source_control_event_id varchar(50) NOT NULL,
  instance_id varchar(50) NULL,
  application_id varchar(50) NOT NULL,
  event_type varchar(50) NOT NULL,
  event_status varchar(50) NOT NULL,
  event_status_details varchar(2048) NULL,
  commit_hash varchar(128) NOT NULL,
  policy_evaluation_id varchar(50) NOT NULL,
  target_policy_evaluation_id varchar(50) NULL,
  branch_name varchar(512) NULL,
  pull_request_number int NULL,
  scm_username varchar(255) NULL,
  initiator varchar(50) NULL,
  create_time timestamp NOT NULL,
  start_time timestamp NULL,
  complete_time timestamp NULL,
  CONSTRAINT source_control_event_pk PRIMARY KEY (source_control_event_id),
  CONSTRAINT source_control_event_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id),
  CONSTRAINT source_control_event_policy_evaluation_fk FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluation (policy_evaluation_id),
  CONSTRAINT source_control_event_target_policy_evaluation_fk FOREIGN KEY (target_policy_evaluation_id) REFERENCES policy_evaluation (policy_evaluation_id)
);
CREATE INDEX source_control_event_instance_id_idx ON source_control_event(instance_id);
CREATE INDEX source_control_event_create_time_idx ON source_control_event(create_time);
CREATE INDEX source_control_event_event_status_idx ON source_control_event(event_status);
CREATE INDEX source_control_event_application_id_idx ON source_control_event(application_id);
