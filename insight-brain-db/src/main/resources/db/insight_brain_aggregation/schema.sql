-- For tests only
CREATE TABLE IF NOT EXISTS test_table (
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

CREATE TABLE IF NOT EXISTS schema_version (
  data_store_id varchar(32) NOT NULL,
  schema_version int NOT NULL
);
INSERT INTO schema_version (data_store_id , schema_version) VALUES ('insight_brain_aggregation', -1);

CREATE TABLE firewall_metrics (
    firewall_metrics_id varchar(50) NOT NULL,
    metrics_date date NOT NULL,
    metrics_name varchar(255) NOT NULL,
    metrics_value int NOT NULL,
    metrics_last_updated_at timestamp NOT NULL,

    CONSTRAINT firewall_metrics_id_pk PRIMARY KEY (firewall_metrics_id),
    CONSTRAINT firewall_metrics_date_name_uk UNIQUE (metrics_date, metrics_name)
);
CREATE INDEX firewall_metrics_name_date_idx ON firewall_metrics(metrics_name, metrics_date);

CREATE TABLE roi_configuration (
  roi_configuration_id varchar(50) NOT NULL,
  currency char(3) NOT NULL,
  malware_attacks_prevented numeric,
  namespace_attacks_prevented numeric,
  safe_components_auto_selected numeric,
  baseline_days_to_resolve_violation int,
  daily_risk_cost_of_unfixed_violation numeric,

  CONSTRAINT roi_configuration_id_pk PRIMARY KEY (roi_configuration_id),
  CONSTRAINT roi_configuration_currency_uk UNIQUE (currency)
);

CREATE TABLE roi_configuration_default_values (
  roi_configuration_default_values_id varchar(50) NOT NULL,
  currency char(3) NOT NULL,
  malware_attacks_prevented_default numeric,
  malware_attacks_prevented_minimum numeric,
  namespace_attacks_prevented_default numeric,
  namespace_attacks_prevented_minimum numeric,
  safe_components_auto_selected_default numeric,
  safe_components_auto_selected_minimum numeric,
  baseline_days_to_resolve_violation_default int,
  baseline_days_to_resolve_violation_minimum int,
  daily_risk_cost_of_unfixed_violation_default numeric,
  daily_risk_cost_of_unfixed_violation_minimum numeric,

  CONSTRAINT roi_configuration_default_values_id_pk PRIMARY KEY (roi_configuration_default_values_id),
  CONSTRAINT roi_configuration_default_values_currency_uk UNIQUE (currency)
);

INSERT INTO roi_configuration_default_values(
  roi_configuration_default_values_id,
  currency,
  malware_attacks_prevented_default,
  malware_attacks_prevented_minimum,
  namespace_attacks_prevented_default,
  namespace_attacks_prevented_minimum,
  safe_components_auto_selected_default,
  safe_components_auto_selected_minimum,
  baseline_days_to_resolve_violation_default,
  baseline_days_to_resolve_violation_minimum,
  daily_risk_cost_of_unfixed_violation_default,
  daily_risk_cost_of_unfixed_violation_minimum
) VALUES ('dd97d70ae24a446ab65c525b6f2b4786', 'USD',  4350000, 500000, 35000, 10000, 25000, 5000, 30, 15, 800, 400);
