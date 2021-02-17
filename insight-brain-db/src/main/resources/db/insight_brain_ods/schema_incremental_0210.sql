-- Since 1.106
CREATE INDEX repository_component_release_quarantine_idx
ON repository_component (quarantine_time, unquarantine_time, auto_unquarantined);
