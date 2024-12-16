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

function routes($stateProvider, $urlServiceProvider) {
  $stateProvider
    // Standalone Developer Dashboard -> Priorities Page
    .state('prioritiesPageFromDashboard', {
      url: '/dashboard/developer/priorities/{publicAppId}/{scanId}?componentNameFilter&filterOnPolicyActions',
      component: 'prioritiesPage',
      data: {
        title: 'Priorities',
      },
    })

    // Standalone Developer Reports Page -> Priorities Page
    .state('prioritiesPageFromReports', {
      url: '/developer/priorities/{publicAppId}/{scanId}?componentNameFilter&filterOnPolicyActions',
      component: 'prioritiesPage',
      data: {
        title: 'Priorities',
      },
    })

    // Standalone Developer Dashboard -> Priorities Page -> Component Details Page
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard', {
      url: '/dashboard/developer/priorities/report/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.dependencyTree', {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails', {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.violationWaivers', {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.vulnerabilityCustomize', {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromDashboard.applicationStageTypeComponentOverview', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/' + '{hash}?scanId&tabId',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })

    // Standalone Developer Reports Page -> Priorities Page -> Component Details Page
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports', {
      url: '/developer/priorities/report/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.dependencyTree', {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails', {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.violationWaivers', {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.vulnerabilityCustomize', {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state('componentDetailsPageWithinPrioritiesPageContainerFromReports.applicationStageTypeComponentOverview', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/' + '{hash}?scanId&tabId',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    });

  $urlServiceProvider.rules.when(
    '/dashboard/developer/priorities/{publicAppId}/{scanId}/',
    (matchValues, _urlParts, router) => router.stateService.go('prioritiesPageFromDashboard', matchValues)
  );
}

routes.$inject = ['$stateProvider', '$urlServiceProvider'];

export default prioritiesPageModule;
