-- Since 1.184
-- SaaS Compatible
ALTER TABLE auto_policy_waiver_revocation ADD COLUMN policy_id VARCHAR(50) NULL;
ALTER TABLE auto_policy_waiver_revocation ADD COLUMN constraint_facts_json text NULL;
