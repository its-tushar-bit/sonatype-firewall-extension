-- Since 1.197
-- SaaS Compatible

ALTER TABLE enterprise_reporting_filter DROP CONSTRAINT enterprise_reporting_filter_user_fk;
ALTER TABLE enterprise_reporting_default_filter DROP CONSTRAINT enterprise_reporting_default_filter_user_fk;
