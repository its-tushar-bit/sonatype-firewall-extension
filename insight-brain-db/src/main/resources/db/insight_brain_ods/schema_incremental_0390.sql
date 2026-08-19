-- since 1.192
-- SaaS Compatible

ALTER TABLE repository_container ADD COLUMN related_organization_id varchar(50);
ALTER TABLE repository_manager ADD COLUMN base_url varchar(2048);
ALTER TABLE repository_manager ADD COLUMN related_organization_id varchar(50);
ALTER TABLE organization ADD COLUMN related_repository_manager_id varchar(50);
ALTER TABLE organization ADD COLUMN related_repository_id varchar(50);
ALTER TABLE repository ADD COLUMN related_organization_id varchar(50);

ALTER TABLE repository_container
ADD CONSTRAINT repository_container_related_organization_fk
FOREIGN KEY (related_organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL;

ALTER TABLE repository_manager
ADD CONSTRAINT repository_manager_related_organization_fk
FOREIGN KEY (related_organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL;

ALTER TABLE repository
ADD CONSTRAINT repository_related_organization_fk
FOREIGN KEY (related_organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL;
