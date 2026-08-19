-- Since 1.185
-- SaaS Compatible
ALTER TABLE policy_waiver ADD COLUMN expire_when_remediation_available boolean DEFAULT false;
