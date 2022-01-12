-- Since 1.132
ALTER TABLE organization ADD COLUMN repository_connection_enabled boolean DEFAULT true NOT NULL;
ALTER TABLE organization ADD COLUMN allow_repository_connection_override boolean DEFAULT true NOT NULL;

ALTER TABLE application ADD COLUMN repository_connection_enabled boolean DEFAULT true NOT NULL;
