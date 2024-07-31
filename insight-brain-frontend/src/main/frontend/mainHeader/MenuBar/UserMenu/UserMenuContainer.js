/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import PropTypes from 'prop-types';
import UserMenu from './UserMenu';
import { showUserTokenModal } from './UserToken/userTokenActions';
import { selectIsStandaloneDeveloper } from 'MainRoot/reduxUiRouter/routerSelectors';

const mapDispatchToProps = (dispatch, { userActions }) =>
  bindActionCreators(
    {
      loadUser: userActions.loadUser,
      onLogout: userActions.logout,
      onChangePassword: userActions.changePassword,
      resetPasswordStatus: userActions.resetChangedPasswordStatus,
      onManageUserToken: showUserTokenModal,
    },
    dispatch
  );

const mapStateToProps = (state) => {
  const user = state.user;
  const userToken = state.userToken;

  return {
    user: user.currentUser,
    isDefaultUser: user.isDefaultUser,
    isUserTokenModalVisible: userToken.isUserTokenModalVisible,
    canChangePassword: user.canChangePassword,
    changePasswordStatus: user.changePasswordStatus,
    changePasswordErrorMessage: user.changePasswordErrorMessage,
    isStandaloneDeveloper: selectIsStandaloneDeveloper(state),
  };
};

const UserMenuContainer = connect(mapStateToProps, mapDispatchToProps)(UserMenu);

UserMenuContainer.propTypes = {
  userActions: PropTypes.shape({
    loadUser: PropTypes.func,
    logout: PropTypes.func,
    changePassword: PropTypes.func,
    resetPasswordStatus: PropTypes.func,
  }),
};

export default UserMenuContainer;
