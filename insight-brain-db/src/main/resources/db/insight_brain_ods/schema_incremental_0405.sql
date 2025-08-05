-- Add ON DELETE CASCADE to application_tag table foreign key constraint for application_id

-- SaaS Compatible

ALTER TABLE application_tag DROP CONSTRAINT application_tag_app_fk;
ALTER TABLE application_tag ADD CONSTRAINT application_tag_app_fk FOREIGN KEY (application_id) REFERENCES application (application_id) ON DELETE CASCADE;

