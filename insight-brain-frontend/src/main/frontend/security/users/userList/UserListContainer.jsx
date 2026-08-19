/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { selectTenantMode } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { loadListPage, deleteUser } from '../usersActions';
import UserList from './UserList';

export default connect(
  (state) => {
    const { userConfiguration } = state,
      tenantMode = selectTenantMode(state);

    return {
      ...pick(
        ['users', 'loading', 'loadError', 'currentUsername', 'deleteError', 'deleteMaskState'],
        userConfiguration
      ),
      tenantMode,
    };
  },
  {
    stateGo,
    loadListPage,
    deleteUser,
  }
)(UserList);
