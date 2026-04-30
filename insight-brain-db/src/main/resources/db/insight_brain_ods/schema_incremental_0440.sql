-- SaaS Compatible
-- CLM-39406: Widen user_id columns in enterprise reporting filter tables to support LDAP DNs > 50 chars

ALTER TABLE enterprise_reporting_filter ALTER COLUMN user_id TYPE varchar(255);
ALTER TABLE enterprise_reporting_default_filter ALTER COLUMN user_id TYPE varchar(255);
