-- since 1.174
-- SaaS compatible
-- CLM-29597 - Step 2 of renaming applicationVersion column to sbomVersion:
--  Drop application_version constraint, add sbomVersion and remove applicationVersion entity attributes

ALTER TABLE sbom_metadata
    DROP CONSTRAINT IF EXISTS sbom_metadata_application_version_uk;

ALTER TABLE sbom_metadata
    ALTER COLUMN application_version DROP NOT NULL;
