-- Since 1.155
-- SaaS Compatible
ALTER TABLE source_control
  ADD COLUMN non_golden_pull_requests_enabled BOOLEAN;