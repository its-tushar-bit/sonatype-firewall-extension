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

const url = 'dashboard/developer/priorities/{publicAppId}/{scanId}';

function routes($stateProvider, $urlRouterProvider) {
  $stateProvider
    .state('prioritiesPageFromDashboard', {
      url: '/dashboard/developer/priorities/{publicAppId}/{scanId}',
      component: 'prioritiesPage',
      data: {
        title: 'Priorities',
      },
    })
    .state('prioritiesPageFromReports', {
      url: '/developer/priorities/{publicAppId}/{scanId}',
      component: 'prioritiesPage',
      data: {
        title: 'Priorities',
      },
    })
    .state('prioritiesPageFromAppReport', {
      url: '/appReport/developer/priorities/{publicAppId}/{scanId}',
      component: 'prioritiesPage',
      data: {
        title: 'Priorities',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard', {
      url: '/dashboard/developer/priorities/report/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.dependencyTree', {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.policy', {
      url: '/policy?roarelSaysCip&componentHash&tabId',
      component: 'applicationReport',
      data: {
        title: 'Application Report',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.rawData', {
      url: '/raw',
      component: 'applicationReportRawData',
      data: {
        title: 'Application Report',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.vulnerabilities', {
      url: '/vulnerabilities',
      component: 'applicationReportVulnerabilities',
      data: {
        title: 'Application Report Vulnerabilities List',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails', {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetailsFromReport', {
      url: '/componentDetailsFromReport/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetailsFromReport.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetailsFromReport.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetailsFromReport.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetailsFromReport.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetailsFromReport.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetailsFromReport.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })

    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetailsFromReport.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.violationWaivers', {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.vulnerabilityCustomize', {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromDashboard.applicationStageTypeComponentOverview', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/' + '{hash}?scanId&tabId',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })

    .state('appReportPageWithinPrioritiesPageContainerFromReports', {
      url: '/developer/priorities/report/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.dependencyTree', {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.policy', {
      url: '/policy?roarelSaysCip&componentHash&tabId',
      component: 'applicationReport',
      data: {
        title: 'Application Report',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.rawData', {
      url: '/raw',
      component: 'applicationReportRawData',
      data: {
        title: 'Application Report',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.vulnerabilities', {
      url: '/vulnerabilities',
      component: 'applicationReportVulnerabilities',
      data: {
        title: 'Application Report Vulnerabilities List',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetails', {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetails.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetails.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetails.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetails.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetails.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetails.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetails.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetailsFromReport', {
      url: '/componentDetailsFromReport/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetailsFromReport.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetailsFromReport.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetailsFromReport.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetailsFromReport.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetailsFromReport.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetailsFromReport.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.componentDetailsFromReport.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.violationWaivers', {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.vulnerabilityCustomize', {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromReports.applicationStageTypeComponentOverview', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/' + '{hash}?scanId&tabId',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })

    .state('appReportPageWithinPrioritiesPageContainerFromAppReport', {
      url: '/appReport/developer/priorities/report/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.dependencyTree', {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.policy', {
      url: '/policy?roarelSaysCip&componentHash&tabId',
      component: 'applicationReport',
      data: {
        title: 'Application Report',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.rawData', {
      url: '/raw',
      component: 'applicationReportRawData',
      data: {
        title: 'Application Report',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.vulnerabilities', {
      url: '/vulnerabilities',
      component: 'applicationReportVulnerabilities',
      data: {
        title: 'Application Report Vulnerabilities List',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetails', {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetails.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetails.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetails.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetails.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetails.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetails.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetails.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport', {
      url: '/componentDetailsFromReport/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.violationWaivers', {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.vulnerabilityCustomize', {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state('appReportPageWithinPrioritiesPageContainerFromAppReport.applicationStageTypeComponentOverview', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/' + '{hash}?scanId&tabId',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    });

  $urlRouterProvider.when(`${url}/`, url);
}

routes.$inject = ['$stateProvider', '$urlRouterProvider'];

export default prioritiesPageModule;
