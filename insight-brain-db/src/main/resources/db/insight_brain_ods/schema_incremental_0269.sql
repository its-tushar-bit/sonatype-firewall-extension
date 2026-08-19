-- Since 1.146
CREATE TABLE source_control_pull_request_result (
  source_control_pull_request_result_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  pull_request_result_json text NOT NULL,
  CONSTRAINT source_control_pull_request_result_pk PRIMARY KEY (source_control_pull_request_result_id),
  CONSTRAINT source_control_pull_request_result_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id)
);
CREATE INDEX source_control_pull_request_result_application_id_idx ON source_control_pull_request_result(application_id);
