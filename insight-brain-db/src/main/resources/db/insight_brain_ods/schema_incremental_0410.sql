-- SaaS Compatible
ALTER TABLE source_control ADD COLUMN close_pr_on_failed_checks_enabled boolean;
ALTER TABLE source_control ADD COLUMN close_pr_after_days_open_enabled boolean;
ALTER TABLE source_control ADD COLUMN close_pr_after_days INTEGER NULL;
