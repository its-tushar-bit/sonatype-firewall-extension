/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var dashboardModule = angular.module('dashboard.module', ['ui.router', 'Stores', 'AngularCommon', 'ComponentModule', 'ComponentDisplay',
                                      'dashboard.utils', 'utility']);

  // To avoid hacking dependency order, states must be declared with their parent.
  // Fixed https://github.com/angular-ui/ui-router/pull/492
  dashboardModule.config(['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {

    $stateProvider.state('dashboard', {
      url: '/dashboard',
      abstract: true,
      templateUrl: 'dashboard/dashboard.view.html?' + clmBuildTimestamp,
      data: {
        title: 'Dashboard',
        crumb: 'Dashboard'
      }
    }).state('dashboard.overview', {
      parent: 'dashboard',
      url: '?timeFilterFeature', // query parameter feature flag for Time-based filter control
      abstract: true,
      views: {
        content: {
          templateUrl: 'dashboard/results/dashboard.results.html?' + clmBuildTimestamp,
          controller: 'dashboard.results.controller'
        },
        filter: {
          templateUrl: 'dashboard/dashboard.filter.html?' + clmBuildTimestamp,
          controller: 'dashboard.filter.controller as vm'
        }
      }
    }).state('dashboard.overview.violations', {
      parent: 'dashboard.overview',
      url: '/violations',
      views: {
        'dashboard-results': {
          templateUrl: 'dashboard/results/violations.html?' + clmBuildTimestamp
        }
      },
      data: {
        title: 'Violations'
      }
    }).state('dashboard.overview.components', {
      parent: 'dashboard.overview',
      url: '/components',
      views: {
        'dashboard-results': {
          templateUrl: 'dashboard/results/components.html?' + clmBuildTimestamp
        }
      },
      data: {
        title: 'Components'
      }
    }).state('dashboard.overview.applications', {
      parent: 'dashboard.overview',
      url: '/applications',
      views: {
        'dashboard-results': {
          templateUrl: 'dashboard/results/applications.html?' + clmBuildTimestamp
        }
      },
      data: {
        title: 'Applications'
      }
    }).state('dashboard.component', {
      parent: 'dashboard',
      url: '/component/{hash}',
      controller: 'componentController',
      templateUrl: 'dashboard/component.html?' + clmBuildTimestamp,
      data: {
        crumb: 'Component Details'
      }
    });

    $urlRouterProvider.when('/dashboard/newest-risk', '/dashboard/violations');
  }]);
}());
