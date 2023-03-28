/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectProductFeaturesSlice = prop('productFeatures');
export const selectProductFeatures = createSelector(selectProductFeaturesSlice, prop('productFeatures'));
export const selectLoadingFeatures = createSelector(selectProductFeaturesSlice, prop('loading'));
export const selectLoadErrorFeatures = createSelector(selectProductFeaturesSlice, prop('loadError'));
export const selectIsEnforcementSupported = createSelector(selectProductFeatures, prop('enforcement'));
export const selectIsFirewallSupported = createSelector(selectProductFeatures, prop('firewall'));
export const selectIsMonitoringSupported = createSelector(selectProductFeatures, prop('policy-monitoring'));
export const selectIsGrandfatheringSupported = createSelector(selectProductFeatures, prop('policy-grandfathering'));
export const selectIsNotificationsSupported = createSelector(selectProductFeatures, prop('notifications'));
import { selectIsRepositories, selectIsRepositoryContainer } from 'MainRoot/reduxUiRouter/routerSelectors';
export const selectIsWebhooksSupported = createSelector(
  selectProductFeatures,
  (features) => features['webhooks-for-applications'] || features['webhooks-for-repositories']
);

export const selectIsPolicyWebhooksSupported = createSelector(
  selectProductFeatures,
  selectIsRepositories,
  selectIsRepositoryContainer,
  (features, isRepositories, isRepositoryContainer) =>
    features['webhooks-for-applications'] && !isRepositories && !isRepositoryContainer
);

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
export const selectIsApiPageSupported = createSelector(selectProductFeatures, prop('api-page'));
export const selectIsWebhookConfigurationEnabled = createSelector(selectProductFeatures, prop('webhook-configuration'));
export const selectIsProductLicenseConfigurationEnabled = createSelector(
  selectProductFeatures,
  prop('product-license-configuration')
);
export const selectIsLdapConfigurationEnabled = createSelector(selectProductFeatures, prop('ldap-configuration'));
export const selectIsEmailConfigurationEnabled = createSelector(selectProductFeatures, prop('email-configuration'));
export const selectIsProxyConfigurationEnabled = createSelector(selectProductFeatures, prop('proxy-configuration'));
export const selectIsSystemNoticeConfigurationEnabled = createSelector(
  selectProductFeatures,
  prop('system-notice-configuration')
);
export const selectIsSuccessMetricsConfigurationEnabled = createSelector(
  selectProductFeatures,
  prop('success-metrics-configuration')
);
export const selectIsAutomaticApplicationConfigurationEnabled = createSelector(
  selectProductFeatures,
  prop('automatic-application-configuration')
);
export const selectIsAutomaticScmConfigurationEnabled = createSelector(
  selectProductFeatures,
  prop('automatic-scm-configuration')
);
export const selectIsAdvancedSearchConfigurationEnabled = createSelector(
  selectProductFeatures,
  prop('advanced-search-configuration')
);

const selectIsSingleTenantEnabled = createSelector(selectProductFeatures, prop('single-tenant'));
const selectIsMultiTenantEnabled = createSelector(selectProductFeatures, prop('multi-tenant'));

const SINGLE_TENANT = 'single-tenant';
const MULTI_TENANT = 'multi-tenant';
const UNKNOWN_TENANT = 'unknown-tenant';
const selectTenantMode = createSelector(selectIsSingleTenantEnabled, selectIsMultiTenantEnabled, (single, multi) => {
  if (single) {
    return SINGLE_TENANT;
  } else if (multi) {
    return MULTI_TENANT;
  }
  return UNKNOWN_TENANT;
});

export const selectIsShowVersionEnabled = createSelector(selectTenantMode, (mode) => {
  return mode === SINGLE_TENANT;
});
export const selectIsShowNotificationMenuEnabled = createSelector(selectTenantMode, (mode) => {
  return mode === SINGLE_TENANT;
});

export const selectIsShowEmailStoppedEnabled = createSelector(selectTenantMode, (mode) => {
  return mode === SINGLE_TENANT;
});
