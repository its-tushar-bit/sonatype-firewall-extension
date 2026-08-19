-- Since 1.170
-- SaaS Compatible

ALTER TABLE application_count_history
  ADD COLUMN policy_action_failures_by_app_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE application_count_history
  ADD COLUMN waivers_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE application_count_history
  ADD COLUMN mean_time_to_remediate_ms BIGINT NOT NULL DEFAULT 0;
