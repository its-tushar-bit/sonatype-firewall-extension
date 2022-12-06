-- Since 1.151
ALTER TABLE vulnerability_group ADD COLUMN name_lowercase_no_whitespace varchar(60) NOT NULL;
