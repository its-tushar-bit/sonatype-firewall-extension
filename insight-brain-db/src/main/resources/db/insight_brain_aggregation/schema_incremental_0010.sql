-- Since 1.52
DELETE FROM policy_violation_aggregation;

ALTER TABLE policy_violation_aggregation DROP COLUMN open_count_security;
ALTER TABLE policy_violation_aggregation DROP COLUMN open_count_license;
ALTER TABLE policy_violation_aggregation DROP COLUMN open_count_quality;
ALTER TABLE policy_violation_aggregation DROP COLUMN open_count_other;

ALTER TABLE policy_violation_aggregation ADD COLUMN (
  open_count_security_low_threat INTEGER NOT NULL,
  open_count_security_moderate_threat INTEGER NOT NULL,
  open_count_security_severe_threat INTEGER NOT NULL,
  open_count_security_critical_threat INTEGER NOT NULL,
  open_count_license_low_threat INTEGER NOT NULL,
  open_count_license_moderate_threat INTEGER NOT NULL,
  open_count_license_severe_threat INTEGER NOT NULL,
  open_count_license_critical_threat INTEGER NOT NULL,
  open_count_quality_low_threat INTEGER NOT NULL,
  open_count_quality_moderate_threat INTEGER NOT NULL,
  open_count_quality_severe_threat INTEGER NOT NULL,
  open_count_quality_critical_threat INTEGER NOT NULL,
  open_count_other_low_threat INTEGER NOT NULL,
  open_count_other_moderate_threat INTEGER NOT NULL,
  open_count_other_severe_threat INTEGER NOT NULL,
  open_count_other_critical_threat INTEGER NOT NULL);
