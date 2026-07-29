/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { Box, DropdownMenu, Flex, IconButton, Tooltip } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import ShellDropdownRoot from 'MainRoot/nosc/shell/ShellDropdownRoot';
import {
  selectIsAutomationSupported,
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
  selectIsAdvancedSearchConfigurationEnabled,
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
import {
  selectIsSbomManager,
  selectIsStandaloneFirewall,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectPermissions } from 'MainRoot/mainHeader/mainHeaderSelectors';

/**
 * Radix-native System Preferences gear menu for the Nexus One TopNav.
 *
 * Same business rules as Classic's `SystemPreferencesMenu` — verbatim
 * permission + license gating per item, verbatim ordering, items
 * navigate to the same Classic ui-router states. Renders Radix
 * primitives (matches PreviewSolutionSwitcher chrome at 320px wide,
 * single flat list, no header bar).
 *
 * Only Nexus-One-specific item: "Nexus One UI" at the top, gated on
 * the Preview feature flag, that links to the existing
 * in-bundle `#/ui-settings` settings page (`nexusOneUiSettings`).
 *
 * All other items drop the user back into Classic chrome for that
 * admin page (no Nexus-One equivalents exist yet).
 *
 * The classic `SystemPreferencesMenu.jsx` ALSO contains a feature-
 * flag-gated "Nexus One UI" item so the same entry surfaces in
 * Classic's gear menu when the Preview flag is on.
 */

interface ItemSpec {
  /** Display label. */
  label: string;
  /** ui-router state name passed to useRouterState().href(...). */
  stateName: string;
  /** Optional state-name prefix (e.g. 'firewall', 'sbomManager') per
   *  the Classic mapping. */
  prefix?: string;
  /** Whether this item is visible to the current user. */
  showIf: boolean;
  /** Stable test id. */
  testId: string;
}

export default function PreviewSystemPreferencesMenu(): JSX.Element {
  const permissions = useSelector(selectPermissions) as {
    CONFIGURE_SYSTEM?: boolean;
    VIEW_ROLES?: boolean;
    MANAGE_AUTOMATIC_APPLICATION_CREATION?: boolean;
    MANAGE_AUTOMATIC_SCM_CONFIGURATION?: boolean;
  };
  const CONFIGURE_SYSTEM = !!permissions?.CONFIGURE_SYSTEM;
  const VIEW_ROLES = !!permissions?.VIEW_ROLES;
  const MANAGE_AUTOMATIC_APPLICATION_CREATION = !!permissions?.MANAGE_AUTOMATIC_APPLICATION_CREATION;
  const MANAGE_AUTOMATIC_SCM_CONFIGURATION = !!permissions?.MANAGE_AUTOMATIC_SCM_CONFIGURATION;

  const isWebhooksSupported = useSelector(selectIsWebhooksSupported);
  const isCrowdIntegrationEnabled = useSelector(selectIsCrowdIntegrationSupported);
  const isWebhookConfigurationEnabled = useSelector(selectIsWebhookConfigurationEnabled);
  const isProductLicenseConfigurationEnabled = useSelector(selectIsProductLicenseConfigurationEnabled);
  const isLdapConfigurationEnabled = useSelector(selectIsLdapConfigurationEnabled);
  const isEmailConfigurationEnabled = useSelector(selectIsEmailConfigurationEnabled);
  const isProxyConfigurationEnabled = useSelector(selectIsProxyConfigurationEnabled);
  const isSystemNoticeConfigurationEnabled = useSelector(selectIsSystemNoticeConfigurationEnabled);
  const isSuccessMetricsConfigurationEnabled = useSelector(selectIsSuccessMetricsConfigurationEnabled);
  const isAutomaticApplicationConfigurationEnabled = useSelector(
    selectIsAutomaticApplicationConfigurationEnabled,
  );
  const isAutomaticScmConfigurationEnabled = useSelector(selectIsAutomaticScmConfigurationEnabled);
  const isAdvancedSearchConfigurationEnabled = useSelector(selectIsAdvancedSearchConfigurationEnabled);
  const isBaseUrlConfigurationEnabled = useSelector(selectIsBaseUrlConfigurationEnabled);
  const isSamlConfigurationEnabled = useSelector(selectIsSAMLEnabled);
  const isOAuth2ConfigurationEnabled = useSelector(selectIsOAuth2Enabled);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isSsoIdpManagedBySonatype = useSelector(selectIsSsoIdpManagedBySonatype);
  const tenantMode = useSelector(selectTenantMode);
  const isSingleTenant = tenantMode !== 'multi-tenant';
  const isSbomManagerOnlyLicense = useSelector(selectIsSbomManagerOnlyLicense);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isFirewallOnlyLicense = useSelector(selectIsFirewallOnlyLicense);
  const isZscalerEnabled = useSelector(selectIsZscalerEnabled);
  const isSbomManager = useSelector(selectIsSbomManager);
  const productLicense = useSelector(selectProductLicense);
  const isUserManagementEnabled = useSelector(selectIsUserManagementPagesEnabled);
  const isUserActivityTrackingEnabled = useSelector(selectIsUserActivityTrackingEnabled);
  const isPreviewNexusOneUiEnabled = useSelector(selectIsPreviewNexusOneUiEnabled);

  const firewallPrefix = isFirewallOnlyLicense || isStandaloneFirewall ? 'firewall' : '';
  const sbomManagerPrefix = isSbomManager ? 'sbomManager' : '';

  // Same items + same showIf conditions as
  // mainHeader/MenuBar/SystemPreferencesMenu/SystemPreferencesMenu.jsx.
  // Keep these two lists in lock-step until the Classic menu is retired.
  // Exception: pages embedded in the NOUX shell (e.g. Administrators, Product
  // License, Advanced Search, Waived Components, LDAP, User Tokens, SAML) intentionally omit `prefix` —
  // they target the in-shell NOUX state directly, which does not use the
  // firewall-prefix routing Classic requires.
  const items: ItemSpec[] = [
    {
      label: 'Nexus One UI',
      stateName: 'nexusOneUiSettings',
      showIf: CONFIGURE_SYSTEM && isPreviewNexusOneUiEnabled,
      testId: 'nexus-one-top-nav-settings-item-preview-ui',
    },
    {
      label: 'Users',
      // No prefix: the NOUX router registers `users` at the top level, not as `firewall.users`.
      stateName: 'users',
      showIf:
        CONFIGURE_SYSTEM &&
        (isUserManagementEnabled || isSsoIdpManagedBySonatype) &&
        !!productLicense,
      testId: 'nexus-one-top-nav-settings-item-users',
    },
    {
      label: 'User Activity',
      // No prefix: same reasoning as `users` above — top-level NOUX state, never Firewall-prefixed.
      stateName: 'userActivity',
      showIf:
        CONFIGURE_SYSTEM &&
        isUserActivityTrackingEnabled &&
        !isUserManagementEnabled &&
        !!productLicense,
      testId: 'nexus-one-top-nav-settings-item-user-activity',
    },
    {
      label: 'Roles',
      stateName: 'rolesList',
      prefix: firewallPrefix,
      showIf: VIEW_ROLES && !!productLicense,
      testId: 'nexus-one-top-nav-settings-item-roles',
    },
    {
      label: 'Administrators',
      stateName: 'administrators',
      showIf: CONFIGURE_SYSTEM && !!productLicense,
      testId: 'nexus-one-top-nav-settings-item-administrators',
    },
    {
      label: 'Product License',
      stateName: 'productlicense',
      showIf: CONFIGURE_SYSTEM && (isProductLicenseConfigurationEnabled || !productLicense),
      testId: 'nexus-one-top-nav-settings-item-product-license',
    },
    {
      label: 'LDAP',
      // No prefix: `ldap-list` is embedded in NOUX at the top level, not a Classic firewall-prefixed state.
      stateName: 'ldap-list',
      showIf: CONFIGURE_SYSTEM && isLdapConfigurationEnabled,
      testId: 'nexus-one-top-nav-settings-item-ldap',
    },
    {
      label: 'SAML',
      // No prefix: saml is embedded in NOUX, not a Classic firewall-prefixed state.
      stateName: 'saml',
      showIf: CONFIGURE_SYSTEM && isSamlConfigurationEnabled,
      testId: 'nexus-one-top-nav-settings-item-saml',
    },
    {
      label: 'OIDC',
      stateName: 'oidc',
      prefix: firewallPrefix,
      showIf: CONFIGURE_SYSTEM && isOAuth2ConfigurationEnabled && isSingleTenant,
      testId: 'nexus-one-top-nav-settings-item-oidc',
    },
    {
      label: 'Waived Components',
      // No prefix: waivedComponentUpgradesConfiguration is embedded in NOUX, not a Classic firewall-prefixed state.
      stateName: 'waivedComponentUpgradesConfiguration',
      showIf:
        CONFIGURE_SYSTEM &&
        (isMonitoringSupported || isStandaloneFirewall || isFirewallOnlyLicense) &&
        !isSbomManagerOnlyLicense,
      testId: 'nexus-one-top-nav-settings-item-waived-components',
    },
    {
      label: 'Atlassian Crowd',
      stateName: 'atlassianCrowdConfiguration',
      prefix: firewallPrefix,
      showIf: CONFIGURE_SYSTEM && isCrowdIntegrationEnabled && !isSbomManagerOnlyLicense,
      testId: 'nexus-one-top-nav-settings-item-crowd',
    },
    {
      label: 'Email',
      stateName: 'mailConfig',
      prefix: firewallPrefix || sbomManagerPrefix,
      showIf: CONFIGURE_SYSTEM && isEmailConfigurationEnabled,
      testId: 'nexus-one-top-nav-settings-item-email',
    },
    {
      label: 'Zscaler',
      stateName: 'zscalerConfig',
      prefix: firewallPrefix,
      showIf:
        CONFIGURE_SYSTEM &&
        isZscalerEnabled &&
        (isStandaloneFirewall || isFirewallOnlyLicense),
      testId: 'nexus-one-top-nav-settings-item-zscaler',
    },
    {
      label: 'Proxy',
      stateName: 'proxyConfig',
      prefix: firewallPrefix,
      showIf: CONFIGURE_SYSTEM && isProxyConfigurationEnabled,
      testId: 'nexus-one-top-nav-settings-item-proxy',
    },
    {
      label: 'Webhooks',
      stateName: 'listWebhooks',
      prefix: firewallPrefix,
      // Classic wraps this in an outer CONFIGURE_SYSTEM check and a
      // not-supported tooltip; we fold both into showIf — if not
      // supported, the item is hidden (rather than disabled with a
      // tooltip; we can layer disabled+tooltip later if asked).
      showIf: CONFIGURE_SYSTEM && isWebhookConfigurationEnabled && isWebhooksSupported,
      testId: 'nexus-one-top-nav-settings-item-webhooks',
    },
    {
      label: 'System Notice',
      stateName: 'systemNoticeConfiguration',
      showIf: CONFIGURE_SYSTEM && isSystemNoticeConfigurationEnabled,
      testId: 'nexus-one-top-nav-settings-item-system-notice',
    },
    {
      label: 'Success Metrics',
      stateName: 'successMetricsConfiguration',
      showIf:
        CONFIGURE_SYSTEM &&
        isSuccessMetricsConfigurationEnabled &&
        !isSbomManagerOnlyLicense &&
        isOrgsAndAppsEnabled &&
        !isStandaloneFirewall &&
        !isFirewallOnlyLicense,
      testId: 'nexus-one-top-nav-settings-item-success-metrics',
    },
    {
      label: 'Automatic Applications',
      stateName: 'automaticApplicationsConfiguration',
      showIf:
        MANAGE_AUTOMATIC_APPLICATION_CREATION &&
        isAutomaticApplicationConfigurationEnabled &&
        !isSbomManagerOnlyLicense &&
        !isStandaloneFirewall &&
        !isFirewallOnlyLicense,
      testId: 'nexus-one-top-nav-settings-item-automatic-apps',
    },
    {
      label: 'Automatic SCM Configuration',
      stateName: 'automaticSourceControlConfiguration',
      showIf:
        MANAGE_AUTOMATIC_SCM_CONFIGURATION &&
        isAutomaticScmConfigurationEnabled &&
        !isSbomManagerOnlyLicense &&
        !isStandaloneFirewall &&
        !isFirewallOnlyLicense,
      testId: 'nexus-one-top-nav-settings-item-automatic-scm',
    },
    {
      label: 'Base URL',
      stateName: 'baseUrlConfiguration',
      showIf: CONFIGURE_SYSTEM && isBaseUrlConfigurationEnabled,
      testId: 'nexus-one-top-nav-settings-item-base-url',
    },
    {
      label: 'Advanced Search',
      // No prefix: advancedSearchConfig is embedded in NOUX, not a Classic firewall-prefixed state.
      stateName: 'advancedSearchConfig',
      showIf:
        CONFIGURE_SYSTEM &&
        isAdvancedSearchConfigurationEnabled &&
        !isSbomManagerOnlyLicense &&
        !isStandaloneFirewall &&
        !isFirewallOnlyLicense,
      testId: 'nexus-one-top-nav-settings-item-advanced-search',
    },
    {
      label: 'User Tokens Configuration',
      // No prefix: userTokensConfiguration is embedded in NOUX, not a Classic firewall-prefixed state.
      stateName: 'userTokensConfiguration',
      showIf: CONFIGURE_SYSTEM,
      testId: 'nexus-one-top-nav-settings-item-user-tokens',
    },
  ];

  const visible = items.filter((i) => i.showIf);

  const { href: hrefFromStateName } = useRouterState();

  return (
    <Flex align="center" data-testid="nexus-one-top-nav-settings">
      <ShellDropdownRoot>
        <Tooltip content="System Preferences" side="bottom" align="center">
          <DropdownMenu.Trigger>
            <IconButton
              variant="ghost"
              size="2"
              color="gray"
              aria-label="System Preferences"
            >
              <ActionIcons.Settings size={18} />
            </IconButton>
          </DropdownMenu.Trigger>
        </Tooltip>
        <DropdownMenu.Content align="end" sideOffset={6} style={{ width: 320 }}>
          {visible.length === 0 ? (
            <Box px="3" py="2">
              <span style={{ color: 'var(--gray-9)', fontSize: 'var(--font-size-2)' }}>
                No preferences available
              </span>
            </Box>
          ) : (
            visible.map((item) => {
              const fullStateName = item.prefix
                ? `${item.prefix}.${item.stateName}`
                : item.stateName;
              const href = hrefFromStateName(fullStateName);
              return (
                <DropdownMenu.Item key={item.testId} asChild>
                  <a
                    href={href}
                    data-testid={item.testId}
                    style={{ textDecoration: 'none', color: 'inherit' }}
                  >
                    {item.label}
                  </a>
                </DropdownMenu.Item>
              );
            })
          )}
        </DropdownMenu.Content>
      </ShellDropdownRoot>
    </Flex>
  );
}
