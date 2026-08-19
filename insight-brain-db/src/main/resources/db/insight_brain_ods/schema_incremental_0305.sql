-- Since 1.165
-- SaaS Compatible
ALTER TABLE organization ALTER COLUMN repository_connection_enabled DROP DEFAULT;
ALTER TABLE application ALTER COLUMN repository_connection_enabled DROP DEFAULT;
