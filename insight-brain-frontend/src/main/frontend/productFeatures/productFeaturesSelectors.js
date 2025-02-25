/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { and, prop, propOr } from 'ramda';
import {
  selectIsRepositories,
  selectIsRepositoryContainer,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { PROVIDER_TYPES, SOURCE_CONTROL_OPTIONS } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';

export const selectProductFeaturesSlice = prop('productFeatures');
export const selectProductFeatures = createSelector(selectProductFeaturesSlice, prop('productFeatures'));
export const selectLoadingFeatures = createSelector(selectProductFeaturesSlice, prop('loading'));
export const selectLoadErrorFeatures = createSelector(selectProductFeaturesSlice, prop('loadError'));
export const selectIsEnforcementSupported = createSelector(selectProductFeatures, propOr(false, 'enforcement'));
export const selectIsFirewallSupported = createSelector(selectProductFeatures, propOr(false, 'firewall'));
export const selectIsMonitoringSupported = createSelector(selectProductFeatures, propOr(false, 'policy-monitoring'));
// this selector will need to be updated in CLM-28105
export const selectIsLegacyViolationSupported = createSelector(
  selectProductFeatures,
  propOr(false, 'policy-grandfathering')
);
export const selectIsNotificationsSupported = createSelector(selectProductFeatures, propOr(false, 'notifications'));
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

export const selectIsAutomationSupported = createSelector(selectProductFeatures, propOr(false, 'automation'));
export const selectIsInnerSourceRepositorySupported = createSelector(
  selectProductFeatures,
  propOr(false, 'inner-source-repository-integration')
);
export const selectIsArtifactoryRepositorySupported = createSelector(
  selectProductFeatures,
  propOr(false, 'built-from-source')
);
export const selectIsEvaluateApplicationAvailable = createSelector(
  selectProductFeatures,
  propOr(false, 'cli-integration')
);
export const selectIsSourceControlForSourceTileSupported = createSelector(
  selectIsNotificationsSupported,
  selectIsAutomationSupported,
  (notifications, automation) => notifications || automation
);
export const selectIsAdvancedLegalPackSupported = createSelector(
  selectProductFeatures,
  propOr(false, 'advanced-legal-pack')
);

export const selectIsReleaseIntegritySupported = createSelector(
  selectProductFeatures,
  propOr(false, 'release-integrity')
);
export const selectIsFirewallAutoUnquarantineSupported = createSelector(
  selectProductFeatures,
  propOr(false, 'firewall-auto-unquarantine')
);
export const selectIsFirewallSupportedForNavigationContainer = createSelector(
  selectIsReleaseIntegritySupported,
  selectIsFirewallAutoUnquarantineSupported,
  (releaseIntegrity, firewallAutoUnquarantine) => releaseIntegrity && firewallAutoUnquarantine
);

export const selectIsDashboardSupported = createSelector(selectProductFeatures, propOr(false, 'dashboard'));
export const selectIsDashboardWaiversSupported = createSelector(
  selectProductFeatures,
  propOr(false, 'waivers-dashboard')
);
export const selectIsReportListSupported = createSelector(selectProductFeatures, propOr(false, 'reports-list'));
export const selectIsCrowdIntegrationSupported = createSelector(
  selectProductFeatures,
  propOr(false, 'crowd-integration')
);
export const selectIsInnerSourceTransitiveWaiverSupported = createSelector(
  selectProductFeatures,
  propOr(false, 'inner-source-transitive-waiver')
);

export const selectIsAllowExternalHyperlinksSupported = createSelector(
  selectProductFeatures,
  propOr(false, 'allow-external-hyperlinks')
);
export const selectIsApiPageSupported = createSelector(selectProductFeatures, propOr(false, 'api-page'));
export const selectIsWebhookConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'webhook-configuration')
);
export const selectIsProductLicenseConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'product-license-configuration')
);
export const selectIsLdapConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'ldap-configuration')
);
export const selectIsEmailConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'email-configuration')
);
export const selectIsProxyConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'proxy-configuration')
);
export const selectIsSystemNoticeConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'system-notice-configuration')
);
export const selectIsSuccessMetricsConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'success-metrics-configuration')
);
export const selectIsAutomaticApplicationConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'automatic-application-configuration')
);
export const selectIsIntegratedEnterpriseReportingSupported = createSelector(
  selectProductFeatures,
  propOr(false, 'integrated-enterprise-reporting')
);
export const selectDataInsightsLicenseError = createSelector(
  selectIsIntegratedEnterpriseReportingSupported,
  selectLoadingFeatures,
  (isIntegratedEnterpriseReportingSupported, loading) =>
    isIntegratedEnterpriseReportingSupported || loading ? null : 'Data Insights feature not supported'
);

export const selectIsScmEnabled = createSelector(selectProductFeatures, propOr(false, 'saas-lifecycle-scm-enabled'));
const _selectIsAutomaticScmConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'automatic-scm-configuration')
);

// While _selectIsAutomaticScmConfigurationEnabled strictly checks the automatic-scm-configuration feature flag,
// this exported selectIsAutomaticScmConfigurationEnabled selector checks that flag in conjunction with whether SCM
// is enabled overall
export const selectIsAutomaticScmConfigurationEnabled = createSelector(
  _selectIsAutomaticScmConfigurationEnabled,
  selectIsScmEnabled,
  and
);

export const selectIsAdvancedSearchConfigurationEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'advanced-search-configuration')
);

export const selectIsSsoIdpManagedBySonatype = createSelector(
  selectProductFeatures,
  propOr(false, 'sso-idp-managed-by-sonatype')
);

export const selectIsDataRetentionEnabled = createSelector(selectProductFeatures, propOr(false, 'data-retention'));

export const selectIsInnerSourceRepositoriesEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'inner-source-repositories')
);
export const selectIsOrgsAndAppsEnabled = createSelector(selectProductFeatures, propOr(false, 'orgs-and-apps'));

export const selectIsProprietaryComponentsEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'proprietary-components')
);

export const selectIsSbomPoliciesSupported = createSelector(selectProductFeatures, propOr(false, 'sbom-policies'));

const selectIsSingleTenantEnabled = createSelector(selectProductFeatures, propOr(false, 'single-tenant'));
const selectIsMultiTenantEnabled = createSelector(selectProductFeatures, propOr(false, 'multi-tenant'));

const SINGLE_TENANT = 'single-tenant';
const MULTI_TENANT = 'multi-tenant';
const UNKNOWN_TENANT = 'unknown-tenant';
export const selectTenantMode = createSelector(
  selectIsSingleTenantEnabled,
  selectIsMultiTenantEnabled,
  (single, multi) => {
    if (single) {
      return SINGLE_TENANT;
    } else if (multi) {
      return MULTI_TENANT;
    }
    return UNKNOWN_TENANT;
  }
);

const MULTI_TENANT_SCM_PROVIDERS = new Set(['azure', 'bitbucket', 'github', 'gitlab']);

export const selectTenantScmProviderTypes = createSelector(selectTenantMode, (mode) => {
  if (mode === MULTI_TENANT) {
    return PROVIDER_TYPES.filter((provider) => MULTI_TENANT_SCM_PROVIDERS.has(provider.value.toLocaleLowerCase()));
  } else {
    return PROVIDER_TYPES;
  }
});

export const selectTenantScmOptionsTypes = createSelector(selectTenantMode, (mode) => {
  if (mode === MULTI_TENANT) {
    return SOURCE_CONTROL_OPTIONS.filter(
      (option) => option.id !== 'source-control-remediation-pull-requests' && option.id !== 'source-control-ssh'
    );
  } else {
    return SOURCE_CONTROL_OPTIONS;
  }
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

export const selectIsBaseUrlConfigurationEnabled = createSelector(selectTenantMode, (mode) => {
  return mode === SINGLE_TENANT;
});

export const selectIsDeveloperDashboardEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'developer-dashboard')
);

export const selectIsSbomManagerEnabled = createSelector(selectProductFeatures, propOr(false, 'sbom-manager'));
export const selectNoSbomManagerEnabledError = createSelector(
  selectIsSbomManagerEnabled,
  selectLoadingFeatures,
  selectIsSbomManager,
  (isSbomManagerEnable, loading, isSbomManager) =>
    !isSbomManagerEnable && isSbomManager && !loading ? 'The SBOM Manager license feature is not enabled.' : null
);

export const selectIsDeveloperBulkRecommendationsEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'developer-bulk-recommendations')
);

export const selectIsDeveloperSummaryTableEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'developer-summary-table')
);

export const selectIsSbomContinuousMonitoringUiEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'sbom-continuous-monitoring-ui')
);

export const selectIsAutoWaiversEnabled = createSelector(selectProductFeatures, propOr(false, 'auto-waivers'));

export const selectIsExpireWhenRemediationAvailableWaiversEnabled = createSelector(
  selectProductFeatures,
  propOr(false, 'expire-waiver-when-remediation-available')
);

export const selectIsNewScanProcessEnabled = createSelector(selectProductFeatures, propOr(false, 'new-scan-process'));
