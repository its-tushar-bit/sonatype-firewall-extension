-- 
-- DO NOT CHANGE THIS FILE!
-- 
-- This is a dump of the database schema at the time PostgreSQL support was added.
-- Given this as baseline, new migration scripts must be able to upgrade the PostgreSQL database.
--

CREATE SCHEMA insight_brain_aggregation;
SET SCHEMA 'insight_brain_aggregation';

-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE policy_violation_aggregation (
  policy_violation_aggregation_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  time_period_start timestamp NOT NULL,
  time_period_end timestamp NULL,
  mttr_low_threat bigint NULL,
  mttr_moderate_threat bigint NULL,
  mttr_severe_threat bigint NULL,
  mttr_critical_threat bigint NULL,
  fixed_count_security_low_threat int NOT NULL,
  fixed_count_security_moderate_threat int NOT NULL,
  fixed_count_security_severe_threat int NOT NULL,
  fixed_count_security_critical_threat int NOT NULL,
  fixed_count_license_low_threat int NOT NULL,
  fixed_count_license_moderate_threat int NOT NULL,
  fixed_count_license_severe_threat int NOT NULL,
  fixed_count_license_critical_threat int NOT NULL,
  fixed_count_quality_low_threat int NOT NULL,
  fixed_count_quality_moderate_threat int NOT NULL,
  fixed_count_quality_severe_threat int NOT NULL,
  fixed_count_quality_critical_threat int NOT NULL,
  fixed_count_other_low_threat int NOT NULL,
  fixed_count_other_moderate_threat int NOT NULL,
  fixed_count_other_severe_threat int NOT NULL,
  fixed_count_other_critical_threat int NOT NULL,
  waived_count_security_low_threat int NOT NULL,
  waived_count_security_moderate_threat int NOT NULL,
  waived_count_security_severe_threat int NOT NULL,
  waived_count_security_critical_threat int NOT NULL,
  waived_count_license_low_threat int NOT NULL,
  waived_count_license_moderate_threat int NOT NULL,
  waived_count_license_severe_threat int NOT NULL,
  waived_count_license_critical_threat int NOT NULL,
  waived_count_quality_low_threat int NOT NULL,
  waived_count_quality_moderate_threat int NOT NULL,
  waived_count_quality_severe_threat int NOT NULL,
  waived_count_quality_critical_threat int NOT NULL,
  waived_count_other_low_threat int NOT NULL,
  waived_count_other_moderate_threat int NOT NULL,
  waived_count_other_severe_threat int NOT NULL,
  waived_count_other_critical_threat int NOT NULL,
  discovered_count_security_low_threat int NOT NULL,
  discovered_count_security_moderate_threat int NOT NULL,
  discovered_count_security_severe_threat int NOT NULL,
  discovered_count_security_critical_threat int NOT NULL,
  discovered_count_license_low_threat int NOT NULL,
  discovered_count_license_moderate_threat int NOT NULL,
  discovered_count_license_severe_threat int NOT NULL,
  discovered_count_license_critical_threat int NOT NULL,
  discovered_count_quality_low_threat int NOT NULL,
  discovered_count_quality_moderate_threat int NOT NULL,
  discovered_count_quality_severe_threat int NOT NULL,
  discovered_count_quality_critical_threat int NOT NULL,
  discovered_count_other_low_threat int NOT NULL,
  discovered_count_other_moderate_threat int NOT NULL,
  discovered_count_other_severe_threat int NOT NULL,
  discovered_count_other_critical_threat int NOT NULL,
  evaluation_count int NOT NULL,
  time_period varchar(20) NOT NULL DEFAULT 'MONTH',
  open_count_security_low_threat int NOT NULL,
  open_count_security_moderate_threat int NOT NULL,
  open_count_security_severe_threat int NOT NULL,
  open_count_security_critical_threat int NOT NULL,
  open_count_license_low_threat int NOT NULL,
  open_count_license_moderate_threat int NOT NULL,
  open_count_license_severe_threat int NOT NULL,
  open_count_license_critical_threat int NOT NULL,
  open_count_quality_low_threat int NOT NULL,
  open_count_quality_moderate_threat int NOT NULL,
  open_count_quality_severe_threat int NOT NULL,
  open_count_quality_critical_threat int NOT NULL,
  open_count_other_low_threat int NOT NULL,
  open_count_other_moderate_threat int NOT NULL,
  open_count_other_severe_threat int NOT NULL,
  open_count_other_critical_threat int NOT NULL,

  CONSTRAINT policy_violation_aggregation_pk PRIMARY KEY (policy_violation_aggregation_id),
  CONSTRAINT policy_violation_aggregation_uk UNIQUE (application_id, time_period_start, time_period)
);

CREATE TABLE success_metrics_report (
  success_metrics_report_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL, -- The internal name of the User (CLM User or LDAP user)
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  scope_json text NOT NULL, -- The scope (app/org ids) stored in json format
  create_time timestamp NOT NULL,
  include_latest_data boolean NOT NULL DEFAULT true,
  CONSTRAINT success_metrics_report_pk PRIMARY KEY (success_metrics_report_id),
  CONSTRAINT success_metrics_report_uk UNIQUE (username, name_lowercase_no_whitespace)
);


-- Cached data to be displayed in this success metrics chart. Derived from the policy_violation_aggregations.
CREATE TABLE success_metrics_report_data (
  -- id and also foreign key to the success_metrics_report table. A success_metrics_report can have at most one
  -- success_metrics_report_data
  success_metrics_report_data_id varchar(50) NOT NULL,

  -- things that need to be tracked in order to determine when this data must be refreshed
  last_updated timestamp NOT NULL,
  included_application_ids_json text NOT NULL,

  -- summary data
  month_count smallint NOT NULL,
  active_application_count int NOT NULL,

  chart_data_json text NOT NULL,

  CONSTRAINT success_metrics_report_data_pk PRIMARY KEY (success_metrics_report_data_id),
  CONSTRAINT success_metrics_report_data_fk FOREIGN KEY (success_metrics_report_data_id) REFERENCES
      success_metrics_report(success_metrics_report_id)
);

CREATE TABLE schema_version (
  schema_version int NOT NULL
);
INSERT INTO schema_version (schema_version) VALUES (11);
