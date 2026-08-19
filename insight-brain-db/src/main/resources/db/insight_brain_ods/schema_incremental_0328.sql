-- since 1.172
-- SaaS Compatible
CREATE TABLE sast_pull_request_comment
(
  sast_pull_request_comment_id                varchar(50) NOT NULL,
  sast_scan_id                                varchar(50) NOT NULL,
  pull_request_url                            varchar(1000) NOT NULL,
  created_at                                  timestamp NOT NULL,
  last_updated_at                             timestamp NOT NULL,
  commit_hash                                 varchar(128) NOT NULL,
  content_hash                                varchar(128) NOT NULL,
  pull_request_comment_id                     varchar(50) NOT NULL,

  CONSTRAINT sast_pull_request_comment_pk PRIMARY KEY (sast_pull_request_comment_id),
  CONSTRAINT sast_pull_request_comment_fk FOREIGN KEY (sast_scan_id) REFERENCES sast_scan(sast_scan_id),
  CONSTRAINT sast_pull_request_comment_sast_scan_id_uk UNIQUE (sast_scan_id),
  CONSTRAINT sast_pull_request_comment_pull_request_url_uk UNIQUE (pull_request_url)
);

CREATE INDEX sast_pull_request_comment_pull_request_url_idx ON sast_pull_request_comment (pull_request_url);
CREATE INDEX sast_pull_request_comment_sast_scan_id_idx ON sast_pull_request_comment (sast_scan_id);
