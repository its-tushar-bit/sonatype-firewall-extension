/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import HelpMenu from './HelpMenu/HelpMenu';
import SystemPreferencesMenu from './SystemPreferencesMenu/SystemPreferencesMenu';
import UserMenu from './UserMenu/UserMenuContainer';
import LoginButton from './LoginButton/LoginButton';
import NotificationsMenuContainer from './NotificationsMenu/NotificationsMenuContainer';

export const MenuBar = ({
  majorMinorVersion = '',
  userActions,
  permissions = {},
  isWebhooksSupported = false,
  isLabsDataInsightsEnabled,
  login,
  isLoggedIn = false,
  shouldShowLoginButton = false,
}) => {
  const hasAnyPermissions = Object.values(permissions).filter(Boolean).length > 0 || isLabsDataInsightsEnabled;

  if (!isLoggedIn && shouldShowLoginButton) {
    return (
      <div id="menu-bar" className="nx-global-header__actions menu-bar">
        <LoginButton onClick={login} />
      </div>
    );
  }
  if (!isLoggedIn) {
    return null;
  }

  return (
    <div id="menu-bar" className="nx-global-header__actions menu-bar">
      <HelpMenu majorMinorVersion={majorMinorVersion} />
      <NotificationsMenuContainer />
      {hasAnyPermissions && (
        <SystemPreferencesMenu
          permissions={permissions}
          isWebhooksSupported={isWebhooksSupported}
          dataInsightsEnabled={isLabsDataInsightsEnabled}
        />
      )}
      <UserMenu userActions={userActions} />
    </div>
  );
};

MenuBar.propTypes = {
  permissions: PropTypes.object,
  isWebhooksSupported: PropTypes.bool,
  isLabsDataInsightsEnabled: PropTypes.bool,
  userActions: PropTypes.shape({
    loadUser: PropTypes.func,
    logout: PropTypes.func,
    changePassword: PropTypes.func,
  }).isRequired,
  majorMinorVersion: PropTypes.string,
  login: PropTypes.func,
  isLoggedIn: PropTypes.bool,
  shouldShowLoginButton: PropTypes.bool,
};

export default MenuBar;
