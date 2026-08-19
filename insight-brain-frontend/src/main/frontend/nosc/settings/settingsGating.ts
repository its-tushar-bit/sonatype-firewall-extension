/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { SettingsItemId } from 'MainRoot/nosc/settings/settingsPageItems';

/**
 * Every permission/feature/license value any Settings hub item's `showIf`
 * may depend on — see useSettingsGatingContext.
 */
export interface SettingsGatingContext {
  readonly CONFIGURE_SYSTEM: boolean;
  readonly VIEW_ROLES: boolean;
  readonly MANAGE_AUTOMATIC_APPLICATION_CREATION: boolean;
  readonly MANAGE_AUTOMATIC_SCM_CONFIGURATION: boolean;
  readonly hasProductLicense: boolean;
  readonly isUserManagementEnabled: boolean;
  readonly isSsoIdpManagedBySonatype: boolean;
  readonly isUserActivityTrackingEnabled: boolean;
  readonly isProductLicenseConfigurationEnabled: boolean;
  readonly isLdapConfigurationEnabled: boolean;
  readonly isSamlConfigurationEnabled: boolean;
  readonly isOAuth2ConfigurationEnabled: boolean;
  readonly isSingleTenant: boolean;
  readonly isMonitoringSupported: boolean;
  readonly isStandaloneFirewall: boolean;
  readonly isFirewallOnlyLicense: boolean;
  readonly isSbomManagerOnlyLicense: boolean;
  readonly isCrowdIntegrationEnabled: boolean;
  readonly isEmailConfigurationEnabled: boolean;
  readonly isZscalerEnabled: boolean;
  readonly isProxyConfigurationEnabled: boolean;
  readonly isWebhookConfigurationEnabled: boolean;
  readonly isWebhooksSupported: boolean;
  readonly isSystemNoticeConfigurationEnabled: boolean;
  readonly isSuccessMetricsConfigurationEnabled: boolean;
  readonly isOrgsAndAppsEnabled: boolean;
  readonly isAutomaticApplicationConfigurationEnabled: boolean;
  readonly isAutomaticScmConfigurationEnabled: boolean;
  readonly isBaseUrlConfigurationEnabled: boolean;
  readonly isPreviewNexusOneUiEnabled: boolean;
}

/**
 * Single source of truth for the Settings hub's per-item visibility, keyed by
 * the same SettingsItemId union as SETTINGS_PAGE_ITEMS so every item has exactly
 * one predicate — a missing or extra entry is a compile error.
 */
export const SETTINGS_ITEM_SHOW_IF: Readonly<Record<SettingsItemId, (ctx: SettingsGatingContext) => boolean>> = {
  'nexus-one-ui': (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isPreviewNexusOneUiEnabled,
  users: (ctx) =>
    ctx.CONFIGURE_SYSTEM && (ctx.isUserManagementEnabled || ctx.isSsoIdpManagedBySonatype) && ctx.hasProductLicense,
  'user-activity': (ctx) =>
    ctx.CONFIGURE_SYSTEM && ctx.isUserActivityTrackingEnabled && !ctx.isUserManagementEnabled && ctx.hasProductLicense,
  roles: (ctx) => ctx.VIEW_ROLES && ctx.hasProductLicense,
  administrators: (ctx) => ctx.CONFIGURE_SYSTEM && ctx.hasProductLicense,
  'product-license': (ctx) =>
    ctx.CONFIGURE_SYSTEM && (ctx.isProductLicenseConfigurationEnabled || !ctx.hasProductLicense),
  ldap: (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isLdapConfigurationEnabled,
  saml: (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isSamlConfigurationEnabled,
  oidc: (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isOAuth2ConfigurationEnabled && ctx.isSingleTenant,
  'waived-components': (ctx) =>
    ctx.CONFIGURE_SYSTEM &&
    (ctx.isMonitoringSupported || ctx.isStandaloneFirewall || ctx.isFirewallOnlyLicense) &&
    !ctx.isSbomManagerOnlyLicense,
  'atlassian-crowd': (ctx) =>
    ctx.CONFIGURE_SYSTEM && ctx.isCrowdIntegrationEnabled && !ctx.isSbomManagerOnlyLicense,
  email: (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isEmailConfigurationEnabled,
  zscaler: (ctx) =>
    ctx.CONFIGURE_SYSTEM && ctx.isZscalerEnabled && (ctx.isStandaloneFirewall || ctx.isFirewallOnlyLicense),
  proxy: (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isProxyConfigurationEnabled,
  webhooks: (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isWebhookConfigurationEnabled && ctx.isWebhooksSupported,
  'system-notice': (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isSystemNoticeConfigurationEnabled,
  'success-metrics': (ctx) =>
    ctx.CONFIGURE_SYSTEM &&
    ctx.isSuccessMetricsConfigurationEnabled &&
    !ctx.isSbomManagerOnlyLicense &&
    ctx.isOrgsAndAppsEnabled &&
    !ctx.isStandaloneFirewall &&
    !ctx.isFirewallOnlyLicense,
  'automatic-applications': (ctx) =>
    ctx.MANAGE_AUTOMATIC_APPLICATION_CREATION &&
    ctx.isAutomaticApplicationConfigurationEnabled &&
    !ctx.isSbomManagerOnlyLicense &&
    !ctx.isStandaloneFirewall &&
    !ctx.isFirewallOnlyLicense,
  'automatic-scm': (ctx) =>
    ctx.MANAGE_AUTOMATIC_SCM_CONFIGURATION &&
    ctx.isAutomaticScmConfigurationEnabled &&
    !ctx.isSbomManagerOnlyLicense &&
    !ctx.isStandaloneFirewall &&
    !ctx.isFirewallOnlyLicense,
  'base-url': (ctx) => ctx.CONFIGURE_SYSTEM && ctx.isBaseUrlConfigurationEnabled,
  'user-tokens': (ctx) => ctx.CONFIGURE_SYSTEM,
  'change-password': () => true,
  'user-details': () => true,
  'display-theme': () => true,
  'user-token': () => true,
};
