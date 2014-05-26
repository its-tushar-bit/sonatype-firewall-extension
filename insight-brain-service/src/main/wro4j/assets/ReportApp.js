/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  angular.module('ReportModule', ['ui.router', 'MainModule', 'MainHeader', 'ReportViolations'],
    ['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {
      $urlRouterProvider.when('/reports', '/reports/violations');
      $stateProvider.state('violations', {
        url: '/reports/violations',
        templateUrl: '../report-assets/violations/report-list.html?' + clmBuildTimestamp,
        controller: 'ReportViolationsController'
      });
    }]);
}());
