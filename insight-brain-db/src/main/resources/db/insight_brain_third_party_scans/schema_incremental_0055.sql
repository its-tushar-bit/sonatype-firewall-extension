-- SaaS Compatible
CREATE INDEX IF NOT EXISTS sbom_metadata_status_app_id_created_at_id_idx ON sbom_metadata(status, application_id, created_at, sbom_metadata_id);
