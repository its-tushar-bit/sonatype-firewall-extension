-- since 1.197.0
-- SaaS Compatible
CREATE INDEX IF NOT EXISTS vulnerability_exploitability_coord_security_id_ref_id_idx ON vulnerability_exploitability (coordinate_security_id, ref_id);
