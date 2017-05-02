-- Since 1.29.0
SET SCHEMA insight_brain_ods;

ALTER TABLE dashboard_filter
  ADD COLUMN acknowledged boolean DEFAULT false NOT NULL;
