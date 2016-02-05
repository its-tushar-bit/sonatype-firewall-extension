/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, clmBuildTimestamp */
(function () {
  'use strict';

  function ProprietaryConfigurationModule ($stateProvider) {
    $stateProvider.state('proprietarycomponents', {
      url: '/proprietarycomponents',
      controller: 'proprietary.configuration.controller as vm',
      templateUrl: 'configuration/components/proprietary.html?' + clmBuildTimestamp,
      data : {
        title : 'Proprietary Configuration'
      },
      resolve : {
        'isAuthorized' : ['PermissionService', function (PermissionService) {
          return PermissionService.isAuthorized(['MANAGE_PROPRIETARY'], true);
        }]
      }
    });
  }

  ProprietaryConfigurationModule.$inject = ['$stateProvider'];

  angular.module('proprietary.configuration.module',
      ['ui.router', 'ProductLicense', 'PermissionServiceModule', 'AngularCommon', 'Validators'],
      ProprietaryConfigurationModule);
}());
