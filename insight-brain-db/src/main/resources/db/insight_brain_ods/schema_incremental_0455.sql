-- SaaS Compatible
-- NEXUS-52482: Remove feature flag gating for firewallWaiverDashboardAndRenew and waiverExpirationNotification
-- These features are now always enabled; remove any lingering DB rows that would have disabled them.

DELETE FROM system_configuration_property WHERE name = 'firewallWaiverDashboardAndRenew';
DELETE FROM system_configuration_property WHERE name = 'waiverExpirationNotification';
