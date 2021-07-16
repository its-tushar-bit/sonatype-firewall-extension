/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import angularCommonModule from '../util/AngularCommon';
import permissionServiceModule from '../util/PermissionService';
import ApplicationSecurityModule from '../policy/AppSecurityController';
import administratorsTemplate from '../policy/components/app-security/app-security.html';

export const SecurityModule = angular.module(
  'SecurityModule',
  ['ui.router', angularCommonModule.name, ApplicationSecurityModule.name, permissionServiceModule.name],
  [
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('administrators', {
        url: '/administrators',
        template: administratorsTemplate,
        data: {
          title: 'Administrators',
        },
        controller: 'AppSecurityController',
        resolve: {
          isAuthorized: [
            'PermissionService',
            function (PermissionService) {
              return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
            },
          ],
        },
      });
    },
  ]
);
