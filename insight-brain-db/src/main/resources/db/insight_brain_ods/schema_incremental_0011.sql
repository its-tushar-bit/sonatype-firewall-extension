SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ADD COLUMN application_profile_id varchar(50) NULL;
  
ALTER TABLE application
  ADD CONSTRAINT application_application_profile_fk FOREIGN KEY (application_profile_id) REFERENCES application_profile(application_profile_id);

UPDATE application SET application_profile_id = 'default_application_profile';

ALTER TABLE application
  ALTER COLUMN application_profile_id varchar(50) NOT NULL;
