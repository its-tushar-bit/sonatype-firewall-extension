-- since 1.174
-- SaaS compatible

ALTER TABLE sbom_metadata
    DROP CONSTRAINT sbom_metadata_uk;

ALTER TABLE sbom_metadata
    DROP COLUMN sbom_version;

ALTER TABLE sbom_metadata
    ALTER COLUMN application_version SET NOT NULL;

ALTER TABLE sbom_metadata
    ADD CONSTRAINT sbom_metadata_application_version_uk UNIQUE (application_id, application_version);
