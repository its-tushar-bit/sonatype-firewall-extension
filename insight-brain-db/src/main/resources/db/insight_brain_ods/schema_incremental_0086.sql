-- Since 1.17.0
ALTER TABLE organization ADD COLUMN parent_organization_id varchar(50) NULL;
ALTER TABLE organization ADD CONSTRAINT organization_parent_organization_fk FOREIGN KEY (parent_organization_id) REFERENCES organization(organization_id);

INSERT INTO organization (organization_id, parent_organization_id, name, name_lowercase_no_whitespace) VALUES('ROOT_ORGANIZATION_ID', null, 'Root Organization', 'rootorganization');
UPDATE organization SET parent_organization_id='ROOT_ORGANIZATION_ID' WHERE organization_id!='ROOT_ORGANIZATION_ID';
