-- Since 1.204
-- SaaS Compatible
-- CLM-39917: Add scan_id and component_count columns to repository_component for hosted repository report navigation
-- Note: scan_id and component_count are intentionally NULL for pre-existing rows. They are populated only when
-- a hosted component is (re-)scanned after this migration. The UI hides Report/Priorities navigation for NULL scan_id.
ALTER TABLE repository_component ADD COLUMN IF NOT EXISTS scan_id varchar(50);
CREATE INDEX IF NOT EXISTS repository_component_scan_id_idx ON repository_component(scan_id);
ALTER TABLE repository_component ADD COLUMN IF NOT EXISTS component_count integer;
