-- Since 1.184
-- SaaS Compatible
UPDATE system_configuration_property
SET value = SUBSTRING(system_configuration_property_id FROM 1 FOR 5)
WHERE name = 'TELEMETRY_GENERATED_INSTANCE_ID'
  AND value = '****';
