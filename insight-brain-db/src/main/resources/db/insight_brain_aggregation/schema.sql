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

CREATE TABLE policy_violation_resolution_state (
  policy_violation_resolution_state_id VARCHAR(50) NOT NULL,
  application_id VARCHAR(50) NOT NULL,
  first_occurrence_time DATETIME NOT NULL,
  threat_category varchar(20) NOT NULL,

  -- PolicyViolationComparable fields
  policy_id varchar(50) NOT NULL,
  policy_name varchar(60) NOT NULL,
  threat_level smallint(2) NOT NULL,
  hash varchar(20),
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000),

  -- whether the violation is unresolved in each of the specified stages
  develop_stage_type BOOLEAN NOT NULL,
  build_stage_type BOOLEAN NOT NULL,
  stage_release_stage_type BOOLEAN NOT NULL,
  release_stage_type BOOLEAN NOT NULL,
  operate_stage_type BOOLEAN NOT NULL,
  proxy_stage_type BOOLEAN NOT NULL,

  CONSTRAINT policy_violation_resolution_state_pk PRIMARY KEY (policy_violation_resolution_state_id),
  CONSTRAINT policy_violation_resolution_state_stages_not_all_false CHECK develop_stage_type OR build_stage_type OR
      stage_release_stage_type OR release_stage_type OR operate_stage_type OR proxy_stage_type
);

CREATE INDEX policy_violation_resolution_state_application_id_idx ON policy_violation_resolution_state(application_id);
