-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE policy_violation_aggregation (
  policy_violation_aggregation_id VARCHAR(50) NOT NULL,
  application_id VARCHAR(50) NOT NULL,
  time_period_start DATETIME NOT NULL,
  time_period_end DATETIME NULL,
  mttr_low_threat BIGINT NULL,
  mttr_moderate_threat BIGINT NULL,
  mttr_severe_threat BIGINT NULL,
  mttr_critical_threat BIGINT NULL,
  resolved_count_low_threat INTEGER NOT NULL,
  resolved_count_moderate_threat INTEGER NOT NULL,
  resolved_count_severe_threat INTEGER NOT NULL,
  resolved_count_critical_threat INTEGER NOT NULL,
  discovered_count_security_low_threat INTEGER NOT NULL,
  discovered_count_security_moderate_threat INTEGER NOT NULL,
  discovered_count_security_severe_threat INTEGER NOT NULL,
  discovered_count_security_critical_threat INTEGER NOT NULL,
  discovered_count_license_low_threat INTEGER NOT NULL,
  discovered_count_license_moderate_threat INTEGER NOT NULL,
  discovered_count_license_severe_threat INTEGER NOT NULL,
  discovered_count_license_critical_threat INTEGER NOT NULL,
  discovered_count_quality_low_threat INTEGER NOT NULL,
  discovered_count_quality_moderate_threat INTEGER NOT NULL,
  discovered_count_quality_severe_threat INTEGER NOT NULL,
  discovered_count_quality_critical_threat INTEGER NOT NULL,
  discovered_count_other_low_threat INTEGER NOT NULL,
  discovered_count_other_moderate_threat INTEGER NOT NULL,
  discovered_count_other_severe_threat INTEGER NOT NULL,
  discovered_count_other_critical_threat INTEGER NOT NULL,
  evaluation_count INTEGER NOT NULL,

  CONSTRAINT policy_violation_aggregation_pk PRIMARY KEY (policy_violation_aggregation_id),
  CONSTRAINT policy_violation_aggregation_uk UNIQUE KEY (application_id, time_period_start)
);

CREATE TABLE success_metrics_report (
  success_metrics_report_id VARCHAR(50) NOT NULL,
  username VARCHAR(60) NOT NULL, -- The internal name of the User (CLM User or LDAP user)
  name VARCHAR(60) NOT NULL,
  name_lowercase_no_whitespace VARCHAR(60) NOT NULL,
  scope_json CLOB NOT NULL, -- The scope (app/org ids) stored in json format
  create_time DATETIME NOT NULL,
  include_latest_data BOOLEAN NOT NULL DEFAULT true,
  CONSTRAINT success_metrics_report_pk PRIMARY KEY (success_metrics_report_id),
  CONSTRAINT success_metrics_report_uk UNIQUE KEY (username, name_lowercase_no_whitespace)
);


-- Cached data to be displayed in this success metrics chart. Derived from the policy_violation_aggregations.
CREATE TABLE success_metrics_report_data (
  -- id and also foreign key to the success_metrics_report table. A success_metrics_report can have at most one
  -- success_metrics_report_data
  success_metrics_report_data_id VARCHAR(50) NOT NULL,

  -- things that need to be tracked in order to determine when this data must be refreshed
  last_updated DATETIME NOT NULL,
  included_application_ids_json CLOB NOT NULL,

  -- summary data
  month_count SMALLINT NOT NULL,
  active_application_count INTEGER NOT NULL,

  -- averages chart data
  evaluations_per_month DOUBLE NOT NULL,
  total_policy_violations_per_application DOUBLE NOT NULL,
  total_critical_policy_violations_per_application DOUBLE NOT NULL,
  security_policy_violations_per_application DOUBLE NOT NULL,
  security_critical_policy_violations_per_application DOUBLE NOT NULL,
  license_policy_violations_per_application DOUBLE NOT NULL,
  license_critical_policy_violations_per_application DOUBLE NOT NULL,
  quality_policy_violations_per_application DOUBLE NOT NULL,
  quality_critical_policy_violations_per_application DOUBLE NOT NULL,
  other_policy_violations_per_application DOUBLE NOT NULL,
  other_critical_policy_violations_per_application DOUBLE NOT NULL,

  -- MTTR chart data
  mttr_month_1_time_period_start DATETIME NULL,
  mttr_month_1_all INTEGER NULL,
  mttr_month_1_critical INTEGER NULL,
  mttr_month_2_time_period_start DATETIME NULL,
  mttr_month_2_all INTEGER NULL,
  mttr_month_2_critical INTEGER NULL,
  mttr_month_3_time_period_start DATETIME NULL,
  mttr_month_3_all INTEGER NULL,
  mttr_month_3_critical INTEGER NULL,
  mttr_month_4_time_period_start DATETIME NULL,
  mttr_month_4_all INTEGER NULL,
  mttr_month_4_critical INTEGER NULL,
  mttr_month_5_time_period_start DATETIME NULL,
  mttr_month_5_all INTEGER NULL,
  mttr_month_5_critical INTEGER NULL,
  mttr_month_6_time_period_start DATETIME NULL,
  mttr_month_6_all INTEGER NULL,
  mttr_month_6_critical INTEGER NULL,
  mttr_month_7_time_period_start DATETIME NULL,
  mttr_month_7_all INTEGER NULL,
  mttr_month_7_critical INTEGER NULL,
  mttr_month_8_time_period_start DATETIME NULL,
  mttr_month_8_all INTEGER NULL,
  mttr_month_8_critical INTEGER NULL,
  mttr_month_9_time_period_start DATETIME NULL,
  mttr_month_9_all INTEGER NULL,
  mttr_month_9_critical INTEGER NULL,
  mttr_month_10_time_period_start DATETIME NULL,
  mttr_month_10_all INTEGER NULL,
  mttr_month_10_critical INTEGER NULL,
  mttr_month_11_time_period_start DATETIME NULL,
  mttr_month_11_all INTEGER NULL,
  mttr_month_11_critical INTEGER NULL,
  mttr_month_12_time_period_start DATETIME NULL,
  mttr_month_12_all INTEGER NULL,
  mttr_month_12_critical INTEGER NULL,

  -- applications chart data
  applications_with_violations_total INTEGER NOT NULL,
  applications_with_critical_violations_total INTEGER NOT NULL,
  applications_with_violations_security INTEGER NOT NULL,
  applications_with_critical_violations_security INTEGER NOT NULL,
  applications_with_violations_license INTEGER NOT NULL,
  applications_with_critical_violations_license INTEGER NOT NULL,
  applications_with_violations_quality INTEGER NOT NULL,
  applications_with_critical_violations_quality INTEGER NOT NULL,
  applications_with_violations_other INTEGER NOT NULL,
  applications_with_critical_violations_other INTEGER NOT NULL,

  CONSTRAINT success_metrics_report_data_pk PRIMARY KEY (success_metrics_report_data_id),
  CONSTRAINT success_metrics_report_data_fk FOREIGN KEY (success_metrics_report_data_id) REFERENCES
      success_metrics_report(success_metrics_report_id)
);
