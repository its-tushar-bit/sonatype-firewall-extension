-- Since 1.106
CREATE TABLE proprietary_component_name_pattern (
  proprietary_component_name_pattern_id varchar(50) NOT NULL,

  format varchar(50) NOT NULL,
  namespace_pattern varchar(200) NOT NULL,
  name_pattern varchar(300) NOT NULL,

  repository_manager_instance_id varchar(50) NOT NULL,
  repository_public_id varchar(500) NOT NULL,

  CONSTRAINT proprietary_component_name_pattern_pk PRIMARY KEY (proprietary_component_name_pattern_id),
  -- to make this uniqueness constraint work as desired, note that all included columns are not nullable, using empty strings instead if needed
  CONSTRAINT proprietary_component_name_pattern_uk UNIQUE (format, namespace_pattern, name_pattern, repository_manager_instance_id, repository_public_id)
);
CREATE INDEX proprietary_component_name_pattern_repo_idx ON proprietary_component_name_pattern(repository_manager_instance_id, repository_public_id);
