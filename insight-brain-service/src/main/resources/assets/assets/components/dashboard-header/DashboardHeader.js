/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";

  var module = angular.module('DashboardHeader', ['UserControls']);

  module.controller('dashboardHeaderController', ['$scope', '$state', '$window', 'CLMLocations', '$http', '$rootScope', function($scope, $state, $window, CLMLocations, $http, $rootScope) {
    function switchDashboard() {
      for ( var i = 0; i < $scope.availableDashboards.length; i++) {
        if ($window.location.href && $window.location.href.indexOf($scope.availableDashboards[i].selector) !== -1) {
          $scope.selectedDashboard = $scope.availableDashboards[i];
          break;
        }
      }
    }

    $scope.$state = $state;
    $scope.availableDashboards = [{
      name: 'Management',
      icon: 'management',
      href: 'index.html#/management/application',
      selector: '#/management'
    }, {
      name: 'Reports',
      icon: 'reports',
      href: 'reports.html#/reports/violations',
      selector: '#/reports'
    }];

    $scope.$watch('$state.current.name', switchDashboard);
    switchDashboard();
  }]);

  module.directive('dashboardHeader', function () {
    return {
      restrict: 'A',
      controller: 'dashboardHeaderController',
      templateUrl : '../assets/components/dashboard-header/dashboard-header.html?' + clmBuildTimestamp
    };
  });
}());