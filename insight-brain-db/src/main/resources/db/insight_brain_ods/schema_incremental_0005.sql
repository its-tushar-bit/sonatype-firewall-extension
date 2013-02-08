SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ALTER COLUMN public_id varchar(60) NOT NULL;

ALTER TABLE application
  ADD COLUMN public_id_lowercase varchar(60) NULL;
  
UPDATE application SET public_id_lowercase=LOWER(public_id);

ALTER TABLE application
  ALTER COLUMN public_id_lowercase varchar(60) NOT NULL;
