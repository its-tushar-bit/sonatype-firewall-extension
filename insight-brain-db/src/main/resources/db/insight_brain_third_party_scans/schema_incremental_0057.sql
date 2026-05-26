-- SaaS Compatible
-- CLM-38556: persist HDS-supplied vulnerability ids on coordinate_security so SBOM
-- exports can emit a <reference> per id without re-calling HDS at export time. URLs
-- are derived from each id at emission via VulnerabilityUrlBuilder, not stored.
ALTER TABLE coordinate_security ADD COLUMN IF NOT EXISTS vuln_ids TEXT;
