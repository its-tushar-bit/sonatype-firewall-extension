-- SaaS Compatible
-- NEXUS-52485: Add manager_type to repository_manager and upstream_url to repository for IQ Proxy support

ALTER TABLE repository_manager ADD COLUMN IF NOT EXISTS manager_type VARCHAR(50) NULL;

ALTER TABLE repository ADD COLUMN IF NOT EXISTS upstream_url VARCHAR(2048) NULL;
