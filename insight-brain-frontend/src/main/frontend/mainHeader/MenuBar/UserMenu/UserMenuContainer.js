/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import UserMenu from './UserMenu';
import { showUserTokenModal } from './UserToken/userTokenActions';
import { selectIsStandaloneDeveloper } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectCurrentUser, selectIsDefaultUser, selectCanChangePassword } from 'MainRoot/user/userSessionSelectors';
import { selectChangePasswordStatus, selectChangePasswordErrorMessage } from './changePasswordModalSelectors';
import { actions as changePasswordActions } from './changePasswordModalSlice';
import { logout } from 'MainRoot/user/userSessionSlice';

const mapDispatchToProps = (dispatch) =>
  bindActionCreators(
    {
      onLogout: logout,
      onChangePassword: changePasswordActions.changePassword,
      resetPasswordStatus: changePasswordActions.resetStatus,
      onManageUserToken: showUserTokenModal,
    },
    dispatch
  );

const mapStateToProps = (state) => {
  const userToken = state.userToken;

  return {
    user: selectCurrentUser(state),
    isDefaultUser: selectIsDefaultUser(state),
    isUserTokenModalVisible: userToken.isUserTokenModalVisible,
    canChangePassword: selectCanChangePassword(state),
    changePasswordStatus: selectChangePasswordStatus(state),
    changePasswordErrorMessage: selectChangePasswordErrorMessage(state),
    isStandaloneDeveloper: selectIsStandaloneDeveloper(state),
  };
};

const UserMenuContainer = connect(mapStateToProps, mapDispatchToProps)(UserMenu);

export default UserMenuContainer;
