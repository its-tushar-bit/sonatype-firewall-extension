/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import withRouterStateProvider from '../../reactAdapter/RouterStateProvider';
import UserAddContainer from './userConfiguration/UserAddContainer';
import UserEditContainer from './userConfiguration/UserEditContainer';
import UserListContainer from './userList/UserListContainer';

export const UserModule = angular
  .module('UserModule', [])
  .component(
    'users',
    react2angular(withStoreProvider(withRouterStateProvider(UserListContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'createUser',
    react2angular(withStoreProvider(withRouterStateProvider(UserAddContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'editUser',
    react2angular(withStoreProvider(withRouterStateProvider(UserEditContainer)), [], ['$ngRedux', '$state'])
  )
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
