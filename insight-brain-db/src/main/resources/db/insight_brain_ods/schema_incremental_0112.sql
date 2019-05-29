-- Since 1.43.0
ALTER TABLE organization ALTER COLUMN name varchar(100) NOT NULL;
ALTER TABLE organization ALTER COLUMN name_lowercase_no_whitespace varchar(100) NOT NULL;
ALTER TABLE application ALTER COLUMN name varchar(100) NOT NULL;
ALTER TABLE application ALTER COLUMN name_lowercase_no_whitespace varchar(100) NOT NULL;
