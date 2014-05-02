-- Since 1.11
SET SCHEMA insight_brain_ods;

CREATE TABLE newest_policy_violation (
  policy_violation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  time datetime NOT NULL,
  CONSTRAINT newest_policy_violation_pk PRIMARY KEY (policy_violation_id),
  CONSTRAINT newest_policy_violation_violation_fk FOREIGN KEY (policy_violation_id) REFERENCES policy_violation(policy_violation_id),
  CONSTRAINT newest_policy_violation_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);
