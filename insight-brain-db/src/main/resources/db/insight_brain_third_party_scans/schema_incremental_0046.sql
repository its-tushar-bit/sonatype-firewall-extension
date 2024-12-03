-- since 1.185
-- SaaS compatible
-- SBOM-1065 - Increase SBOM application version database length limit:
--  change type from VARCHAR(200) to VARCHAR(1100)

ALTER TABLE sbom_metadata ALTER COLUMN sbom_version TYPE VARCHAR(1100);
