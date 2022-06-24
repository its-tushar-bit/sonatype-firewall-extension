/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import RoleListContainer from './roleList/RoleListContainer';
import RoleEditorContainer from './roleEditor/RoleEditorContainer';

const module = angular
  .module('RoleModule', ['ui.router', 'ui.router.state'])
  .component('roles', iqReact2Angular(RoleListContainer, [], ['$ngRedux', '$state']))
  .component('roleEditor', iqReact2Angular(RoleEditorContainer, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('rolesList', {
      url: '/roles',
      component: 'roles',
      data: {
        title: 'Roles',
      },
    })
    .state('addRole', {
      url: '/roles/_new_',
      component: 'roleEditor',
      data: {
        title: 'Create a role',
        isDirty: ['roleEditor', 'isDirty'],
      },
    })
    .state('editRole', {
      url: '/roles/{roleId}',
      component: 'roleEditor',
      data: {
        title: 'Edit a Role',
        isDirty: ['roleEditor', 'isDirty'],
      },
    });
}

routes.$inject = ['$stateProvider'];

export default module;
