-- Since 1.11
SET SCHEMA insight_brain_ods;

ALTER TABLE dashboard_filter
  ADD CONSTRAINT dashboard_filter_uk UNIQUE KEY (username);
