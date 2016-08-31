/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var dashboardModule = angular.module('dashboard.module');

  // To avoid hacking dependency order, states must be declared with their parent.
  // Fixed https://github.com/angular-ui/ui-router/pull/492
  dashboardModule.config(['$stateProvider', '$urlRouterProvider', function($stateProvider,  $urlRouterProvider) {

    $stateProvider.state('dashboard-new', {
      url: '/dashboard-new',
      abstract: true,
      templateUrl: 'dashboard-new/dashboard.view.html?' + clmBuildTimestamp,
      data: {
        title: 'Dashboard'
      }
    }).state('dashboard-new.overview', {
      parent: 'dashboard-new',
      url: '',
      abstract: true,
      views: {
        content: {
          templateUrl: 'dashboard-new/dashboard.overview.html?' + clmBuildTimestamp,
          controller: 'dashboard.controller'
        },
        filter: {
          templateUrl: 'dashboard/dashboard.filter.html?' + clmBuildTimestamp,
          controller: 'dashboard.filter.controller as vm'
        }
      }
    }).state('dashboard-new.overview.violations', {
      parent: 'dashboard-new.overview',
      url: '/violations',
      views: {
        'dashboard-results': {
          templateUrl: 'dashboard-new/violations.html?' + clmBuildTimestamp
        }
      }
    }).state('dashboard-new.overview.components', {
      parent: 'dashboard-new.overview',
      url: '/components',
      views: {
        'dashboard-results': {
          templateUrl: 'dashboard-new/components.html?' + clmBuildTimestamp
        }
      }
    }).state('dashboard-new.overview.applications', {
      parent: 'dashboard-new.overview',
      url: '/applications',
      views: {
        'dashboard-results': {
          templateUrl: 'dashboard-new/applications.html?' + clmBuildTimestamp
        }
      }
    }).state('dashboard-new.component', {
      parent: 'dashboard-new',
      url: '/component/{hash}',
      controller: 'componentController',
      templateUrl: 'dashboard/component.html?' + clmBuildTimestamp
    });

    $urlRouterProvider.when('/dashboard-new/newest-risk', '/dashboard-new/violations');
  }]);
}());
