/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { faUserCircle } from '@fortawesome/pro-regular-svg-icons';
import UserTokenModalContainer from './UserToken/UserTokenModalContainer';
import { useStateTransition } from '../../../react/useStateTransition';
import DisplayThemeModal from 'MainRoot/configuration/displayTheme/DisplayThemeModal';
import {
  NxStatefulNavigationDropdown,
  NxTextLink,
  NxH4,
  NxP,
  NxNavigationDropdown,
} from '@sonatype/react-shared-components';
import ChangePasswordModal from './ChangePasswordModal';
import UserDetailsModal from './UserDetailsModal';

const UserMenu = ({
  user,
  isUserTokenModalVisible,
  onLogout,
  canChangePassword,
  onChangePassword,
  resetPasswordStatus,
  changePasswordStatus,
  changePasswordErrorMessage,
  onManageUserToken,
  isStandaloneDeveloper,
}) => {
  const [isChangePasswordModalVisible, setIsChangePasswordModalVisible] = useState(false);
  const [isUserDetailsModalVisible, setIsUserDetailsModalVisible] = useState(false);
  const [isDisplayThemeModalVisible, setIsDisplayThemeModalVisible] = useState(false);

  // used to close the modal after a successful password change
  useStateTransition(changePasswordStatus, 'success', 'idle', () => {
    setIsChangePasswordModalVisible(false);
  });

  useEffect(() => {
    resetPasswordStatus();
  }, [isChangePasswordModalVisible]);

  return (
    <div id="user-menu" className="iq-user-menu">
      <NxStatefulNavigationDropdown id="user-menu-dropdown" icon={faUserCircle} title="Manage User Account">
        <NxNavigationDropdown.MenuHeader>
          <NxH4>Current User:</NxH4>
          {user && user.displayName && <NxP id="user-name">{user.displayName}</NxP>}
        </NxNavigationDropdown.MenuHeader>
        {canChangePassword && !isStandaloneDeveloper && (
          <button
            id="change-password"
            onClick={() => setIsChangePasswordModalVisible(true)}
            className="nx-dropdown-button"
          >
            Change Password
          </button>
        )}
        <button id="user-token-management" onClick={onManageUserToken} className="nx-dropdown-button">
          Manage User Token
        </button>
        <NxTextLink id="user-details" onClick={() => setIsUserDetailsModalVisible(true)} className="nx-dropdown-button">
          Details
        </NxTextLink>

        <button
          id="display-theme"
          tabIndex="0"
          onClick={() => setIsDisplayThemeModalVisible(true)}
          className="nx-dropdown-button"
        >
          Display Theme
        </button>

        <button id="logout" onClick={onLogout} className="nx-dropdown-button">
          Logout
        </button>
      </NxStatefulNavigationDropdown>

      {isUserTokenModalVisible && <UserTokenModalContainer />}
      {isChangePasswordModalVisible && (
        <ChangePasswordModal
          onClose={() => setIsChangePasswordModalVisible(false)}
          onChangePassword={onChangePassword}
          changePasswordError={changePasswordErrorMessage}
          changePasswordStatus={changePasswordStatus}
        />
      )}
      {isUserDetailsModalVisible && (
        <UserDetailsModal user={user} onClose={() => setIsUserDetailsModalVisible(false)} />
      )}
      {isDisplayThemeModalVisible && <DisplayThemeModal onClose={() => setIsDisplayThemeModalVisible(false)} />}
    </div>
  );
};

UserMenu.propTypes = {
  user: PropTypes.shape({
    displayName: PropTypes.string,
  }),
  isDefaultUser: PropTypes.bool,
  canChangePassword: PropTypes.bool,
  isUserTokenModalVisible: PropTypes.bool,
  onLogout: PropTypes.func,
  onManageUserToken: PropTypes.func,
  onOpenUserDetails: PropTypes.func,
  onChangePassword: PropTypes.func,
  resetPasswordStatus: PropTypes.func,
  changePasswordStatus: PropTypes.oneOf(['idle', 'pending', 'success', 'failure']),
  changePasswordErrorMessage: PropTypes.string,
  isStandaloneDeveloper: PropTypes.bool,
};

export default UserMenu;
