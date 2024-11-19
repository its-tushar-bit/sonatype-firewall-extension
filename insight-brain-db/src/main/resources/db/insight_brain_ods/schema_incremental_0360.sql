-- Since 1.184
-- SaaS Compatible
ALTER TABLE auto_policy_waiver_revocation ALTER COLUMN hash DROP NOT NULL;
ALTER TABLE auto_policy_waiver_revocation ADD COLUMN component_match_strategy VARCHAR(30) NULL;
ALTER TABLE auto_policy_waiver_revocation ADD COLUMN policy_violation_id VARCHAR(50) NULL;
ALTER TABLE auto_policy_waiver_revocation ADD COLUMN threat_level SMALLINT NULL;
ALTER TABLE auto_policy_waiver_revocation ADD COLUMN policy_name VARCHAR(60) NULL;
ALTER TABLE auto_policy_waiver_revocation ADD COLUMN component_display_name VARCHAR(200) NULL;
ALTER TABLE auto_policy_waiver_revocation ADD COLUMN vulnerability_identifiers VARCHAR(200) NULL;
