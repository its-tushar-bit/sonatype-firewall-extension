-- since 1.186
-- SaaS compatible

-- Remove NOT NULL constraint
ALTER TABLE sbom_metadata ALTER COLUMN file_name DROP NOT NULL;
