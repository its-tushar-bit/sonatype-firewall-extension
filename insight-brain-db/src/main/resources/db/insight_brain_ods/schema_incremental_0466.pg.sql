-- SaaS Compatible
-- CLM-39800: Track per-app InnerSource cleanup pending on next new scan after HDS bug fix.

CREATE TABLE IF NOT EXISTS innersource_cleanup_pending (
  application_id varchar(50) NOT NULL,
  last_scan_id varchar(50),
  CONSTRAINT innersource_cleanup_pending_pk PRIMARY KEY (application_id),
  CONSTRAINT innersource_cleanup_pending_app_fk FOREIGN KEY (application_id)
      REFERENCES application (application_id) ON DELETE CASCADE
);

INSERT INTO innersource_cleanup_pending (application_id, last_scan_id)
SELECT DISTINCT isa.application_id,
       (SELECT pe.scan_id
        FROM policy_evaluation pe
        JOIN last_policy_evaluation lpe ON pe.policy_evaluation_id = lpe.policy_evaluation_id
        WHERE lpe.application_id = isa.application_id
        ORDER BY pe.time DESC
        LIMIT 1)
FROM inner_source_application isa
ON CONFLICT DO NOTHING;
