/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function(angular) {
  'use strict';

  angular.module('repository.manager.module', ['ui.router', 'CLMLocation', 'AngularCommon'])
      .config(['$stateProvider', function($stateProvider) {
        $stateProvider.state('management.repositories', {
          parent: 'management',
          url: '/repositories',
          controller: angular.noop,
          templateUrl: '../assets/repository/manager/state/repositories.manager.view.html?' + clmBuildTimestamp
        }).state('management.repositories.configuration', {
          parent: 'management.repositories',
          url: '/configuration',
          controller: 'repository.manager.configuration.controller',
          controllerAs: 'vm',
          templateUrl: '../assets/repository/manager/state/repository.configuration.view.html?' + clmBuildTimestamp
        }).state('management.repositories.security', {
          parent: 'management.repositories',
          url: '/security',
          controller: 'AppSecurityController',
          templateUrl: '../policy-assets/components/app-security/app-security.html?' + clmBuildTimestamp,
          resolve : {
            isAuthorized : function () {
              return true;
            }
          }
        });
      }]);
}(angular));
