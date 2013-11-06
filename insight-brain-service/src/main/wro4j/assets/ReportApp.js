/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';
  angular.module('reportApp', ['ReportModule', 'Report'],
    [
      '$urlRouterProvider',
      function($urlRouterProvider) {
        $urlRouterProvider.when('', '/reports/violations');
      }
    ]);
}());

(function() {
  'use strict';

  var reportModule = angular.module('ReportModule', ['ui.router', 'DashboardModule', 'ReportViolations', 'ReportTrending'],
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
        }).state('reports.trending', {
          url: '/trending',
`          templateUrl: '../report-assets/trending/trending-report.html?' + clmBuildTimestamp,
          parent: 'reports',
          controller: 'TrendingReportController'
        });
    }]);

  reportModule.controller('ReportsController', ['$scope', '$state', function($scope, $state) {
    $scope.$state = $state;

    $scope.panes = [
      {
        name: 'Violations',
        state: 'reports/violations',
        isEnabled: true
      },
      {
        name: 'Trending',
        state: 'reports/trending',
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
