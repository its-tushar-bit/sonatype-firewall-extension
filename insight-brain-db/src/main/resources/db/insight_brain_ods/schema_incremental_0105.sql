-- Since 1.25.0
SET SCHEMA insight_brain_ods;

ALTER TABLE dashboard_filter ADD COLUMN based_on_filter_name varchar(60);
