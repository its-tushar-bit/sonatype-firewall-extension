-- Since 1.125
ALTER TABLE source_control ADD COLUMN repository_ssh_url text NULL;
ALTER TABLE source_control ADD COLUMN ssh_enabled boolean DEFAULT false;
