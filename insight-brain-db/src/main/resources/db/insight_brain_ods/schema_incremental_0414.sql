-- Update application component to do cascading delete on the application-id

-- SaaS Compatible

BEGIN;
ALTER TABLE aggregate_file DROP CONSTRAINT IF EXISTS aggregate_file_application_component_fk;
ALTER TABLE aggregate_file ADD CONSTRAINT aggregate_file_application_component_fk FOREIGN KEY (application_component_id) REFERENCES application_component(application_component_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE application_component_license DROP CONSTRAINT IF EXISTS application_component_license_application_component_fk;
ALTER TABLE application_component_license ADD CONSTRAINT application_component_license_application_component_fk FOREIGN KEY (application_component_id) REFERENCES application_component(application_component_id) ON DELETE CASCADE;
COMMIT;

BEGIN;
ALTER TABLE application_component DROP CONSTRAINT IF EXISTS application_component_application_fk;
ALTER TABLE application_component ADD CONSTRAINT application_component_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE;
COMMIT;
