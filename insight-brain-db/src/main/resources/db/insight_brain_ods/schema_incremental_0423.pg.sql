-- Since 1.200
-- SaaS Compatible

-- CLM-37867: Enable CODE_INSIGHTS for all MTIQ tenants (PostgreSQL version)
--
-- Background:
-- The CODE_INSIGHTS feature flag (SystemConfigurationPropertyFeature.CODE_INSIGHTS) defaults to true.
-- This migration ensures CODE_INSIGHTS is set to false for all MTIQ tenant schemas.
-- The global tenant should have this row deleted (allowing default true), while
-- non-global tenants should have it explicitly set to false.
--
-- This migration runs once per tenant schema in MTIQ environments.
-- The schema context is already set via SET SCHEMA before this script runs.

-- Delete any existing CODE_INSIGHTS entry regardless of value
-- For global tenant: this allows the feature to use its default value (true)
-- For non-global tenants: this clears any existing value before inserting the new one
DELETE FROM system_configuration_property
WHERE name = 'CODE_INSIGHTS';

-- For non-global tenants only: Insert CODE_INSIGHTS='false'
-- We use CURRENT_SCHEMA() to conditionally insert only for tenant schemas (prefixed with 't_')
-- Note: No need for EXISTS check since we just deleted all CODE_INSIGHTS entries above
INSERT INTO system_configuration_property (system_configuration_property_id, name, value)
SELECT 'a7f3e9c2d1b04586a93f8c7e2d4b6f1a', 'CODE_INSIGHTS', 'false'
WHERE CURRENT_SCHEMA() LIKE 't_%';
