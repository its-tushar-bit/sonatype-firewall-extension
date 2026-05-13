-- Since 1.204
-- SaaS Compatible
-- CLM-39934: The githubAppAuthentication feature gate has been removed; GitHub App authentication
-- is now permanently on as part of its GA promotion. Delete any orphan rows customers may have
-- created while the feature was gated so no stale configuration remains.

DELETE FROM system_configuration_property WHERE name = 'githubAppAuthentication';
