/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import PrioritiesPage from 'MainRoot/development/prioritiesPage/PrioritiesPage';

const prioritiesPageModule = angular
  .module('prioritiesPageModule', ['ngRedux'])
  .component('prioritiesPage', iqReact2Angular(PrioritiesPage, [], ['$ngRedux', '$state']))
  .config(routes);

const url = '/development/priorities/{publicAppId}/{scanId}';

function routes($stateProvider, $urlRouterProvider) {
  $stateProvider
    .state('prioritiesPage', {
      url,
      component: 'prioritiesPage',
      data: {
        title: 'Priorities',
      },
    })
    .state('prioritiesPageContainer', {
      url: '/development/priorities/report/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
    })
    .state('prioritiesPageContainer.policy', {
      url: '/policy',
      component: 'applicationReport',
      data: {
        title: 'Application Report',
      },
    })
    .state('prioritiesPageContainer.rawData', {
      url: '/raw',
      component: 'applicationReportRawData',
      data: {
        title: 'Application Report',
      },
    })
    .state('prioritiesPageContainer.vulnerabilities', {
      url: '/vulnerabilities',
      component: 'applicationReportVulnerabilities',
      data: {
        title: 'Application Report Vulnerabilities List',
      },
    })
    .state('prioritiesPageContainer.dependencyTree', {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state('prioritiesPageContainer.componentDetailsFromReport', {
      url: '/componentDetailsFromReport/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('prioritiesPageContainer.componentDetails', {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('prioritiesPageContainer.componentDetails.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('prioritiesPageContainer.componentDetails.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('prioritiesPageContainer.componentDetails.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('prioritiesPageContainer.componentDetails.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('prioritiesPageContainer.componentDetails.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('prioritiesPageContainer.componentDetails.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('prioritiesPageContainer.componentDetailsFromReport.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('prioritiesPageContainer.componentDetailsFromReport.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('prioritiesPageContainer.componentDetailsFromReport.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('prioritiesPageContainer.componentDetailsFromReport.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('prioritiesPageContainer.componentDetailsFromReport.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('prioritiesPageContainer.componentDetailsFromReport.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    });

  $urlRouterProvider.when(`${url}/`, url);
}

routes.$inject = ['$stateProvider', '$urlRouterProvider'];

export default prioritiesPageModule;
