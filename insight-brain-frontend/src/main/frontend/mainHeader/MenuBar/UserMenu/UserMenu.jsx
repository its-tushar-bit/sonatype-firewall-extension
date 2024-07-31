/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { faUserCircle } from '@fortawesome/pro-solid-svg-icons';
import { MenuButton, MenuTitle } from '../MenuButton/MenuButton';
import UserTokenModalContainer from './UserToken/UserTokenModalContainer';
import { useStateTransition } from '../../../react/useStateTransition';

import ChangePasswordModal from './ChangePasswordModal';
import UserDetailsModal from './UserDetailsModal';

const UserMenu = ({
  user,
  isUserTokenModalVisible,
  loadUser,
  onLogout,
  canChangePassword,
  onChangePassword,
  resetPasswordStatus,
  changePasswordStatus,
  changePasswordErrorMessage,
  onManageUserToken,
  isStandaloneDeveloper,
}) => {
  useEffect(() => void loadUser(), []);
  const [isChangePasswordModalVisible, setIsChangePasswordModalVisible] = useState(false);
  const [isUserDetailsModalVisible, setIsUserDetailsModalVisible] = useState(false);

  // used to close the modal after a successful password change
  useStateTransition(changePasswordStatus, 'success', 'idle', () => {
    setIsChangePasswordModalVisible(false);
  });

  useEffect(() => {
    resetPasswordStatus();
  }, [isChangePasswordModalVisible]);

  return (
    <div id="user-menu">
      <MenuButton icon={faUserCircle} id="user-menu-dropdown" iconLabel="Manage User Account">
        <MenuTitle>
          Current User:
          <span id="user-name" className="iq-user-name">
            {user ? user.displayName : ''}
          </span>
        </MenuTitle>
        {canChangePassword && !isStandaloneDeveloper && (
          <a
            id="change-password"
            tabIndex="0"
            onClick={() => setIsChangePasswordModalVisible(true)}
            className="iq-dropdown-menu__link--main-header"
          >
            Change Password
          </a>
        )}

        <a
          id="user-token-management"
          tabIndex="0"
          onClick={onManageUserToken}
          className="iq-dropdown-menu__link--main-header"
        >
          Manage User Token
        </a>

        <a
          id="user-details"
          tabIndex="0"
          onClick={() => setIsUserDetailsModalVisible(true)}
          className="iq-dropdown-menu__link--main-header"
        >
          Details
        </a>

        <a id="logout" tabIndex="0" onClick={onLogout} className="iq-dropdown-menu__link--main-header">
          Logout
        </a>
      </MenuButton>
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
  loadUser: PropTypes.func,
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
