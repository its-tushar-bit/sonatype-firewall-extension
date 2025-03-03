/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import SonatypeDeveloperPage from 'MainRoot/development/developmentDashboard/SonatypeDeveloperPage';
import { ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE } from 'MainRoot/utility/services/routeStateUtilService';

export const SECTIONS = {
  OVERVIEW: 'overview',
  CICD: 'cicd',
  SCM: 'scm',
  ISSUE_TRACKING: 'issuetracking',
  IDE: 'ide',
};

const developerModule = angular
  .module('developerModule', ['ngRedux'])
  .component('sonatypeDeveloperPage', iqReact2Angular(SonatypeDeveloperPage, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider, $urlServiceProvider) {
  $stateProvider
    .state('developer', {
      url: '/developer',
      abstract: true,
    })
    .state('developer.dashboard', {
      url: '/dashboard',
      component: 'sonatypeDeveloperPage',
      redirectTo: `developer.dashboard.${SECTIONS.OVERVIEW}`,
      data: {
        title: 'Sonatype Developer - Dashboard',
        authenticationRequired: true,
      },
    })
    .state(`developer.dashboard.${SECTIONS.OVERVIEW}`, {
      url: '/overview',
      data: {
        title: 'Overview',
      },
    })
    .state(`developer.dashboard.${SECTIONS.CICD}`, {
      url: '/ci-cd',
    })
    .state(`developer.dashboard.${SECTIONS.SCM}`, {
      url: '/scm',
    })
    .state(`developer.dashboard.${SECTIONS.ISSUE_TRACKING}`, {
      url: '/issue-tracking',
    })
    .state(`developer.dashboard.${SECTIONS.IDE}`, {
      url: '/ide',
    })
    .state('developer.priorities', {
      url: '/priorities',
      component: 'reportsPage',
      data: {
        title: 'Priorities',
      },
    })
    .state('developer.advancedSearch', {
      url: '/advancedSearch?search',
      component: 'advancedSearch',
      data: {
        title: 'Sonatype Developer - Advanced Search',
        authenticationRequired: true,
      },
    })
    .state('developer.addWaiver', {
      component: 'addWaiverPage',
      data: {
        title: 'Sonatype Developer - Add Waiver',
        isDirty: ['addWaiver', 'isDirty'],
      },
      url: '/addWaiver/{violationId}?comments&reasonId',
    })
    .state('developer.requestWaiver', {
      component: 'requestWaiverPage',
      data: {
        title: 'Sonatype Developer - Request Waiver',
        isDirty: ['requestWaiver', 'isDirty'],
      },
      url: '/requestWaiver/{violationId}',
    })
    .state('developer.api', {
      url: '/api',
      data: {
        title: 'API',
        authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
      },
      component: 'apiPage',
    });

  // Redirect from old URL to new URL
  $urlServiceProvider.rules.when('/developer/reports', (matchValues, _urlParts, router) =>
    router.stateService.go('developer.priorities', matchValues)
  );
}

routes.$inject = ['$stateProvider', '$urlServiceProvider'];

export default developerModule;
