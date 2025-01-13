/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import SonatypeDeveloperPage from 'MainRoot/development/developmentDashboard/SonatypeDeveloperPage';

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

function routes($stateProvider) {
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
    .state('developer.reports', {
      url: '/reports',
      component: 'reportsPage',
      data: {
        title: 'Reports',
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
    });
}

routes.$inject = ['$stateProvider'];

export default developerModule;
