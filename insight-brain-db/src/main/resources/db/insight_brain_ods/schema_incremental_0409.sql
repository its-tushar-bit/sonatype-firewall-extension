-- Update SAST pull request table to do cascading delete on the scan-id

-- SaaS Compatible

BEGIN;
ALTER TABLE sast_pull_request_comment DROP CONSTRAINT IF EXISTS sast_pull_request_comment_fk;
ALTER TABLE sast_pull_request_comment ADD CONSTRAINT sast_pull_request_comment_fk FOREIGN KEY (sast_scan_id) REFERENCES sast_scan (sast_scan_id) ON DELETE CASCADE;
COMMIT;
