-- SaaS Compatible
ALTER TABLE sbom_metadata ADD COLUMN IF NOT EXISTS extended_profile_elements TEXT;
ALTER TABLE sbom_metadata ADD COLUMN IF NOT EXISTS root_component_ref VARCHAR(40);
