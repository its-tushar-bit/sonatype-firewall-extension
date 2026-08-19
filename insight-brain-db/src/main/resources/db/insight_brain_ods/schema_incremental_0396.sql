-- SaaS Compatible
-- Since 1.193
ALTER TABLE hash_component_identifier ADD COLUMN IF NOT EXISTS claimer_id varchar(60) NULL;
ALTER TABLE hash_component_identifier ADD COLUMN IF NOT EXISTS claimer_name varchar(210) NULL;
