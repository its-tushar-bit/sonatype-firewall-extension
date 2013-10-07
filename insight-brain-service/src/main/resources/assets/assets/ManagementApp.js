/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var managementApp = angular.module('managementApp',
      ['DashboardModule', 'OrganizationModule', 'ApplicationModule', 'Configuration', 'UserModule', 'LdapConfiguration'],
      [
        '$urlRouterProvider',
        function($urlRouterProvider) {
          $urlRouterProvider.when('', '/management/application');
        }
      ]);
}());

(function() {
  'use strict';

  var managementModule = angular.module('ManagementModule', ['ui.router'], ['$stateProvider', function($stateProvider) {
    $stateProvider.state('management', {
      url: '/management',
      templateUrl: '../assets/management.html?' + clmBuildTimestamp,
      controller: 'ManagementController'
    });
  }]);

  managementModule.controller('ManagementController', function($scope, $state, commonCodeFactory) {
    $scope.$state = $state;

    $scope.managementPanes = [
      {
        name: 'Organizations',
        state: 'management/organization',
        isEnabled: true
      },
      {
        name: 'Applications',
        state: 'management/application',
        isEnabled: true
      },
      {
        name: 'Security',
        state: 'management/security',
        isEnabled: true
      },
      {
        name: 'Configuration',
        state: 'management/configuration',
        isEnabled: true
      }
    ];

    for (var i = 0; i < $scope.managementPanes.length; i++) {
      var normalizedState = $scope.managementPanes[i].state.replace('/', '.');
      if ($scope.$state.current.name.indexOf(normalizedState) !== -1) {
        $scope.$state.selectedPane = $scope.managementPanes[i];
        break;
      }
    }

    $scope.$watch('$state.current.name', function() {
      if ($state.current.name === 'management') {
        $state.transitionTo('management.application');
      }
    });

    $scope.syncAlerts = [];
    var error = commonCodeFactory.getEncodedQueryString('errorMessage');
    if (error) {
      $scope.syncAlerts.push({ type: 'error', msg: decodeURIComponent(error) });
    }
  });
}());
