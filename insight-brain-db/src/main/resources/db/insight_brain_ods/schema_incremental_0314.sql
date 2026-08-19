-- Since 1.169
-- SaaS Compatible

UPDATE system_configuration_property
SET name = 'integratedEnterpriseReporting'
WHERE name = 'lookerIntegratedEnterpriseReporting'
AND NOT EXISTS (
    SELECT 1 FROM system_configuration_property WHERE name = 'integratedEnterpriseReporting'
);
