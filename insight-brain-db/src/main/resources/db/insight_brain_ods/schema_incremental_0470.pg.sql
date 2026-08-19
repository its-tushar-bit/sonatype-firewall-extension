-- SaaS Compatible
-- CLM-41006: Add index on repository_component.component_id to avoid full table scans
-- when NXRM calls DELETE /api/v2/repositories/{instanceId}/components for disqualification.
-- The column is nullable so we use a partial index to skip NULL rows.

CREATE INDEX IF NOT EXISTS idx_repository_component_component_id
  ON repository_component(component_id)
  WHERE component_id IS NOT NULL;
