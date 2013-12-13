SET SCHEMA insight_brain_ods;

CREATE TABLE policy_monitoring (
  policy_monitoring_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  stage_type_id varchar(50) NOT NULL,
  CONSTRAINT policy_monitoring_pk PRIMARY KEY (policy_monitoring_id),
  CONSTRAINT policy_monitoring_uk UNIQUE KEY (owner_id)
);
