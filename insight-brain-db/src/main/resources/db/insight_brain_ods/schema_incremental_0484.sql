-- SaaS Compatible
-- CLM-42806: Remove consumption-reporting residue (phase 2 of 2). Phase 1 deleted the code; this
-- drops the leftover control surface: the Usage Viewer role, the VIEW_USAGE permission, the
-- consumption-reporting feature flag row, and the consumption tables. No running code references
-- these after phase 1, and phase 2 deploys after phase 1.

-- Remove the Usage Viewer role. role_permission and membership_mapping both FK role(role_id),
-- so their rows go first. Deleting all VIEW_USAGE rows also strips the permission from the
-- System Administrator role (and any custom role it was added to).
DELETE FROM membership_mapping WHERE role_id = '070e6c31fc8a42159df5298313b8a829';
DELETE FROM role_permission WHERE permission = 'VIEW_USAGE';
DELETE FROM role WHERE role_id = '070e6c31fc8a42159df5298313b8a829';

-- Remove the persisted consumption-reporting feature flag (present only if it was ever enabled).
DELETE FROM system_configuration_property WHERE name = 'consumptionReportingEnabled';

DROP TABLE IF EXISTS consumption_events;
DROP TABLE IF EXISTS consumption_limit_config;
