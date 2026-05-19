-- SaaS Compatible
-- CLM-39840: Add failure classification columns to source_control_event
-- so the UI can distinguish retryable from non-retryable PR failures.

ALTER TABLE source_control_event
  ADD COLUMN IF NOT EXISTS event_failure_category VARCHAR(64) NULL;

ALTER TABLE source_control_event
  ADD COLUMN IF NOT EXISTS event_is_retryable BOOLEAN NULL;
