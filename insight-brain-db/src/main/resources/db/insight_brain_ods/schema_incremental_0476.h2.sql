-- SaaS Compatible
-- CLM-42788: Firewall rename — H2 variant (on-prem).
--
-- On-prem has no rolling deploy — migrations run in a single maintenance window with the
-- application stopped — so no compat view or _t shim is needed. Plain in-place renames.
-- Companion to schema_incremental_0476.pg.sql (Postgres side, which uses the compat-view
-- shim per §4 of the design doc).

------------------------------------------------------------------------
-- repository_component → proxy_repository_component (table + PK column + constraints + indexes)
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
ALTER INDEX repository_component_dedup_keyset_idx             RENAME TO proxy_repository_component_dedup_keyset_idx;

------------------------------------------------------------------------
-- repository_policy_violation → proxy_repository_policy_violation
------------------------------------------------------------------------

ALTER TABLE repository_policy_violation RENAME TO proxy_repository_policy_violation;
ALTER TABLE proxy_repository_policy_violation RENAME COLUMN repository_policy_violation_id TO proxy_repository_policy_violation_id;

ALTER TABLE proxy_repository_policy_violation RENAME CONSTRAINT repository_policy_violation_pk                     TO proxy_repository_policy_violation_pk;
ALTER TABLE proxy_repository_policy_violation RENAME CONSTRAINT repository_policy_violation_repository_fk          TO proxy_repository_policy_violation_repository_fk;
ALTER TABLE proxy_repository_policy_violation RENAME CONSTRAINT repository_policy_violation_constraint_facts_id_fk TO proxy_repository_policy_violation_constraint_facts_id_fk;

ALTER INDEX repository_policy_violation_pathname_idx      RENAME TO proxy_repository_policy_violation_pathname_idx;
ALTER INDEX repository_policy_violation_repository_id_idx RENAME TO proxy_repository_policy_violation_repository_id_idx;

------------------------------------------------------------------------
-- reevaluate_cascade_progress.repository_component_id → proxy_repository_component_id
-- (Column rename only; table keeps its name. No _t / no view on H2.)
------------------------------------------------------------------------

ALTER TABLE reevaluate_cascade_progress RENAME COLUMN repository_component_id TO proxy_repository_component_id;
ALTER TABLE reevaluate_cascade_progress RENAME CONSTRAINT repository_component_id_fk TO proxy_repository_component_id_fk;

------------------------------------------------------------------------
-- quarantined_component_access.repository_component_id → proxy_repository_component_id
-- (Column rename only; table keeps its name. No _t / no view on H2.)
------------------------------------------------------------------------

ALTER TABLE quarantined_component_access RENAME COLUMN repository_component_id TO proxy_repository_component_id;
ALTER TABLE quarantined_component_access RENAME CONSTRAINT quarantined_component_access_repository_component_fk TO quarantined_component_access_proxy_repository_component_fk;
ALTER INDEX quarantined_component_access_repository_component_id_idx RENAME TO quarantined_component_access_proxy_repository_component_id_idx;
