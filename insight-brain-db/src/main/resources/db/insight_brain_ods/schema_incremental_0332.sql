-- since 1.174
-- SaaS Compatible

ALTER TABLE sast_pull_request_comment ADD COLUMN pull_request_comment_version int DEFAULT 0 NOT NULL;
