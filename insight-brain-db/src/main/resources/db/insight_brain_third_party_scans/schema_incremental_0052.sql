-- SaaS compatible
-- Since 1.193

ALTER TABLE coordinate_security ADD COLUMN IF NOT EXISTS research_type VARCHAR(50);
ALTER TABLE coordinate_security ADD COLUMN IF NOT EXISTS detection_type VARCHAR(50);
