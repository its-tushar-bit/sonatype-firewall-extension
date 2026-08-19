-- Since 1.166
-- SaaS Compatible

ALTER TABLE coordinate_license ALTER COLUMN license_id TYPE varchar(1000);
ALTER TABLE coordinate_license ALTER COLUMN name TYPE varchar(1000);
