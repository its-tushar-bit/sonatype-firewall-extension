-- Since 1.193
-- SaaS Compatible

ALTER TABLE source_control ADD COLUMN IF NOT EXISTS inner_source_automated_updates_enabled boolean;
