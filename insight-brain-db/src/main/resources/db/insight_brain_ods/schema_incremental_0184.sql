-- Since 1.98
CREATE TABLE persisted_policy_evaluation_polling_result (
  persisted_policy_evaluation_polling_result_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  status_id varchar(50) NOT NULL,
  policy_evaluation_polling_result_json text NOT NULL,
  create_time timestamp NOT NULL,
  CONSTRAINT persisted_policy_evaluation_polling_result_pk PRIMARY KEY (persisted_policy_evaluation_polling_result_id),
  CONSTRAINT persisted_policy_evaluation_polling_result_uk UNIQUE (application_id, status_id)
);
CREATE INDEX persisted_policy_evaluation_polling_result_create_time_idx ON persisted_policy_evaluation_polling_result(create_time);
