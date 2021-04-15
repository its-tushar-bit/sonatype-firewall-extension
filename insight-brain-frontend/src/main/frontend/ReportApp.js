/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
import reportViolationsModule from './report/ReportViolationsController';
import template from './report/violations/report-list.html';

export default angular.module(
  'ReportModule',
  ['ui.router', reportViolationsModule.name],
  [
    '$stateProvider',
    '$urlRouterProvider',
    function ($stateProvider, $urlRouterProvider) {
      $urlRouterProvider.when('/reports', '/reports/violations');
      $stateProvider.state('violations', {
        url: '/reports/violations',
        template,
        controller: 'ReportViolationsController',
        controllerAs: 'vm',
        data: {
          title: 'Report Violations',
        },
      });
    },
  ]
);
