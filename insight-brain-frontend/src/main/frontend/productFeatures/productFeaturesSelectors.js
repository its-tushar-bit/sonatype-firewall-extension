/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectProductFeaturesSlice = prop('productFeatures');
export const selectProductFeatures = createSelector(selectProductFeaturesSlice, prop('productFeatures'));
export const selectLoadingFeaturesSlice = createSelector(selectProductFeaturesSlice, prop('loading'));
export const selectLoadErrorFeaturesSlice = createSelector(selectProductFeaturesSlice, prop('loadError'));
export const selectIsEnforcementSupported = createSelector(selectProductFeatures, prop('enforcement'));
export const selectIsFirewallSupported = createSelector(selectProductFeatures, prop('firewall'));
export const selectIsMonitoringSupported = createSelector(selectProductFeatures, prop('policy-monitoring'));
export const selectIsGrandfatheringSupported = createSelector(selectProductFeatures, prop('policy-grandfathering'));
export const selectIsNotificationsSupported = createSelector(selectProductFeatures, prop('notifications'));
export const selectIsWebhooksSupported = createSelector(selectProductFeatures, (features) => {
  return features['webhooks-for-applications'] || features['webhooks-for-repositories'];
});

export const selectIsSourceControlSupported = createSelector(selectProductFeatures, prop('automation'));
export const selectIsInnerSourceRepositorySupported = createSelector(
  selectProductFeatures,
  prop('inner-source-repository-integration')
);
export const selectIsArtifactoryRepositorySupported = createSelector(selectProductFeatures, prop('built-from-source'));
export const selectIsEvaluateApplicationAvailable = createSelector(selectProductFeatures, prop('cli-integration'));
export const selectIsSourceControlForSourceTileSupported = createSelector(
  selectIsNotificationsSupported,
  selectIsSourceControlSupported,
  (notifications, automation) => notifications || automation
);
export const selectIsAdvancedLegalPackSupported = createSelector(selectProductFeatures, prop('advanced-legal-pack'));

export const selectIsReleaseIntegritySupported = createSelector(selectProductFeatures, prop('release-integrity'));
export const selectIsFirewallAutoUnquarantineSupported = createSelector(
  selectProductFeatures,
  prop('firewall-auto-unquarantine')
);
export const selectIsFirewallSupportedForNavigationContainer = createSelector(
  selectIsReleaseIntegritySupported,
  selectIsFirewallAutoUnquarantineSupported,
  (releaseIntegrity, firewallAutoUnquarantine) => releaseIntegrity && firewallAutoUnquarantine
);

export const selectIsDashboardSupported = createSelector(selectProductFeatures, prop('dashboard'));
export const selectIsReportListSupported = createSelector(selectProductFeatures, prop('reports-list'));
export const selectIsDataInsightsSupported = createSelector(selectProductFeatures, prop('data-insights'));
export const selectIsCrowdIntegrationSupported = createSelector(selectProductFeatures, prop('crowd-integration'));
export const selectIsInnerSourceTransitiveWaiverSupported = createSelector(
  selectProductFeatures,
  prop('inner-source-transitive-waiver')
);

export const selectIsAllowExternalHyperlinksSupported = createSelector(
  selectProductFeatures,
  prop('allow-external-hyperlinks')
);
