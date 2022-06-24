/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import UserAddContainer from './userConfiguration/UserAddContainer';
import UserEditContainer from './userConfiguration/UserEditContainer';
import UserListContainer from './userList/UserListContainer';

export const UserModule = angular
  .module('UserModule', [])
  .component('users', iqReact2Angular(UserListContainer, [], ['$ngRedux', '$state']))
  .component('createUser', iqReact2Angular(UserAddContainer, [], ['$ngRedux', '$state']))
  .component('editUser', iqReact2Angular(UserEditContainer, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('users', {
      url: '/users',
      component: 'users',
      data: {
        title: 'Users',
      },
    })
    .state('createUser', {
      url: '/users/_new_',
      component: 'createUser',
      data: {
        title: 'Add New User',
        isDirty: ['userConfiguration', 'isDirty'],
      },
    })
    .state('editUser', {
      url: '/users/{userId}',
      component: 'editUser',
      data: {
        title: 'Edit User',
        isDirty: ['userConfiguration', 'isDirty'],
      },
    });
}

routes.$inject = ['$stateProvider'];

export default UserModule;
