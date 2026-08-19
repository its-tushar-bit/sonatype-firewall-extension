-- Since 1.18.0
ALTER TABLE license
  DROP COLUMN license_category_id;
DROP TABLE license_category;
