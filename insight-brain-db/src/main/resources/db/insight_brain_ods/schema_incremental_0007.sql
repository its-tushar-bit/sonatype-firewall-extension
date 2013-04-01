SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ADD COLUMN name VARCHAR(60) NULL;

ALTER TABLE application
  ADD COLUMN name_lowercase_no_whitespace VARCHAR(60) NULL;

UPDATE application SET name = public_id;

UPDATE application SET name_lowercase_no_whitespace = REPLACE(public_id_lowercase, ' ', '')

ALTER TABLE application
  ALTER COLUMN name varchar(60) NOT NULL;

ALTER TABLE application
  ALTER COLUMN name_lowercase_no_whitespace varchar(60) NOT NULL;

