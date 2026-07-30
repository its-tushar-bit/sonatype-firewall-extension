-- SaaS Compatible
-- CLM-42785: widen LC scan-based tables to owner_id. Pattern A renames the owner_component
-- cluster; Pattern B renames columns on tables that keep their names via a _t base table and a
-- compat view exposing both column names. Views are collapsed in release N+1 (CLM-42778).

-- Pattern A: application_component -> owner_component
ALTER TABLE application_component RENAME TO owner_component;
ALTER TABLE owner_component RENAME COLUMN application_component_id TO owner_component_id;
ALTER TABLE owner_component RENAME COLUMN application_id TO owner_id;
ALTER TABLE owner_component RENAME CONSTRAINT application_component_pk TO owner_component_pk;
ALTER TABLE owner_component RENAME CONSTRAINT application_component_uk TO owner_component_uk;
ALTER TABLE owner_component DROP CONSTRAINT application_component_application_fk;
ALTER INDEX application_component_hash_idx RENAME TO owner_component_hash_idx;
ALTER INDEX application_component_time_idx RENAME TO owner_component_time_idx;
CREATE VIEW application_component AS
  SELECT owner_component_id AS application_component_id, owner_id AS application_id,
         stage_type_id, time, hash, component_id_format, component_id_coordinates_json,
         match_state_id, identification_source_id, proprietary, pathnames
  FROM owner_component;

-- Pattern A: application_component_license -> owner_component_license
ALTER TABLE application_component_license RENAME TO owner_component_license;
ALTER TABLE owner_component_license RENAME COLUMN application_component_license_id TO owner_component_license_id;
ALTER TABLE owner_component_license RENAME COLUMN application_component_id TO owner_component_id;
ALTER TABLE owner_component_license RENAME CONSTRAINT application_component_license_pk TO owner_component_license_pk;
ALTER TABLE owner_component_license RENAME CONSTRAINT application_component_license_uk TO owner_component_license_uk;
ALTER TABLE owner_component_license RENAME CONSTRAINT application_component_license_application_component_fk TO owner_component_license_owner_component_fk;
ALTER INDEX application_component_license_effective_license_id_idx RENAME TO owner_component_license_effective_license_id_idx;
CREATE VIEW application_component_license AS
  SELECT owner_component_license_id AS application_component_license_id,
         owner_component_id AS application_component_id, effective_license_id
  FROM owner_component_license;

-- Pattern B: aggregate_file column rename
ALTER TABLE aggregate_file RENAME TO aggregate_file_t;
ALTER TABLE aggregate_file_t RENAME COLUMN application_component_id TO owner_component_id;
ALTER TABLE aggregate_file_t RENAME CONSTRAINT aggregate_file_application_component_fk TO aggregate_file_owner_component_fk;
ALTER INDEX aggregate_file_application_component_id_idx RENAME TO aggregate_file_owner_component_id_idx;
CREATE VIEW aggregate_file AS
  SELECT aggregate_file_id, owner_component_id, owner_component_id AS application_component_id,
         hash, pathnames
  FROM aggregate_file_t;

-- Pattern B: policy_evaluation column rename
ALTER TABLE policy_evaluation RENAME TO policy_evaluation_t;
ALTER TABLE policy_evaluation_t RENAME COLUMN application_id TO owner_id;
ALTER TABLE policy_evaluation_t DROP CONSTRAINT policy_evaluation_app_fk;
ALTER INDEX policy_evaluation_app_monitoring_stage_idx RENAME TO policy_evaluation_owner_monitoring_stage_idx;
ALTER INDEX policy_evaluation_app_time_idx RENAME TO policy_evaluation_owner_time_idx;
ALTER INDEX policy_evaluation_scan_app_idx RENAME TO policy_evaluation_scan_owner_idx;
CREATE VIEW policy_evaluation AS
  SELECT policy_evaluation_id, owner_id, owner_id AS application_id, stage_type_id, scan_id,
         reevaluation, for_monitoring, for_obsolete_scan, time, commit_hash, initiator,
         scan_trigger_type, client_scan_type, branch_name, scm_repository_url,
         commit_hash_source, branch_name_source, scm_repository_url_source
  FROM policy_evaluation_t;

-- Pattern B: policy_violation column rename
ALTER TABLE policy_violation RENAME TO policy_violation_t;
ALTER TABLE policy_violation_t RENAME COLUMN application_id TO owner_id;
ALTER TABLE policy_violation_t DROP CONSTRAINT policy_violation_app_fk;
ALTER INDEX policy_violation_app_fix_time_stage_idx RENAME TO policy_violation_owner_fix_time_stage_idx;
ALTER INDEX policy_violation_policy_app_idx RENAME TO policy_violation_policy_owner_idx;
CREATE VIEW policy_violation AS
  SELECT policy_violation_id, owner_id, owner_id AS application_id, stage_type_id, policy_id,
         policy_name, threat_level, threat_category, hash, component_id_format,
         component_id_coordinates_json, filename, constraint_facts_json, action_type_id,
         open_time, waive_time, legacy_violation_time, fix_time, policy_waiver_id,
         policy_waiver_comment, seen_by_primary_evaluation, seen_by_monitoring_evaluation,
         legacy_violation_applied, reachability_status, auto_policy_waiver_id, constraint_facts_id,
         is_remediated_by_version_change, last_telemetry_emitted_date
  FROM policy_violation_t;

-- Pattern B: last_policy_evaluation column rename
ALTER TABLE last_policy_evaluation RENAME TO last_policy_evaluation_t;
ALTER TABLE last_policy_evaluation_t RENAME COLUMN application_id TO owner_id;
ALTER TABLE last_policy_evaluation_t DROP CONSTRAINT last_policy_evaluation_app_fk;
CREATE VIEW last_policy_evaluation AS
  SELECT policy_evaluation_id, owner_id, owner_id AS application_id, stage_type_id
  FROM last_policy_evaluation_t;
