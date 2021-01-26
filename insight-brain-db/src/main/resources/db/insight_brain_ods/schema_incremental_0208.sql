-- Since 1.105
ALTER TABLE component_copyright ADD COLUMN last_updated_by_username varchar(256) NOT NULL;
ALTER TABLE component_copyright ADD COLUMN last_updated_at timestamp NOT NULL;
ALTER TABLE component_legal_file ADD COLUMN last_updated_by_username varchar(256) NOT NULL;
ALTER TABLE component_legal_file ADD COLUMN last_updated_at timestamp NOT NULL;
ALTER TABLE component_obligation ADD COLUMN last_updated_by_username varchar(256) NOT NULL;
ALTER TABLE component_obligation ADD COLUMN last_updated_at timestamp NOT NULL;
ALTER TABLE component_obligation_attribution ADD COLUMN last_updated_by_username varchar(256) NOT NULL;
ALTER TABLE component_obligation_attribution ADD COLUMN last_updated_at timestamp NOT NULL;
