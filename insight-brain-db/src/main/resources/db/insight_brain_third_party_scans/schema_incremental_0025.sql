-- since 1.175
-- SaaS compatible
-- CLM-29597 - Step 3 of renaming applicationVersion column to sbomVersion:
--  Drop application_version column, add not null and unique constraints to sbom_version

ALTER TABLE sbom_metadata
    DROP COLUMN application_version;

ALTER TABLE sbom_metadata
    ALTER COLUMN sbom_version SET NOT NULL;

ALTER TABLE sbom_metadata
    ADD CONSTRAINT sbom_metadata_sbom_version_uk UNIQUE (application_id, sbom_version);
