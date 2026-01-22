-- Since 1.161
ALTER TABLE proprietary_component_name_pattern
  ADD CONSTRAINT proprietary_component_name_pattern_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id);

-- The column was added by the previous incremental script (as nullable)
ALTER TABLE proprietary_component_name_pattern
  ALTER COLUMN repository_id SET NOT NULL;

ALTER TABLE proprietary_component_name_pattern
  DROP CONSTRAINT proprietary_component_name_pattern_uk;
ALTER TABLE proprietary_component_name_pattern
  ADD CONSTRAINT proprietary_component_name_pattern_uk UNIQUE (format, namespace_pattern, name_pattern, repository_id);

DROP INDEX IF EXISTS proprietary_component_name_pattern_repo_idx;
CREATE INDEX proprietary_component_name_pattern_repo_idx ON proprietary_component_name_pattern(repository_id);
  
ALTER TABLE proprietary_component_name_pattern
  DROP COLUMN repository_manager_instance_id;
ALTER TABLE proprietary_component_name_pattern
  DROP COLUMN repository_public_id;
