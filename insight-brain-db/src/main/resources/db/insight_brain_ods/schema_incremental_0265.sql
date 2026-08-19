-- Since 1.140
CREATE TABLE source_control_configuration (
  source_control_configuration_id varchar(50) NOT NULL,
  clone_directory varchar(1000) NOT NULL,
  git_implementation varchar(20),
  pr_comment_purge_window int,
  pr_event_purge_window int,
  git_executable varchar(1000),
  git_timeout_seconds int NOT NULL DEFAULT 0,
  commit_username varchar(256),
  commit_email varchar(256),
  use_username_in_repository_clone_url boolean NOT NULL DEFAULT false,
  default_branch_monitoring_start_time varchar(5),
  default_branch_monitoring_interval_hours int NOT NULL DEFAULT 24,
  pull_request_monitoring_interval_seconds int NOT NULL DEFAULT 60,
  CONSTRAINT source_control_configuration_pk PRIMARY KEY (source_control_configuration_id)
);
