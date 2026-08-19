/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useSelector } from 'react-redux';
import {
  selectIsWebhooksSupported,
  selectIsCrowdIntegrationSupported,
  selectIsWebhookConfigurationEnabled,
  selectIsProductLicenseConfigurationEnabled,
  selectIsLdapConfigurationEnabled,
  selectIsEmailConfigurationEnabled,
  selectIsProxyConfigurationEnabled,
  selectIsSystemNoticeConfigurationEnabled,
  selectIsSuccessMetricsConfigurationEnabled,
  selectIsAutomaticApplicationConfigurationEnabled,
  selectIsAutomaticScmConfigurationEnabled,
  selectIsBaseUrlConfigurationEnabled,
  selectIsMonitoringSupported,
  selectTenantMode,
  selectIsSsoIdpManagedBySonatype,
  selectIsOrgsAndAppsEnabled,
  selectIsZscalerEnabled,
  selectIsSAMLEnabled,
  selectIsOAuth2Enabled,
  selectIsUserManagementPagesEnabled,
  selectIsUserActivityTrackingEnabled,
  selectIsPreviewNexusOneUiEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectIsSbomManagerOnlyLicense,
  selectIsFirewallOnlyLicense,
  selectProductLicense,
} from 'MainRoot/productFeatures/productLicenseSelectors';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectPermissions } from 'MainRoot/mainHeader/mainHeaderSelectors';
import type { SettingsGatingContext } from 'MainRoot/nosc/settings/settingsGating';

/**
 * Reads every permission/feature/license value any Settings hub item's
 * `showIf` may depend on, in one place.
 */
export function useSettingsGatingContext(): SettingsGatingContext {
  const permissions = useSelector(selectPermissions) as {
    CONFIGURE_SYSTEM?: boolean;
    VIEW_ROLES?: boolean;
    MANAGE_AUTOMATIC_APPLICATION_CREATION?: boolean;
    MANAGE_AUTOMATIC_SCM_CONFIGURATION?: boolean;
  };

  const isWebhooksSupported = useSelector(selectIsWebhooksSupported);
  const isCrowdIntegrationEnabled = useSelector(selectIsCrowdIntegrationSupported);
  const isWebhookConfigurationEnabled = useSelector(selectIsWebhookConfigurationEnabled);
  const isProductLicenseConfigurationEnabled = useSelector(selectIsProductLicenseConfigurationEnabled);
  const isLdapConfigurationEnabled = useSelector(selectIsLdapConfigurationEnabled);
  const isEmailConfigurationEnabled = useSelector(selectIsEmailConfigurationEnabled);
  const isProxyConfigurationEnabled = useSelector(selectIsProxyConfigurationEnabled);
  const isSystemNoticeConfigurationEnabled = useSelector(selectIsSystemNoticeConfigurationEnabled);
  const isSuccessMetricsConfigurationEnabled = useSelector(selectIsSuccessMetricsConfigurationEnabled);
  const isAutomaticApplicationConfigurationEnabled = useSelector(selectIsAutomaticApplicationConfigurationEnabled);
  const isAutomaticScmConfigurationEnabled = useSelector(selectIsAutomaticScmConfigurationEnabled);
  const isBaseUrlConfigurationEnabled = useSelector(selectIsBaseUrlConfigurationEnabled);
  const isSamlConfigurationEnabled = useSelector(selectIsSAMLEnabled);
  const isOAuth2ConfigurationEnabled = useSelector(selectIsOAuth2Enabled);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isSsoIdpManagedBySonatype = useSelector(selectIsSsoIdpManagedBySonatype);
  const tenantMode = useSelector(selectTenantMode);
  const isSbomManagerOnlyLicense = useSelector(selectIsSbomManagerOnlyLicense);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isFirewallOnlyLicense = useSelector(selectIsFirewallOnlyLicense);
  const isZscalerEnabled = useSelector(selectIsZscalerEnabled);
  const productLicense = useSelector(selectProductLicense);
  const isUserManagementEnabled = useSelector(selectIsUserManagementPagesEnabled);
  const isUserActivityTrackingEnabled = useSelector(selectIsUserActivityTrackingEnabled);
  const isPreviewNexusOneUiEnabled = useSelector(selectIsPreviewNexusOneUiEnabled);

  return {
    CONFIGURE_SYSTEM: !!permissions?.CONFIGURE_SYSTEM,
    VIEW_ROLES: !!permissions?.VIEW_ROLES,
    MANAGE_AUTOMATIC_APPLICATION_CREATION: !!permissions?.MANAGE_AUTOMATIC_APPLICATION_CREATION,
    MANAGE_AUTOMATIC_SCM_CONFIGURATION: !!permissions?.MANAGE_AUTOMATIC_SCM_CONFIGURATION,
    hasProductLicense: !!productLicense,
    isUserManagementEnabled,
    isSsoIdpManagedBySonatype,
    isUserActivityTrackingEnabled,
    isProductLicenseConfigurationEnabled,
    isLdapConfigurationEnabled,
    isSamlConfigurationEnabled,
    isOAuth2ConfigurationEnabled,
    isSingleTenant: tenantMode !== 'multi-tenant',
    isMonitoringSupported,
    isStandaloneFirewall,
    isFirewallOnlyLicense,
    isSbomManagerOnlyLicense,
    isCrowdIntegrationEnabled,
    isEmailConfigurationEnabled,
    isZscalerEnabled,
    isProxyConfigurationEnabled,
    isWebhookConfigurationEnabled,
    isWebhooksSupported,
    isSystemNoticeConfigurationEnabled,
    isSuccessMetricsConfigurationEnabled,
    isOrgsAndAppsEnabled,
    isAutomaticApplicationConfigurationEnabled,
    isAutomaticScmConfigurationEnabled,
    isBaseUrlConfigurationEnabled,
    isPreviewNexusOneUiEnabled,
  };
}
