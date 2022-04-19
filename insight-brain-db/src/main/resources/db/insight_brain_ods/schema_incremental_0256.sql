-- Since 1.137
ALTER TABLE organization ADD COLUMN artifactory_connection_enabled boolean;
ALTER TABLE organization ADD COLUMN allow_artifactory_connection_override boolean DEFAULT true NOT NULL;

ALTER TABLE application ADD COLUMN artifactory_connection_enabled boolean;
