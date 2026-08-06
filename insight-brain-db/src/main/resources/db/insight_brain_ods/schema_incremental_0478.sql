-- SaaS Compatible
-- CLM-42453: Remove feature flag gating for GLOBAL_SEARCH.
-- Global Search is now gated on PREVIEW_NEXUS_ONE_UI alongside the rest of the Nexus One UI it
-- backs; remove any lingering DB rows for the retired flag.

DELETE FROM system_configuration_property WHERE name = 'GLOBAL_SEARCH';
