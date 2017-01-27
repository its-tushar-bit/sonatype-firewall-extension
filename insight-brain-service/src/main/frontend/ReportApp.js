/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  angular.module('ReportModule', ['ui.router', 'ReportViolations'],
    ['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {
      $urlRouterProvider.when('/reports', '/reports/violations');
      $stateProvider.state('violations', {
        url: '/reports/violations',
        templateUrl: 'report/violations/report-list.html?' + clmBuildTimestamp,
        controller: 'ReportViolationsController',
        data : {
          title : 'Report Violations'
        }
      });
    }]);
}());
