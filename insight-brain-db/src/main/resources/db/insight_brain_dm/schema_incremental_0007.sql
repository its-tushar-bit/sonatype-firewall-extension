-- Since 1.18.0
SET SCHEMA insight_brain_dm;

ALTER TABLE license
  DROP COLUMN license_category_id;
DROP TABLE license_category;
