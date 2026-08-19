-- SaaS Compatible
-- CLM-43709: Drop hosted-only columns from proxy_repository_component.
-- These columns were only populated by hosted-repo scanning; Firewall never read them.
-- After CLM-43711 (writer removal) and CLM-45067 (read-path port to HRC / policy_evaluation)
-- these columns have no remaining readers or writers.

DROP INDEX IF EXISTS proxy_repository_component_scan_id_idx;
ALTER TABLE proxy_repository_component DROP COLUMN IF EXISTS last_evaluation_stage;
ALTER TABLE proxy_repository_component DROP COLUMN IF EXISTS scan_id;
ALTER TABLE proxy_repository_component DROP COLUMN IF EXISTS component_count;
