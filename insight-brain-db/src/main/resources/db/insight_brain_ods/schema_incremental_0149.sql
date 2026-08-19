-- Since 1.80.0
ALTER TABLE dashboard_filter ADD COLUMN realm_id varchar(50) NULL;

ALTER TABLE dashboard_filter ADD COLUMN username_lowercase varchar(60) NULL;
UPDATE dashboard_filter SET username_lowercase=LOWER(username);
ALTER TABLE dashboard_filter ALTER COLUMN username_lowercase SET NOT NULL;

ALTER TABLE dashboard_filter DROP CONSTRAINT dashboard_filter_uk;
ALTER TABLE dashboard_filter ADD CONSTRAINT dashboard_filter_uk UNIQUE (username_lowercase, realm_id, name_lowercase_no_whitespace);
