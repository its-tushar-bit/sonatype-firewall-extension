/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';
  angular.module('reportApp', ['ReportModule', 'Report', 'ngRoute'], ['$urlRouterProvider', function($urlRouterProvider) {
    $urlRouterProvider.when('', '/reports/triage');
  }]);
}());

(function() {
  'use strict';

  var reportModule = angular.module('ReportModule', ['ui.router', 'DashboardModule', 'DashboardHeader', 'ReportViolations', 'ReportTrending'],
    ['$stateProvider', function($stateProvider) {
      $stateProvider.state('reports', {
        url: '/reports',
        templateUrl: '../assets/management.html?' + clmBuildTimestamp,
        controller: 'ReportsController'
      }).state('reports.triage', {
          url: '/triage',
          templateUrl: '../report-assets/violations/report-list.html?' + clmBuildTimestamp,
          parent: 'reports',
          controller: 'ReportViolationsController'
        }).state('reports.reporting', {
          url: '/reporting',
          templateUrl: '../report-assets/trending/trending-report.html?' + clmBuildTimestamp,
          parent: 'reports',
          controller: 'TrendingReportController'
        });
    }]);

  reportModule.controller('ReportsController', ['$scope', '$state', function($scope, $state) {
    $scope.$state = $state;

    $scope.panes = [
      {
        name: 'Triage',
        state: 'reports/triage',
        isEnabled: true
      },
      {
        name: 'Reporting',
        state: 'reports/reporting',
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
  }]);
}());
