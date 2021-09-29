-- Since 1.125
ALTER TABLE source_control_pull_request_comment ADD COLUMN pathname varchar(1000) NULL;

-- The "source_control_pull_request_comment_uk" constraint was named incorrectly "source_control_pull_request_uk"
-- in schema_incremental_0171. So we have to try both names to drop this constraint. 
ALTER TABLE source_control_pull_request_comment DROP CONSTRAINT IF EXISTS source_control_pull_request_comment_uk;
ALTER TABLE source_control_pull_request_comment DROP CONSTRAINT IF EXISTS source_control_pull_request_uk;
ALTER TABLE source_control_pull_request_comment ADD CONSTRAINT source_control_pull_request_comment_uk UNIQUE (application_id, component_hash, pull_request_id, pathname);
