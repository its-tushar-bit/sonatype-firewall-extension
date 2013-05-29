SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ADD COLUMN organization_id varchar(50) NULL;
ALTER TABLE application
  ADD CONSTRAINT application_organization_fk FOREIGN KEY (organization_id) REFERENCES organization(organization_id);
