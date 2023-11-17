-- Since 1.170
-- SaaS Compatible
ALTER TABLE application_count_history
    ADD COLUMN IF NOT EXISTS scm_feedback_enabled_count INTEGER;

UPDATE application_count_history
SET scm_feedback_enabled_count = 0
WHERE scm_feedback_enabled_count IS NULL;

ALTER TABLE application_count_history
    ALTER COLUMN scm_feedback_enabled_count SET NOT NULL;
