/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

import dashboardUtilsModule from './utils/dashboard.utils.module';
import dashboardDataService from './services/dashboard.data.service';
import dashboardFilterService from './services/dashboard.filter.service';
import policyTrendController from './results/PolicyTrendController';
import violationsTableRow from './results/violationsTableRow';
import dashboardResultsController from './results/dashboard.results.controller';
import dashboardFilterController from './dashboard.filter.controller';
import dashboardFilterDimension from './dashboard.filter.dimension.directive';
import dashboardFilterRadioDimension from './dashboard.filter.dimension.radio.directive';
import deleteFiltersModalController from './manage.filter.menu/delete.filters.modal.controller';
import deleteFiltersModal from './manage.filter.menu/delete.filters.modal';
import saveFilterModalController from './manage.filter.menu/save.filter.modal.controller';
import saveFilterModal from './manage.filter.menu/save.filter.modal';
import manageFilterMenu from './manage.filter.menu/manage.filter.menu';

var dashboardModule = angular.module('dashboard.module',
    [
      'ui.router', 'Stores', 'AngularCommon', 'ComponentModule', 'ComponentDisplay', dashboardUtilsModule.name,
      'utility'
    ])
    .service('dashboard.data.service', dashboardDataService)
    .service('dashboard.filter.service', dashboardFilterService)

    // dashboard results
    .controller('PolicyTrendController', policyTrendController)
    .component('violationsTableRow', violationsTableRow)
    .controller('dashboard.results.controller', dashboardResultsController)

    // dashboard filter
    .controller('dashboard.filter.controller', dashboardFilterController)
    .directive('dashboardFilterDimension', dashboardFilterDimension)
    .directive('dashboardFilterRadioDimension', dashboardFilterRadioDimension)

    // manage filter modal
    .controller('delete.filters.modal.controller', deleteFiltersModalController)
    .service('delete.filters.modal', deleteFiltersModal)
    .controller('save.filter.modal.controller', saveFilterModalController)
    .service('save.filter.modal', saveFilterModal)
    .component('manageFilterMenu', manageFilterMenu);

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
      filter: {
        templateUrl: 'dashboard/dashboard.filter.html?' + clmBuildTimestamp,
        controller: 'dashboard.filter.controller as vm'
      }
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
