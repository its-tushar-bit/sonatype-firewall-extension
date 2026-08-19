-- Since 1.164
-- SaaS Compatible
ALTER TABLE repository_manager ADD name varchar(200) default NULL;
ALTER TABLE repository_manager ADD name_lowercase_no_whitespace varchar(200) default NULL;
ALTER TABLE repository_manager ADD CONSTRAINT repository_manager_name_uk UNIQUE (name_lowercase_no_whitespace);
