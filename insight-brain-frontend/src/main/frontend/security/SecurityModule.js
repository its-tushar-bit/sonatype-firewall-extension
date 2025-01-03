/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import permissionServiceModule from '../utilAngular/PermissionService';

export const SecurityModule = angular.module(
  'SecurityModule',
  ['ui.router', permissionServiceModule.name],
  [
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider
        .state('administrators', {
          component: 'administratorsConfig',
          url: '/administrators',
          data: {
            title: 'Administrator Config',
          },
        })
        .state('administratorsEdit', {
          component: 'administratorsEdit',
          url: '/administrators/{roleId}',
          data: {
            title: 'Administrator Edit',
            isDirty: ['administratorsConfig', 'isDirty'],
          },
        });
    },
  ]
);
