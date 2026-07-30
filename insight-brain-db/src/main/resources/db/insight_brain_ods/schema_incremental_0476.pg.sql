-- SaaS Compatible
-- CLM-42788: Firewall rename — repository_component / repository_policy_violation → proxy_*.
--
-- This is the Release-N Postgres shim for the two-release rename cadence defined in the
-- Hosted-Repository Scanning: Target Architecture doc, §4 (Release sequencing and database
-- migration). The final target names are in schema.sql; this incremental brings existing
-- MTIQ tenants to the final target while keeping draining old pods functional through the
-- rolling deploy window.
--
-- Two rename shapes:
--   Pattern A — table name changes. Rename the real table + columns + constraints + indexes
--     to the final names, then create an auto-updatable compat VIEW under the old name that
--     aliases the new PK column back to its old name. Draining old pods hit the view; new
--     pods hit the real table directly.
--   Pattern B — table name stays, only a column renames. The real table takes a temporary
--     _t name for the N window because the final name is occupied by the compat view. The
--     view exposes BOTH column names so old (application_id-style) and new (owner_id-style)
--     writes both work on the same table name concurrently.
--
-- Postgres FKs bind by OID (not name), so all inbound FKs auto-follow the parent-table
-- rename. No FK is dropped or recreated for the rename.
--
-- Release-N+1 (out of scope here): DROP VIEW for Pattern A; relkind-guarded rename-back of
-- the _t tables for Pattern B. Tenants provisioned fresh during N already land on the final
-- schema (via schema.sql) and have no view / _t to collapse.

------------------------------------------------------------------------
-- Pattern A: repository_component → proxy_repository_component
------------------------------------------------------------------------

ALTER TABLE repository_component RENAME TO proxy_repository_component;
ALTER TABLE proxy_repository_component RENAME COLUMN repository_component_id TO proxy_repository_component_id;

ALTER TABLE proxy_repository_component RENAME CONSTRAINT repository_component_pk             TO proxy_repository_component_pk;
ALTER TABLE proxy_repository_component RENAME CONSTRAINT repository_component_repository_fk  TO proxy_repository_component_repository_fk;
ALTER TABLE proxy_repository_component RENAME CONSTRAINT repository_component_uk             TO proxy_repository_component_uk;

ALTER INDEX repository_component_hash_idx                     RENAME TO proxy_repository_component_hash_idx;
ALTER INDEX repository_component_repository_unquarantine_idx  RENAME TO proxy_repository_component_repository_unquarantine_idx;
ALTER INDEX repository_component_quarantine_idx               RENAME TO proxy_repository_component_quarantine_idx;
ALTER INDEX repository_component_release_quarantine_idx       RENAME TO proxy_repository_component_release_quarantine_idx;
ALTER INDEX repository_component_component_coordinates_idx    RENAME TO proxy_repository_component_component_coordinates_idx;
ALTER INDEX repository_component_scan_id_idx                  RENAME TO proxy_repository_component_scan_id_idx;
ALTER INDEX idx_repository_component_component_id             RENAME TO idx_proxy_repository_component_component_id;
ALTER INDEX repository_component_last_evaluation_time_idx     RENAME TO proxy_repository_component_last_evaluation_time_idx;
-- IF EXISTS: this index is created asynchronously by RepositoryComponentDedupKeysetIndexAsyncDbMigration,
-- so it may not exist yet at this point on paths that haven't run the async migrator.
ALTER INDEX IF EXISTS repository_component_dedup_keyset_idx    RENAME TO proxy_repository_component_dedup_keyset_idx;

CREATE VIEW repository_component AS
  SELECT proxy_repository_component_id AS repository_component_id,
         repository_id,
         pathname,
         time,
         hash,
         component_id_format,
         component_id_coordinates_json,
         display_name,
         match_state_id,
         identification_source_id,
         last_evaluation_time,
         quarantine_time,
         unquarantine_time,
         analyzer_features_json,
         auto_unquarantined,
         component_id,
         last_evaluation_stage,
         scan_id,
         component_count
    FROM proxy_repository_component;

------------------------------------------------------------------------
-- Pattern A: repository_policy_violation → proxy_repository_policy_violation
------------------------------------------------------------------------

ALTER TABLE repository_policy_violation RENAME TO proxy_repository_policy_violation;
ALTER TABLE proxy_repository_policy_violation RENAME COLUMN repository_policy_violation_id TO proxy_repository_policy_violation_id;

ALTER TABLE proxy_repository_policy_violation RENAME CONSTRAINT repository_policy_violation_pk                       TO proxy_repository_policy_violation_pk;
ALTER TABLE proxy_repository_policy_violation RENAME CONSTRAINT repository_policy_violation_repository_fk            TO proxy_repository_policy_violation_repository_fk;
ALTER TABLE proxy_repository_policy_violation RENAME CONSTRAINT repository_policy_violation_constraint_facts_id_fk   TO proxy_repository_policy_violation_constraint_facts_id_fk;

ALTER INDEX repository_policy_violation_pathname_idx      RENAME TO proxy_repository_policy_violation_pathname_idx;
ALTER INDEX repository_policy_violation_repository_id_idx RENAME TO proxy_repository_policy_violation_repository_id_idx;

CREATE VIEW repository_policy_violation AS
  SELECT proxy_repository_policy_violation_id AS repository_policy_violation_id,
         repository_id,
         pathname,
         time,
         policy_id,
         policy_name,
         threat_level,
         threat_category,
         hash,
         component_id_format,
         component_id_coordinates_json,
         constraint_facts_json,
         action_type_id,
         waived,
         active,
         policy_waiver_id,
         policy_waiver_comment,
         waive_time,
         constraint_facts_id,
         component_id
    FROM proxy_repository_policy_violation;

------------------------------------------------------------------------
-- Pattern B: reevaluate_cascade_progress.repository_component_id → proxy_repository_component_id
--
-- Table keeps its final name; real table takes _t during the N window because the final
-- name is occupied by the compat view. The view exposes both column names so old pods
-- (writing repository_component_id) and new pods (writing proxy_repository_component_id)
-- can co-exist on the unchanged table name.
------------------------------------------------------------------------

ALTER TABLE reevaluate_cascade_progress RENAME TO reevaluate_cascade_progress_t;
ALTER TABLE reevaluate_cascade_progress_t RENAME COLUMN repository_component_id TO proxy_repository_component_id;
ALTER TABLE reevaluate_cascade_progress_t RENAME CONSTRAINT repository_component_id_fk TO proxy_repository_component_id_fk;

CREATE VIEW reevaluate_cascade_progress AS
  SELECT reevaluate_cascade_progress_id,
         reevaluate_cascade_request_id,
         repository_id,
         proxy_repository_component_id,
         proxy_repository_component_id AS repository_component_id,
         quarantined,
         status
    FROM reevaluate_cascade_progress_t;

------------------------------------------------------------------------
-- Pattern B: quarantined_component_access.repository_component_id → proxy_repository_component_id
--
-- Scope expansion vs. ticket AC: quarantined_component_access is a second FK-child into
-- Firewall's proxy_repository_component (added in v1.125, after the design doc was
-- drafted). Treated with Pattern B for consistency with the rest of the Firewall FK
-- cluster and to keep the schema internally consistent (child column name matches parent
-- PK column name). See docs/plans/2026-07-23-CLM-42788-firewall-rename.md for context.
------------------------------------------------------------------------

ALTER TABLE quarantined_component_access RENAME TO quarantined_component_access_t;
ALTER TABLE quarantined_component_access_t RENAME COLUMN repository_component_id TO proxy_repository_component_id;
ALTER TABLE quarantined_component_access_t RENAME CONSTRAINT quarantined_component_access_repository_component_fk TO quarantined_component_access_proxy_repository_component_fk;
ALTER INDEX quarantined_component_access_repository_component_id_idx RENAME TO quarantined_component_access_proxy_repository_component_id_idx;

CREATE VIEW quarantined_component_access AS
  SELECT quarantined_component_access_id,
         repository_id,
         proxy_repository_component_id,
         proxy_repository_component_id AS repository_component_id,
         generate_time
    FROM quarantined_component_access_t;
