-- since 1.189
-- SaaS Compatible

ALTER TABLE auto_policy_waiver ADD COLUMN scopes_operator_any BOOLEAN NOT NULL DEFAULT true;
