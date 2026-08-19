-- Since 1.24.0
ALTER TABLE dashboard_filter ADD COLUMN name varchar(60);
ALTER TABLE dashboard_filter ADD COLUMN name_lowercase_no_whitespace varchar(60);

UPDATE dashboard_filter SET name = '';
UPDATE dashboard_filter SET name_lowercase_no_whitespace = '';

ALTER TABLE dashboard_filter ALTER COLUMN name SET NOT NULL;
ALTER TABLE dashboard_filter ALTER COLUMN name_lowercase_no_whitespace SET NOT NULL;

ALTER TABLE dashboard_filter DROP CONSTRAINT dashboard_filter_uk;
ALTER TABLE dashboard_filter ADD CONSTRAINT dashboard_filter_uk UNIQUE KEY (username, name_lowercase_no_whitespace);
