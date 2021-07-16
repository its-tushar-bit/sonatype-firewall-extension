/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import {
  loadUserById,
  setFirstName,
  setLastName,
  setEmail,
  update,
  resetForm,
  resetInitialNewPasswordValue,
  deleteUser,
  resetPassword,
} from '../usersActions';

import UserEdit from './UserEdit';

export default connect(
  ({ userConfiguration, router }) => ({
    ...userConfiguration,
    username: userConfiguration.selectedUserServerData.username,
    router,
  }),
  {
    loadUserById,
    setFirstName,
    setLastName,
    setEmail,
    deleteUser,
    resetForm,
    resetInitialNewPasswordValue,
    resetPassword,
    update,
    stateGo,
  }
)(UserEdit);
