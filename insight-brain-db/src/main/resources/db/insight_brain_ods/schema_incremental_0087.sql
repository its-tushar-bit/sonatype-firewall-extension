-- Since 1.17.0
ALTER TABLE application
  ALTER COLUMN public_id varchar(200) NOT NULL;

ALTER TABLE application
  ALTER COLUMN public_id_lowercase varchar(200) NOT NULL;
