-- Since 1.114
CREATE TABLE source_control_pull_request
(
  source_control_pull_request_id varchar(50) NOT NULL,
  repository_url_lowercase varchar(2048) NOT NULL,
  pull_request_id int NOT NULL,
  head_commit_hash varchar(128) NOT NULL,
  branch_name varchar(512) NOT NULL,
  create_time timestamp NOT NULL,
  last_check_time timestamp NOT NULL,
  last_detected_update_time timestamp NOT NULL,
  CONSTRAINT source_control_pull_request_pk PRIMARY KEY (source_control_pull_request_id),
  -- The "source_control_pull_request_uk" name is taken by a constraint incorrectly named in schema_incremental_0171
  CONSTRAINT source_control_pull_request_uk1 UNIQUE (repository_url_lowercase, pull_request_id)
);
