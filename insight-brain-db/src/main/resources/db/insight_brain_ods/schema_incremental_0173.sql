-- Since 1.94
ALTER TABLE source_control_pull_request_comment
    ADD COLUMN   pull_request_comment_version int default NULL;
