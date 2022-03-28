/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectProductFeaturesSlice = prop('productFeatures');
export const selectIsEnforcementSupported = createSelector(selectProductFeaturesSlice, prop('enforcement'));
export const selectIsFirewallSupported = createSelector(selectProductFeaturesSlice, prop('firewall'));
export const selectIsMonitoringSupported = createSelector(selectProductFeaturesSlice, prop('policy-monitoring'));
export const selectIsGrandfatheringSupported = createSelector(
  selectProductFeaturesSlice,
  prop('policy-grandfathering')
);
export const selectIsNotificationsSupported = createSelector(selectProductFeaturesSlice, prop('notifications'));
export const selectIsWebhooksSupported = createSelector(selectProductFeaturesSlice, (features) => {
  return features['webhooks-for-applications'] || features['webhooks-for-repositories'];
});

export const selectIsSourceControlSupported = createSelector(selectProductFeaturesSlice, prop('automation'));
export const selectIsInnerSourceRepositorySupported = createSelector(
  selectProductFeaturesSlice,
  prop('inner-source-repository-integration')
);
export const selectIsEvaluateApplicationAvailable = createSelector(selectProductFeaturesSlice, prop('cli-integration'));
export const selectIsSourceControlForSourceTileSupported = createSelector(
  selectIsNotificationsSupported,
  selectIsSourceControlSupported,
  (notifications, automation) => notifications || automation
);
export const selectIsAdvancedLegalPackSupported = createSelector(
  selectProductFeaturesSlice,
  prop('advanced-legal-pack')
);

export const selectIsReleaseIntegritySupported = createSelector(selectProductFeaturesSlice, prop('release-integrity'));
export const selectIsFirewallAutoUnquarantineSupported = createSelector(
  selectProductFeaturesSlice,
  prop('firewall-auto-unquarantine')
);
export const selectIsFirewallSupportedForNavigationContainer = createSelector(
  selectIsReleaseIntegritySupported,
  selectIsFirewallAutoUnquarantineSupported,
  (releaseIntegrity, firewallAutoUnquarantine) => releaseIntegrity && firewallAutoUnquarantine
);

export const selectIsDashboardSupported = createSelector(selectProductFeaturesSlice, prop('dashboard'));
export const selectIsReportListSupported = createSelector(selectProductFeaturesSlice, prop('reports-list'));
export const selectIsDataInsightsSupported = createSelector(selectProductFeaturesSlice, prop('data-insights'));
export const selectIsCrowdIntegrationSupported = createSelector(selectProductFeaturesSlice, prop('crowd-integration'));
