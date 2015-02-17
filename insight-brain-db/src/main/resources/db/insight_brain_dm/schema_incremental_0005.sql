-- Since 1.13.0
SET SCHEMA insight_brain_dm;

ALTER TABLE license DROP COLUMN description;
ALTER TABLE multi_license DROP COLUMN description;
