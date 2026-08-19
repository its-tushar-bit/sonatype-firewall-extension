-- SaaS Compatible
-- CLM-XXXXX: Add source column to policy_waiver_request to record where a waiver request
-- originated (repository-manager proxy path vs browser extension).
--
-- SaaS safety: adding a nullable column with no default is a safe single-deployment
-- operation. There is deliberately no backfill UPDATE -- bulk DML is what exceeds the ECS
-- health-check grace period. Legacy rows stay NULL and are read as FIREWALL_PROXY by
-- PolicyWaiverRequest.getSource().
ALTER TABLE policy_waiver_request ADD COLUMN IF NOT EXISTS source varchar(32);
