-- Update SAST finding table to do cascading delete on the scan-id

-- SaaS Compatible

BEGIN;
ALTER TABLE sast_finding DROP CONSTRAINT IF EXISTS sast_finding_sast_scan_fk;
ALTER TABLE sast_finding ADD CONSTRAINT sast_finding_sast_scan_fk FOREIGN KEY (sast_scan_id) REFERENCES sast_scan (sast_scan_id) ON DELETE CASCADE;
COMMIT;
