-- SaaS Compatible
-- Add index on pull_request_poll_time to optimize PR polling queries.
CREATE INDEX IF NOT EXISTS source_control_pull_request_poll_time_idx
  ON source_control(pull_request_poll_time);
