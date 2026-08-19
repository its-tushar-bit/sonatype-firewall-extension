-- Since 1.70.0
-- rename application_id to owner_id
ALTER TABLE source_control ADD COLUMN owner_id varchar(50) NULL;
UPDATE source_control SET owner_id = application_id;
ALTER TABLE source_control ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE source_control ADD CONSTRAINT source_control_owner_id_uk UNIQUE (owner_id);
ALTER TABLE source_control DROP COLUMN application_id;

-- repository will be nullable for organization
ALTER TABLE source_control ALTER COLUMN repository_url DROP NOT NULL;


