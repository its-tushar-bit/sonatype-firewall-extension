/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';
import { stateGo } from '../../reduxUiRouter/routerActions';
import { load } from '../rolesActions';
import RoleList from './RoleList';

export default connect(
  ({ roles }) => ({ ...pick(['readOnly', 'isAuthorized', 'loading', 'loadError', 'roles'], roles) }),
  { stateGo, load }
)(RoleList);
