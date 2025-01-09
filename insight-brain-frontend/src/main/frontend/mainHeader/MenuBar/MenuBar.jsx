/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import HelpMenu from './HelpMenu/HelpMenu';
import SystemPreferencesMenu from './SystemPreferencesMenu/SystemPreferencesMenu';
import SolutionSwitcherContainer from './SolutionSwitcherContainer/SolutionSwitcherContainer';
import UserMenu from './UserMenu/UserMenuContainer';
import LoginButton from './LoginButton/LoginButton';
import NotificationsMenuContainer from './NotificationsMenu/NotificationsMenuContainer';

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
}) => {
  const hasAnyPermissions = Object.values(permissions).filter(Boolean).length > 0;

  const backButtonPortalContainer = <div id="menu-bar__back-button-container" />;
  const breadcrumbPortalContainer = <div id="menu-bar__bread-crumb-container" />;

  if (!isLoggedIn && shouldShowLoginButton) {
    return (
      <div id="menu-bar" className="nx-global-header__actions menu-bar">
        <LoginButton onClick={login} />
      </div>
    );
  }

  if (!isLoggedIn) {
    return backButtonPortalContainer;
  }

  return (
    <Fragment>
      {backButtonPortalContainer}
      {breadcrumbPortalContainer}
      <div id="menu-bar" className="nx-global-header__actions menu-bar">
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
            isSingleTenant={isSingleTenant}
            isSbomManagerOnlyLicense={isSbomManagerOnlyLicense}
            isStandaloneFirewall={isStandaloneFirewall}
            isOrgsAndAppsEnabled={isOrgsAndAppsEnabled}
            isFirewallOnlyLicense={isFirewallOnlyLicense}
          />
        )}
        <SolutionSwitcherContainer />
        <UserMenu userActions={userActions} />
      </div>
    </Fragment>
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
  isStandaloneFirewall: PropTypes.bool,
  isFirewallOnlyLicense: PropTypes.bool,
};

export default MenuBar;
