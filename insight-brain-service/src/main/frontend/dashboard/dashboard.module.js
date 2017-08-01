/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

import dashboardUtilsModule from './utils/dashboard.utils.module';
import dashboardDataService from './services/dashboard.data.service';
import policyTrendController from './results/PolicyTrendController';
import violationsTableRow from './results/violationsTableRow';
import dashboardResultsController from './results/dashboard.results.controller';
import dashboardFilterModule from './filter/module';

var dashboardModule = angular.module('dashboard.module',
    [
      'ui.router', 'Stores', 'AngularCommon', 'ComponentModule', 'ComponentDisplay', dashboardUtilsModule.name,
      'utility', dashboardFilterModule.name
    ])
    .service('dashboard.data.service', dashboardDataService)

    // dashboard results
    .controller('PolicyTrendController', policyTrendController)
    .component('violationsTableRow', violationsTableRow)
    .controller('dashboard.results.controller', dashboardResultsController);

export default dashboardModule;

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
    url: '?timeFilterFeature', // query parameter feature flag for Time-based filter control
    abstract: true,
    views: {
      content: {
        templateUrl: 'dashboard/results/dashboard.results.html?' + clmBuildTimestamp,
        controller: 'dashboard.results.controller'
      },
      filter: 'dashboardFilter'
    }
  }).state('dashboard.overview.violations', {
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
    url: '/component/{hash}',
    controller: 'componentController',
    templateUrl: 'dashboard/component.html?' + clmBuildTimestamp,
    data: {
      crumb: 'Component Details'
    }
  });

  $urlRouterProvider.when('/dashboard/newest-risk', '/dashboard/violations');
}]);
