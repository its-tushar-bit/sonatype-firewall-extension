-- Since 1.29.0
ALTER TABLE dashboard_filter
  ADD COLUMN acknowledged boolean DEFAULT false NOT NULL;
