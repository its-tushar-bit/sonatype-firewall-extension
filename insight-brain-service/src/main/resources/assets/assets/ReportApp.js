/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var reportApp = angular.module('reportApp', ['ReportModule', 'Report'],
      [
        '$urlRouterProvider',
        function($urlRouterProvider) {
          $urlRouterProvider.when('', '/reports');
        }
      ]);
}());

(function() {
  'use strict';

  var reportModule = angular.module('ReportModule', ['ui.router', 'DashboardModule', 'ReportViolations'],
          ['$stateProvider', function($stateProvider) {
            $stateProvider.state('reports', {
              url: '/reports',
              templateUrl: '../assets/management.html?' + clmBuildTimestamp,
              controller: 'ReportsController'
            }).state('reports.violations', {
              url: '/violations',
              templateUrl: '../report-assets/violations/report-list.html?' + clmBuildTimestamp,
              parent: 'reports',
              controller: 'ReportViolationsController'
            });
          }]);

  reportModule.controller('ReportsController', ['$scope', '$state', function($scope, $state) {
    $scope.$state = $state;

    $scope.panes = [
      {
        name: 'Violations',
        state: 'reports/violations',
        isEnabled: true
      }
    ];

    for (var i = 0; i < $scope.panes.length; i++) {
      var normalizedState = $scope.panes[i].state.replace('/', '.');
      if ($scope.$state.current.name.indexOf(normalizedState) !== -1) {
        $scope.$state.selectedPane = $scope.panes[i];
        break;
      }
    }

    $scope.$watch('$state.current.name', function() {
      if ($state.current.name === 'reports') {
        $state.transitionTo('reports.violations');
      }
    });
  }]);
}());
