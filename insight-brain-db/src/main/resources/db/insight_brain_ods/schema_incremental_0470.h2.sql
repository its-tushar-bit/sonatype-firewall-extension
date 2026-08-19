-- SaaS Compatible
-- CLM-41006: Add index on repository_component.component_id (H2 variant — no partial index support)
CREATE INDEX IF NOT EXISTS idx_repository_component_component_id
  ON repository_component(component_id);
