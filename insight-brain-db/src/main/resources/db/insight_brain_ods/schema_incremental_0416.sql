-- SaaS Compatible

-- Drop NOT NULL constraint from quarantined column in reevaluate_cascade_progress table
ALTER TABLE reevaluate_cascade_progress ALTER COLUMN quarantined DROP NOT NULL;
ALTER TABLE reevaluate_cascade_progress ALTER COLUMN quarantined DROP DEFAULT;
