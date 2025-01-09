/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import ComponentDisplayModule from '../ComponentDisplay/module';
import template from './dashboard.view.html';
import DashboardHeaderContainer from './results/DashboardHeaderContainer';

import dashboardResultsContainer from './results/dashboardResultsContainer';
import DashboardViolationsContainer from './results/violations/DashboardViolationsContainer';
import DashboardComponentsContainer from './results/components/DashboardComponentsContainer';
import ComponentRisk from './results/componentRisk/ComponentRisk';
import DashboardApplicationsContainer from './results/applications/DashboardApplicationsContainer';
import DashboardWaivers from './results/waivers/DashboardWaivers';

var dashboardModule = angular
  .module('dashboard.module', ['ui.router', ComponentDisplayModule.name])
  .component('dashboardResultsContainer', dashboardResultsContainer)
  .component('dashboardHeader', iqReact2Angular(DashboardHeaderContainer, [], ['$ngRedux', '$state']))
  .component('violations', iqReact2Angular(DashboardViolationsContainer, [], ['$ngRedux']))
  .component('components', iqReact2Angular(DashboardComponentsContainer, [], ['$ngRedux']))
  .component('applications', iqReact2Angular(DashboardApplicationsContainer, [], ['$ngRedux', '$state']))
  .component('waivers', iqReact2Angular(DashboardWaivers, [], ['$ngRedux', '$state']))
  .component('component', iqReact2Angular(ComponentRisk, [], ['$ngRedux', '$state']));

export default dashboardModule;

// To avoid hacking dependency order, states must be declared with their parent.
// Fixed https://github.com/angular-ui/ui-router/pull/492
dashboardModule.config([
  '$stateProvider',
  '$urlServiceProvider',
  function ($stateProvider, $urlServiceProvider) {
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
      .state('dashboard.overview.waivers', {
        url: '/waivers',
        component: 'waivers',
        data: {
          title: 'Dashboard - Waivers',
          exportTitle: 'Waivers',
        },
      })
      .state('dashboard.component', {
        url: '/component/{hash}',
        component: 'component',
        data: {
          crumb: 'Component Details',
        },
      });

    $urlServiceProvider.rules.when('/dashboard/newest-risk', (matchValues, _urlParts, router) =>
      router.stateService.go('dashboard.overview.violations', matchValues)
    );

    $urlServiceProvider.rules.when('/repositories/quarantinedComponent/{token}', (matchValues, _urlParts, router) =>
      router.stateService.go('firewall.quarantinedComponentReport', matchValues)
    );
  },
]);
