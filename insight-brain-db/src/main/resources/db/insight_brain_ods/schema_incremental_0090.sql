-- Since 1.17.0
ALTER TABLE repository_component ADD COLUMN pathname varchar(1000) NOT NULL;
ALTER TABLE repository_component DROP CONSTRAINT repository_component_uk;
ALTER TABLE repository_component ADD CONSTRAINT repository_component_uk UNIQUE KEY (repository_id, pathname);

ALTER TABLE repository_policy_violation ADD COLUMN pathname varchar(1000) NOT NULL;
