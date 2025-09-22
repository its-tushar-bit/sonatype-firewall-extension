-- Add status column to reevaluate_cascade_request table for request-level lifecycle tracking
-- Enables tracking of cascade re-evaluation request status (PENDING, IN_PROGRESS, COMPLETED, NO_COMPONENTS_FOUND, FAILED)
-- Removes owner_id column as it was never used and never queried in the application

-- SaaS Compatible

ALTER TABLE reevaluate_cascade_request ADD COLUMN status varchar(50) DEFAULT 'PENDING' NOT NULL;
ALTER TABLE reevaluate_cascade_request DROP COLUMN owner_id;
