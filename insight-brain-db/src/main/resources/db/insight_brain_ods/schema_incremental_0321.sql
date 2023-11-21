-- Since 1.170
-- SaaS Compatible

CREATE TABLE source_control_user (
  source_control_user_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  email varchar(255) NOT NULL,
  CONSTRAINT source_control_user_pk PRIMARY KEY (source_control_user_id),
  CONSTRAINT email_application_id_uk UNIQUE (application_id, email)
);

CREATE TABLE source_control_user_activity (
  source_control_user_activity_id varchar(50) NOT NULL,
  source_control_user_id varchar(50) NOT NULL,
  commit_year_month timestamp NOT NULL,
  is_sent_to_telemetry boolean DEFAULT false NOT NULL,
  CONSTRAINT source_control_user_activity_pk PRIMARY KEY (source_control_user_activity_id),
  CONSTRAINT source_control_user_activity_fk FOREIGN KEY (source_control_user_id) REFERENCES source_control_user(source_control_user_id),
  CONSTRAINT userid_commit_id_uk UNIQUE (commit_year_month, source_control_user_id)
);
