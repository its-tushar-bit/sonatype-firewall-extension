-- since 1.181
-- SaaS compatible

BEGIN;

UPDATE sbom_metadata
SET scan_type = 'SBOM'
WHERE scan_type IS NULL;

ALTER TABLE sbom_metadata
    ALTER COLUMN scan_type SET NOT NULL;

COMMIT;
