-- Since 1.133
ALTER TABLE organization ALTER COLUMN repository_connection_enabled DROP NOT NULL;

ALTER TABLE application ALTER COLUMN repository_connection_enabled DROP NOT NULL;
