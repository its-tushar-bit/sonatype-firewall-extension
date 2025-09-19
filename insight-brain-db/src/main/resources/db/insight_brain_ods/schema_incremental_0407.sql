-- Update SAST remediation table to do cascading delete on the the finding-id

-- SaaS Compatible

BEGIN;
ALTER TABLE sast_remediation DROP CONSTRAINT IF EXISTS sast_remediation_sast_finding_fk;
ALTER TABLE sast_remediation ADD CONSTRAINT sast_remediation_sast_finding_fk FOREIGN KEY (sast_finding_id) REFERENCES sast_finding (sast_finding_id) ON DELETE CASCADE;
COMMIT;
