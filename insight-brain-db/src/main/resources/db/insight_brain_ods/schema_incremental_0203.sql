-- Since 1.104
CREATE INDEX repository_component_quarantine_idx ON repository_component(repository_id, quarantine_time);
