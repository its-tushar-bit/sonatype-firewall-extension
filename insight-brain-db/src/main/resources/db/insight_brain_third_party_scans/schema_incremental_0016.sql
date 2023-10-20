-- since 1.169
-- SaaS Compatible

-- This index was requested to improve performance mostly with Neuvector scans
CREATE INDEX coordinate_license_file_coordinate_id ON coordinate_license (file_coordinate_id);
