-- since 1.189
-- SaaS Compatible
BEGIN;
ALTER TABLE policy_violation DROP CONSTRAINT IF EXISTS policy_violation_app_fk;
ALTER TABLE policy_violation ADD CONSTRAINT policy_violation_app_fk FOREIGN KEY (application_id) REFERENCES application (application_id) ON DELETE CASCADE;
COMMIT;
