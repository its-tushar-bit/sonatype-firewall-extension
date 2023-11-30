-- Since 1.170
-- SaaS Compatible

-- TODO: follow-up PR to add NOT NULL constraints
ALTER TABLE application_count_history
  ADD COLUMN policy_action_failures_by_app_count INTEGER DEFAULT 0;
ALTER TABLE application_count_history
  ADD COLUMN waivers_count INTEGER DEFAULT 0;
ALTER TABLE application_count_history
  ADD COLUMN mean_time_to_remediate_ms BIGINT DEFAULT 0;

ALTER TABLE application_count_history ALTER COLUMN policy_action_failures_by_app_count DROP DEFAULT;
ALTER TABLE application_count_history ALTER COLUMN waivers_count DROP DEFAULT;
ALTER TABLE application_count_history ALTER COLUMN mean_time_to_remediate_ms DROP DEFAULT;
