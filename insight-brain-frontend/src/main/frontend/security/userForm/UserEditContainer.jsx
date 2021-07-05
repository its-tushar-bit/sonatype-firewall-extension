/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { stateGo } from '../../reduxUiRouter/routerActions';
import { loadUserById, setFirstName, setLastName, setEmail, update, resetForm, deleteUser } from './userFormActions';

import UserEdit from './UserEdit';

export default connect(
  ({ userForm, router }) => ({
    ...userForm,
    username: userForm.selectedUserServerData.username,
    router,
  }),
  {
    loadUserById,
    setFirstName,
    setLastName,
    setEmail,
    deleteUser,
    resetForm,
    update,
    stateGo,
  }
)(UserEdit);
