-- Since 1.182
-- SaaS Compatible
BEGIN;
    ALTER TABLE policy_monitoring DROP CONSTRAINT IF EXISTS policy_monitoring_uk;
    ALTER TABLE policy_monitoring ADD CONSTRAINT policy_monitoring_uk UNIQUE (owner_id, stage_type_id);
COMMIT;
