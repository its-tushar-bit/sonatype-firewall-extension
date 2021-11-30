-- Since 1.130
ALTER TABLE repository_connection ADD COLUMN format varchar(50) NOT NULL DEFAULT 'GENERIC';
ALTER TABLE repository_connection DROP CONSTRAINT repository_connection_url_uk;
ALTER TABLE repository_connection ADD CONSTRAINT repository_connection_url_uk UNIQUE (owner_id, format);
