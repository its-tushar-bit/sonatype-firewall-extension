-- since 1.174
-- SaaS compatible
-- CLM-29597 - Step 1 of renaming applicationVersion column to sbomVersion - Add nullable column sbom_version

ALTER TABLE sbom_metadata
    ADD sbom_version VARCHAR(200) NULL;
