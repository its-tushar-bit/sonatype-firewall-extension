/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { selectTenantMode } from '../../../productFeatures/productFeaturesSelectors';
import {
  loadCreateUserPage,
  save,
  setFirstName,
  setLastName,
  setEmail,
  setUserName,
  setPassword,
  setMatchPassword,
  resetForm,
} from '../usersActions';
import UserAdd from './UserAdd';

export default connect(
  (state) => {
    const { userConfiguration } = state,
      tenantMode = selectTenantMode(state);
    return {
      tenantMode,
      ...pick(
        ['loading', 'loadError', 'saveError', 'submitMaskState', 'isDirty', 'validationError', 'inputFields'],
        userConfiguration
      ),
    };
  },
  {
    loadCreateUserPage,
    save,
    setFirstName,
    setLastName,
    setEmail,
    setUserName,
    setPassword,
    setMatchPassword,
    resetForm,
    stateGo,
  }
)(UserAdd);
