-- Since 1.51
DELETE FROM policy_violation_aggregation;

ALTER TABLE policy_violation_aggregation DROP COLUMN resolved_count_low_threat;
ALTER TABLE policy_violation_aggregation DROP COLUMN resolved_count_moderate_threat;
ALTER TABLE policy_violation_aggregation DROP COLUMN resolved_count_severe_threat;
ALTER TABLE policy_violation_aggregation DROP COLUMN resolved_count_critical_threat;

ALTER TABLE policy_violation_aggregation ADD COLUMN (
  fixed_count_security_low_threat INTEGER NOT NULL,
  fixed_count_security_moderate_threat INTEGER NOT NULL,
  fixed_count_security_severe_threat INTEGER NOT NULL,
  fixed_count_security_critical_threat INTEGER NOT NULL,
  fixed_count_license_low_threat INTEGER NOT NULL,
  fixed_count_license_moderate_threat INTEGER NOT NULL,
  fixed_count_license_severe_threat INTEGER NOT NULL,
  fixed_count_license_critical_threat INTEGER NOT NULL,
  fixed_count_quality_low_threat INTEGER NOT NULL,
  fixed_count_quality_moderate_threat INTEGER NOT NULL,
  fixed_count_quality_severe_threat INTEGER NOT NULL,
  fixed_count_quality_critical_threat INTEGER NOT NULL,
  fixed_count_other_low_threat INTEGER NOT NULL,
  fixed_count_other_moderate_threat INTEGER NOT NULL,
  fixed_count_other_severe_threat INTEGER NOT NULL,
  fixed_count_other_critical_threat INTEGER NOT NULL,
  waived_count_security_low_threat INTEGER NOT NULL,
  waived_count_security_moderate_threat INTEGER NOT NULL,
  waived_count_security_severe_threat INTEGER NOT NULL,
  waived_count_security_critical_threat INTEGER NOT NULL,
  waived_count_license_low_threat INTEGER NOT NULL,
  waived_count_license_moderate_threat INTEGER NOT NULL,
  waived_count_license_severe_threat INTEGER NOT NULL,
  waived_count_license_critical_threat INTEGER NOT NULL,
  waived_count_quality_low_threat INTEGER NOT NULL,
  waived_count_quality_moderate_threat INTEGER NOT NULL,
  waived_count_quality_severe_threat INTEGER NOT NULL,
  waived_count_quality_critical_threat INTEGER NOT NULL,
  waived_count_other_low_threat INTEGER NOT NULL,
  waived_count_other_moderate_threat INTEGER NOT NULL,
  waived_count_other_severe_threat INTEGER NOT NULL,
  waived_count_other_critical_threat INTEGER NOT NULL,
  open_count_security INTEGER NOT NULL,
  open_count_license INTEGER NOT NULL,
  open_count_quality INTEGER NOT NULL,
  open_count_other INTEGER NOT NULL);

ALTER TABLE policy_violation_aggregation ADD COLUMN time_period varchar(20) NOT NULL DEFAULT 'MONTH';

ALTER TABLE policy_violation_aggregation DROP CONSTRAINT policy_violation_aggregation_uk;

ALTER TABLE policy_violation_aggregation ADD CONSTRAINT policy_violation_aggregation_uk UNIQUE KEY
    (application_id, time_period_start, time_period);

DROP TABLE success_metrics_report_data;

CREATE TABLE success_metrics_report_data (
  success_metrics_report_data_id VARCHAR(50) NOT NULL,
  last_updated DATETIME NOT NULL,
  included_application_ids_json CLOB NOT NULL,
  month_count SMALLINT NOT NULL,
  active_application_count INTEGER NOT NULL,
  chart_data_json CLOB NOT NULL,
  CONSTRAINT success_metrics_report_data_pk PRIMARY KEY (success_metrics_report_data_id),
  CONSTRAINT success_metrics_report_data_fk FOREIGN KEY (success_metrics_report_data_id) REFERENCES
      success_metrics_report(success_metrics_report_id)
);
