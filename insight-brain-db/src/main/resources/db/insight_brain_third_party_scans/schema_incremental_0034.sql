-- since 1.181
-- SaaS compatible

ALTER TABLE sbom_metadata
    ADD COLUMN scan_type VARCHAR(20) NULL;
