-- Since 1.197
-- SaaS Compatible

-- CLM-37572: Add index to improve performance of /api/v2/reports/metrics REST API
-- This index optimizes queries on policy_evaluation table filtering by application_id and time
-- Customer reported improvement from ~7 minutes to ~1.5 minutes for one month of data

CREATE INDEX IF NOT EXISTS policy_evaluation_app_time_idx
  ON policy_evaluation(application_id, time);
