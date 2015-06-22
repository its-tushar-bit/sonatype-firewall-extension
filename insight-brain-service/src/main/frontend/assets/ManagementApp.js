/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular, clmBuildTimestamp */
(function() {
  'use strict';
  angular.module('managementApp',
    ['MainModule', 'OrganizationModule', 'ApplicationModule', 'Configuration', 'UserModule', 'RoleModule', 'LdapConfiguration']);
}());

(function() {
  'use strict';

  var managementModule = angular.module('ManagementModule', ['ui.router'], ['$stateProvider', function($stateProvider) {
    $stateProvider.state('management', {
      url: '/management',
      templateUrl: '../assets/management.html?' + clmBuildTimestamp,
      controller: 'ManagementController',
      data : {
        title : 'Management'
      }
    });
  }]);

  managementModule.controller('ManagementController', ['$scope', '$state', 'commonCodeFactory', function($scope, $state, commonCodeFactory) {
    $scope.$state = $state;
    $scope.syncAlerts = [];
    var error = commonCodeFactory.getEncodedQueryString('errorMessage');
    if (error) {
      $scope.syncAlerts.push({ type: 'error', msg: decodeURIComponent(error) });
    }
  }]);
}());
