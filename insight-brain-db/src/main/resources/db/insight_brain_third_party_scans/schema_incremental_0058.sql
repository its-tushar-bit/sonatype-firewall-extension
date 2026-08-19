-- SaaS Compatible
-- CLM-38729: increase coordinate_license.url from VARCHAR(200) to VARCHAR(1000)
-- to accommodate license URLs that exceed 200 characters

ALTER TABLE coordinate_license ALTER COLUMN url TYPE VARCHAR(1000);
