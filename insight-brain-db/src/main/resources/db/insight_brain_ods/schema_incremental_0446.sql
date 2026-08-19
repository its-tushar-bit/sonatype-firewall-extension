-- SaaS Compatible
-- CLM-39888: Clear existing SAAS_LIFECYCLE_SCM_PRS_ENABLED rows before flipping enabledWhenAbsent to true.
-- With enabledWhenAbsent=false, a present row meant "enabled". With enabledWhenAbsent=true, a present row
-- means "disabled". Deleting existing rows ensures previously-enabled tenants remain enabled under the new default.

DELETE FROM system_configuration_property WHERE name = 'SAAS_LIFECYCLE_SCM_PRS_ENABLED';
