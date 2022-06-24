/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import angularCommonModule from '../utilAngular/AngularCommon';
import storesModule from '../utilAngular/Stores';
import ComponentModule from './ComponentController';
import ComponentDisplayModule from '../ComponentDisplay/module';
import template from './dashboard.view.html';
import componentTemplate from './component.html';
import dashboardResultsActionsModule from './results/dashboardResultsActions';
import DashboardHeaderContainer from './results/DashboardHeaderContainer';

import dashboardResultsContainer from './results/dashboardResultsContainer';
import DashboardViolationsContainer from './results/violations/DashboardViolationsContainer';
import DashboardComponentsContainer from './results/components/DashboardComponentsContainer';
import DashboardApplicationsContainer from './results/applications/DashboardApplicationsContainer';

var dashboardModule = angular
  .module('dashboard.module', [
    'ui.router',
    storesModule.name,
    angularCommonModule.name,
    ComponentModule.name,
    ComponentDisplayModule.name,
    dashboardResultsActionsModule.name,
  ])
  .component('dashboardResultsContainer', dashboardResultsContainer)
  .component('dashboardHeader', iqReact2Angular(DashboardHeaderContainer, [], ['$ngRedux', '$state']))
  .component('violations', iqReact2Angular(DashboardViolationsContainer, [], ['$ngRedux']))
  .component('components', iqReact2Angular(DashboardComponentsContainer, [], ['$ngRedux']))
  .component('applications', iqReact2Angular(DashboardApplicationsContainer, [], ['$ngRedux', '$state']));

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
