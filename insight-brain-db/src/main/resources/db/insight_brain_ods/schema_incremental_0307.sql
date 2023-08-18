-- Since 1.166
-- SaaS Compatible
ALTER TABLE repository_manager ADD COLUMN product_name varchar(50) NULL;
ALTER TABLE repository_manager ADD COLUMN product_version varchar(100) NULL;