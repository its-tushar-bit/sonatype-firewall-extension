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

const cdpFromDashboard = 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard';
const cdpFromReports = 'componentDetailsPageWithinPrioritiesPageContainerFromReports';
const cdpFromIntegrations = 'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations';

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

    // Integrations -> Priorities Page
    .state('prioritiesPageFromIntegrations', {
      url: '/developer/integrations/{publicAppId}/{scanId}/{integrationType}?componentNameFilter&filterOnPolicyActions',
      component: 'prioritiesPage',
      data: {
        title: 'Priorities',
      },
    })

    // Standalone Developer Dashboard -> Priorities Page -> Component Details Page
    .state(cdpFromDashboard, {
      url: '/dashboard/developer/priorities/report/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state(`${cdpFromDashboard}.dependencyTree`, {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state(`${cdpFromDashboard}.componentDetails`, {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state(`${cdpFromDashboard}.componentDetails.overview`, {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state(`${cdpFromDashboard}.componentDetails.violations`, {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state(`${cdpFromDashboard}.componentDetails.security`, {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state(`${cdpFromDashboard}.componentDetails.legal`, {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state(`${cdpFromDashboard}.componentDetails.audit`, {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state(`${cdpFromDashboard}.componentDetails.claim`, {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state(`${cdpFromDashboard}.componentDetails.labels`, {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state(`${cdpFromDashboard}.violationWaivers`, {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state(`${cdpFromDashboard}.vulnerabilityCustomize`, {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state(`${cdpFromDashboard}.applicationStageTypeComponentOverview`, {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/' + '{hash}?scanId&tabId',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })

    // Standalone Developer Reports Page -> Priorities Page -> Component Details Page
    .state(cdpFromReports, {
      url: '/developer/priorities/report/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state(`${cdpFromReports}.dependencyTree`, {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state(`${cdpFromReports}.componentDetails`, {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state(`${cdpFromReports}.componentDetails.overview`, {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state(`${cdpFromReports}.componentDetails.violations`, {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state(`${cdpFromReports}.componentDetails.security`, {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state(`${cdpFromReports}.componentDetails.legal`, {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state(`${cdpFromReports}.componentDetails.claim`, {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state(`${cdpFromReports}.componentDetails.audit`, {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state(`${cdpFromReports}.componentDetails.labels`, {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state(`${cdpFromReports}.violationWaivers`, {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state(`${cdpFromReports}.vulnerabilityCustomize`, {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state(`${cdpFromReports}.applicationStageTypeComponentOverview`, {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/' + '{hash}?scanId&tabId',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })

    // Integrations -> Priorities Page -> Component Details Page
    .state(cdpFromIntegrations, {
      url: '/developer/integrations/{publicId}/{scanId}',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state(`${cdpFromIntegrations}.dependencyTree`, {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state(`${cdpFromIntegrations}.componentDetails`, {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state(`${cdpFromIntegrations}.componentDetails.overview`, {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state(`${cdpFromIntegrations}.componentDetails.violations`, {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state(`${cdpFromIntegrations}.componentDetails.security`, {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state(`${cdpFromIntegrations}.componentDetails.legal`, {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state(`${cdpFromIntegrations}.componentDetails.audit`, {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state(`${cdpFromIntegrations}.componentDetails.claim`, {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state(`${cdpFromIntegrations}.componentDetails.labels`, {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state(`${cdpFromIntegrations}.violationWaivers`, {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state(`${cdpFromIntegrations}.vulnerabilityCustomize`, {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state(`${cdpFromIntegrations}.applicationStageTypeComponentOverview`, {
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
