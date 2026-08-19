-- Since 1.48.0
ALTER TABLE organization ALTER COLUMN name varchar(200) NOT NULL;
ALTER TABLE organization ALTER COLUMN name_lowercase_no_whitespace varchar(200) NOT NULL;
ALTER TABLE application ALTER COLUMN name varchar(200) NOT NULL;
ALTER TABLE application ALTER COLUMN name_lowercase_no_whitespace varchar(200) NOT NULL;
