-- Since 1.160
ALTER TABLE repository_manager ADD COLUMN configured boolean DEFAULT true NOT NULL;
ALTER TABLE repository_manager ADD COLUMN configure_time timestamp NULL;
