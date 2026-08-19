-- Since 1.173
-- Backwards compatible to 1.172
-- SaaS Compatible
DELETE FROM system_configuration_property
WHERE NAME = 'ORG_APP_MANAGEMENT_WEBHOOK_EVENT';
