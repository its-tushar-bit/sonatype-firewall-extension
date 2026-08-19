/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';

import RoleEditor from './RoleEditor';
import * as roleEditorAction from './roleEditorActions';
import { load as loadRolesAction } from './../rolesActions';
import { stateGo } from '../../reduxUiRouter/routerActions';

function mapStateProp({ roleEditor, roles: { roles }, loadRoles, router }) {
  return {
    ...roleEditor,
    roles,
    loadRoles,
    router,
  };
}

export default connect(mapStateProp, { ...roleEditorAction, loadRoles: loadRolesAction, stateGo })(RoleEditor);
