-- since 1.172
-- SaaS Compatible
CREATE TABLE sast_scm_scan_context
(
  sast_scm_scan_context_id                    varchar(50) NOT NULL,
  branch_name                                 varchar(512) NOT NULL,
  commit_hash                                 varchar(128) NOT NULL,
  created_at                                  timestamp NOT NULL,
  CONSTRAINT sast_scm_scan_context_pk PRIMARY KEY (sast_scm_scan_context_id)
);

CREATE INDEX sast_scm_scan_context_branch_name_idx ON sast_scm_scan_context (branch_name);
CREATE INDEX sast_scm_scan_context_commit_hash_idx ON sast_scm_scan_context (commit_hash);

ALTER TABLE sast_scan ADD COLUMN sast_scm_scan_context_id varchar(50) NULL;

ALTER TABLE sast_scan
  ADD CONSTRAINT sast_scm_scan_context_fk FOREIGN KEY (sast_scm_scan_context_id)
    REFERENCES sast_scm_scan_context(sast_scm_scan_context_id) ON DELETE SET NULL;

CREATE INDEX sast_scan_sast_scm_scan_context_id_idx ON sast_scan (sast_scm_scan_context_id);
