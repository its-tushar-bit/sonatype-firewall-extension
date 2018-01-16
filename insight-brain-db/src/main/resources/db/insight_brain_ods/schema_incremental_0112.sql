-- Since 1.43.0
SET SCHEMA insight_brain_ods;

ALTER TABLE organization ALTER COLUMN name varchar(100) NOT NULL;
ALTER TABLE organization ALTER COLUMN name_lowercase_no_whitespace varchar(100) NOT NULL;
ALTER TABLE application ALTER COLUMN name varchar(100) NOT NULL;
ALTER TABLE application ALTER COLUMN name_lowercase_no_whitespace varchar(100) NOT NULL;
