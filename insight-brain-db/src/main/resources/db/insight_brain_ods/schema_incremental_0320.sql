-- Since 1.170
-- SaaS Compatible
ALTER TABLE application_count_history
    ADD COLUMN IF NOT EXISTS scm_feedback_enabled_count INTEGER;

UPDATE application_count_history
SET scm_feedback_enabled_count = 0
WHERE scm_feedback_enabled_count IS NULL;

-- Normally marking this non-nullable in a single migration would not be SaaS compatible. This part of the migration
-- would have to happen in the next release after the first part of this migration.
-- It is ok, in this case because, a. this feature is only being used by a handful of lighthouse customers (none of whom
-- are on SaaS) and b. The
-- only code that would possibly fail as a result of this is not critical, would not be visible to the user, and would
-- recover over time (this table is updated by a cron job that runs once per day)
ALTER TABLE application_count_history
    ALTER COLUMN scm_feedback_enabled_count SET NOT NULL;
