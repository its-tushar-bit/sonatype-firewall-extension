/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

import dashboardResultsModule from './results/module';
import dashboardUtilsModule from './utils/dashboard.utils.module';
import dashboardFilterModule from './filter/module';
import angularCommonModule from '../util/AngularCommon';
import utilityModule from '../utility/utility.module';
import storesModule from '../util/Stores';
import dashboardReducer from './dashboardReducer';
import ComponentModule from './ComponentController';
import ComponentDisplayModule from '../ComponentDisplay/module';
import template from './dashboard.view.html';
import componentTemplate from './component.html';

var dashboardModule = angular
  .module('dashboard.module', [
    'ui.router',
    storesModule.name,
    angularCommonModule.name,
    ComponentModule.name,
    ComponentDisplayModule.name,
    dashboardUtilsModule.name,
    utilityModule.name,
    dashboardFilterModule.name,
    dashboardResultsModule.name,
  ])
  .value('dashboardReducer', dashboardReducer); // add to angular so we can test it

export default dashboardModule;

// To avoid hacking dependency order, states must be declared with their parent.
// Fixed https://github.com/angular-ui/ui-router/pull/492
dashboardModule.config([
  '$stateProvider',
  '$urlRouterProvider',
  function ($stateProvider, $urlRouterProvider) {
    $stateProvider
      .state('dashboard', {
        url: '/dashboard',
        abstract: true,
        template,
        data: {
          title: 'Dashboard',
          crumb: 'Dashboard',
        },
      })
      .state('dashboard.overview', {
        abstract: true,
        component: 'dashboardResultsContainer',
      })
      .state('dashboard.overview.violations', {
        url: '/violations',
        component: 'violations',
        data: {
          title: 'Dashboard - Violations',
          exportTitle: 'Violations',
        },
      })
      .state('dashboard.overview.components', {
        url: '/components',
        component: 'components',
        data: {
          title: 'Dashboard - Components',
          exportTitle: 'Components',
        },
      })
      .state('dashboard.overview.applications', {
        url: '/applications',
        component: 'applications',
        data: {
          title: 'Dashboard - Applications',
          exportTitle: 'Applications',
        },
      })
      .state('dashboard.component', {
        url: '/component/{hash}',
        controller: 'componentController',
        template: componentTemplate,
        data: {
          crumb: 'Component Details',
        },
      });

    $urlRouterProvider.when('/dashboard/newest-risk', '/dashboard/violations');
  },
]);
