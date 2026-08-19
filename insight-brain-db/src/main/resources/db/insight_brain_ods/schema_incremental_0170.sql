-- Since 1.91
ALTER TABLE source_control_pull_request_comment
    ADD COLUMN content_hash varchar(40);
