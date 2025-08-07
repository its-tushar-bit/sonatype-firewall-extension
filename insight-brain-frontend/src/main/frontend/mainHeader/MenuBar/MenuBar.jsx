/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import HelpMenu from './HelpMenu/HelpMenu';
import SystemPreferencesMenu from './SystemPreferencesMenu/SystemPreferencesMenu';
import SolutionSwitcherContainer from './SolutionSwitcherContainer/SolutionSwitcherContainer';
import UserMenu from './UserMenu/UserMenuContainer';
import LoginButton from './LoginButton/LoginButton';
import NotificationsMenuContainer from './NotificationsMenu/NotificationsMenuContainer';
import GlobalHeader from './GlobalHeader';
import { PRODUCT_NAMES } from './useProductInfo';

export const MenuBar = ({
  majorMinorVersion = '',
  userActions,
  permissions = {},
  isWebhooksSupported = false,
  isSourceControlSupported,
  login,
  isLoggedIn = false,
  shouldShowLoginButton = false,
  isCrowdIntegrationEnabled,
  isWebhookConfigurationEnabled,
  isProductLicenseConfigurationEnabled,
  isLdapConfigurationEnabled,
  isEmailConfigurationEnabled,
  isProxyConfigurationEnabled,
  isSystemNoticeConfigurationEnabled,
  isSuccessMetricsConfigurationEnabled,
  isAutomaticApplicationConfigurationEnabled,
  isAutomaticScmConfigurationEnabled,
  isAdvancedSearchConfigurationEnabled,
  isShowNotificationMenuEnabled,
  isBaseUrlConfigurationEnabled,
  isSamlConfigurationEnabled,
  isMonitoringSupported,
  isSsoIdpManagedBySonatype,
  isSingleTenant,
  isSbomManagerOnlyLicense,
  isStandaloneDeveloper,
  isStandaloneFirewall,
  isOrgsAndAppsEnabled,
  isFirewallOnlyLicense,
  isZscalerEnabled,
  isStandaloneSbomManager,
}) => {
  const hasAnyPermissions = Object.values(permissions).filter(Boolean).length > 0;

  const product = getProduct(
    isStandaloneDeveloper,
    isStandaloneFirewall,
    isStandaloneSbomManager,
    isFirewallOnlyLicense,
    isSbomManagerOnlyLicense
  );

  if (!isLoggedIn && shouldShowLoginButton) {
    return (
      <GlobalHeader product={product}>
        <LoginButton onClick={login} />
      </GlobalHeader>
    );
  }

  if (!isLoggedIn) {
    return <GlobalHeader product={product} />;
  }

  return (
    <GlobalHeader product={product}>
      <HelpMenu majorMinorVersion={majorMinorVersion} />
      {isShowNotificationMenuEnabled && !isStandaloneDeveloper && <NotificationsMenuContainer />}
      {hasAnyPermissions && !isStandaloneDeveloper && (
        <SystemPreferencesMenu
          permissions={permissions}
          isWebhooksSupported={isWebhooksSupported}
          isSourceControlSupported={isSourceControlSupported}
          isCrowdIntegrationEnabled={isCrowdIntegrationEnabled}
          isWebhookConfigurationEnabled={isWebhookConfigurationEnabled}
          isProductLicenseConfigurationEnabled={isProductLicenseConfigurationEnabled}
          isLdapConfigurationEnabled={isLdapConfigurationEnabled}
          isEmailConfigurationEnabled={isEmailConfigurationEnabled}
          isProxyConfigurationEnabled={isProxyConfigurationEnabled}
          isSystemNoticeConfigurationEnabled={isSystemNoticeConfigurationEnabled}
          isSuccessMetricsConfigurationEnabled={isSuccessMetricsConfigurationEnabled}
          isAutomaticApplicationConfigurationEnabled={isAutomaticApplicationConfigurationEnabled}
          isAutomaticScmConfigurationEnabled={isAutomaticScmConfigurationEnabled}
          isAdvancedSearchConfigurationEnabled={isAdvancedSearchConfigurationEnabled}
          isBaseUrlConfigurationEnabled={isBaseUrlConfigurationEnabled}
          isSamlConfigurationEnabled={isSamlConfigurationEnabled}
          isMonitoringSupported={isMonitoringSupported}
          isSsoIdpManagedBySonatype={isSsoIdpManagedBySonatype}
          isSbomManagerOnlyLicense={isSbomManagerOnlyLicense}
          isStandaloneFirewall={isStandaloneFirewall}
          isOrgsAndAppsEnabled={isOrgsAndAppsEnabled}
          isFirewallOnlyLicense={isFirewallOnlyLicense}
          isZscalerEnabled={isZscalerEnabled}
        />
      )}
      <SolutionSwitcherContainer />
      <UserMenu userActions={userActions} />
    </GlobalHeader>
  );
};

MenuBar.propTypes = {
  permissions: PropTypes.object,
  isWebhooksSupported: PropTypes.bool,
  isSourceControlSupported: PropTypes.bool,
  userActions: PropTypes.shape({
    loadUser: PropTypes.func,
    logout: PropTypes.func,
    changePassword: PropTypes.func,
  }).isRequired,
  majorMinorVersion: PropTypes.string,
  login: PropTypes.func,
  isLoggedIn: PropTypes.bool,
  shouldShowLoginButton: PropTypes.bool,
  isCrowdIntegrationEnabled: PropTypes.bool,
  isWebhookConfigurationEnabled: PropTypes.bool,
  isProductLicenseConfigurationEnabled: PropTypes.bool,
  isLdapConfigurationEnabled: PropTypes.bool,
  isEmailConfigurationEnabled: PropTypes.bool,
  isProxyConfigurationEnabled: PropTypes.bool,
  isSystemNoticeConfigurationEnabled: PropTypes.bool,
  isSuccessMetricsConfigurationEnabled: PropTypes.bool,
  isAutomaticApplicationConfigurationEnabled: PropTypes.bool,
  isAutomaticScmConfigurationEnabled: PropTypes.bool,
  isAdvancedSearchConfigurationEnabled: PropTypes.bool,
  isNotificationMenuEnabled: PropTypes.bool,
  isBaseUrlConfigurationEnabled: PropTypes.bool,
  isSamlConfigurationEnabled: PropTypes.bool,
  isMonitoringSupported: PropTypes.bool,
  isShowNotificationMenuEnabled: PropTypes.bool,
  isSsoIdpManagedBySonatype: PropTypes.bool,
  isSingleTenant: PropTypes.bool,
  isSbomManagerOnlyLicense: PropTypes.bool,
  isStandaloneDeveloper: PropTypes.bool,
  isStandaloneFirewall: PropTypes.bool,
  isOrgsAndAppsEnabled: PropTypes.bool,
  isFirewallOnlyLicense: PropTypes.bool,
  isZscalerEnabled: PropTypes.bool,
  isStandaloneSbomManager: PropTypes.bool,
};

function getProduct(
  isStandaloneDeveloper,
  isStandaloneFirewall,
  isStandaloneSbomManager,
  isFirewallOnlyLicense,
  isSbomManagerOnlyLicense
) {
  if (isStandaloneFirewall || isFirewallOnlyLicense) {
    return PRODUCT_NAMES.FIREWALL;
  } else if (isStandaloneSbomManager || isSbomManagerOnlyLicense) {
    return PRODUCT_NAMES.SBOM_MANAGER;
  } else if (isStandaloneDeveloper) {
    return PRODUCT_NAMES.DEVELOPER;
  }
  return PRODUCT_NAMES.LIFECYCLE;
}

export default MenuBar;
