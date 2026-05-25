-- SaaS Compatible
-- CLM-38736: Add SCM metadata and source tracking columns to policy_evaluation table
-- Purpose: Store SCM repository URL and provenance information for Apiiro integration

-- VARCHAR(2048) for URL: Git URLs can include embedded authentication tokens
-- or long path segments, especially for self-hosted Git servers.

-- Source columns: VARCHAR(50) to store enum values with room for future expansion

ALTER TABLE policy_evaluation ADD COLUMN IF NOT EXISTS scm_repository_url VARCHAR(2048);
ALTER TABLE policy_evaluation ADD COLUMN IF NOT EXISTS commit_hash_source VARCHAR(50);
ALTER TABLE policy_evaluation ADD COLUMN IF NOT EXISTS branch_name_source VARCHAR(50);
ALTER TABLE policy_evaluation ADD COLUMN IF NOT EXISTS scm_repository_url_source VARCHAR(50);
