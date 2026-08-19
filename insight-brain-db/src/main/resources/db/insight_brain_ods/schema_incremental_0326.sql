-- Since 1.171
-- SaaS Compatible
-- Delete feature flag for Enterprise Reporting

DELETE FROM system_configuration_property WHERE name = 'integratedEnterpriseReporting';