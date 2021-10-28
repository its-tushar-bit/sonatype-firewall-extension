-- Since 1.126
-- Clear out all entries already in the table before adding fk to be safe
-- There should not be any entries since this table is for an in development feature
TRUNCATE TABLE quarantined_component_access;

ALTER TABLE quarantined_component_access
  ADD COLUMN repository_id varchar(50) NOT NULL;
ALTER TABLE quarantined_component_access
  ADD CONSTRAINT quarantined_component_access_repository_fk FOREIGN KEY (repository_id) REFERENCES repository (repository_id);
ALTER TABLE quarantined_component_access
  ADD CONSTRAINT quarantined_component_access_repository_component_fk FOREIGN KEY (repository_component_id) REFERENCES repository_component (repository_component_id);

CREATE
INDEX quarantined_component_access_repository_id_idx ON quarantined_component_access(repository_id);
CREATE
INDEX quarantined_component_access_repository_component_id_idx ON quarantined_component_access(repository_component_id);
