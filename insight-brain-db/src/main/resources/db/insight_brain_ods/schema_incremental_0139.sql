-- Since 1.72.0

-- provider can be null for applications
ALTER TABLE source_control ALTER COLUMN provider DROP NOT NULL;
-- token can be null for applications
ALTER TABLE source_control ALTER COLUMN token DROP NOT NULL;
