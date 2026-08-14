/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import ReportPage from 'MainRoot/applicationReport/ReportPage';
import ApplicationReportRawDataContainer from 'MainRoot/applicationReport/rawData/ApplicationReportRawDataContainer';
import ApplicationReportVulnerabilities from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilities';
import HostedRepositoryComponentReportRoot from './HostedRepositoryComponentReportRoot';
import ComponentDetails from 'MainRoot/componentDetails/ComponentDetails';
import DependencyTreePage from 'MainRoot/DependencyTree/DependencyTreePage';

// Abstract parent state
router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport',
  // componentDisplayName is threaded through the URL so the friendly title survives
  // page refresh and deep-links (goToHrcReport in hostedReposActions.js passes it as a
  // route param; UI-Router silently drops undeclared params, so it must be listed here).
  url:
    '/hostedRepositoryComponentReport/{hrcId}/{scanId}?embeddable&policyViolationId&componentHash&tabId&componentDisplayName',
  abstract: true,
  component: HostedRepositoryComponentReportRoot,
  params: {
    policyViolationId: { dynamic: true },
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.summary',
  url: '/summary',
  component: ReportPage,
  data: {
    title: 'HRC Report - Summary',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.policy',
  url: '/policy?componentHash&tabId',
  component: ReportPage,
  data: {
    title: 'HRC Report - Policy Violations',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.dependencyTree',
  url: '/dependencyTree',
  component: DependencyTreePage,
  data: {
    title: 'HRC Report - Dependency Tree',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.rawData',
  url: '/raw',
  component: ApplicationReportRawDataContainer,
  data: {
    title: 'HRC Report - Raw Data',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.licenseAnalysis',
  url: '/licenseAnalysis',
  component: ReportPage,
  data: {
    title: 'HRC Report - License Analysis',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.reachability',
  url: '/reachability',
  component: ReportPage,
  data: {
    title: 'HRC Report - Reachability',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.vulnerabilities',
  url: '/vulnerabilities',
  component: ApplicationReportVulnerabilities,
  data: {
    title: 'HRC Report - Vulnerabilities List',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.componentDetails',
  url: '/componentDetails/{hash}',
  component: ComponentDetails,
  data: {
    title: 'Component Details',
    hideFooter: true,
  },
  params: {
    tabId: 'overview',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.componentDetails.overview',
  url: '/overview',
  params: {
    tabId: 'overview',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.componentDetails.violations',
  url: '/violations',
  params: {
    tabId: 'violations',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.componentDetails.security',
  url: '/security',
  params: {
    tabId: 'security',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.componentDetails.legal',
  url: '/legal',
  params: {
    tabId: 'legal',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.componentDetails.audit',
  url: '/audit',
  params: {
    tabId: 'audit',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.componentDetails.claim',
  url: '/claim',
  params: {
    tabId: 'claim',
  },
});

router.stateRegistry.register({
  name: 'hostedRepositoryComponentReport.componentDetails.labels',
  url: '/labels',
  params: {
    tabId: 'labels',
  },
});

// URL rewrites - redirect root to policy tab (consistent with application report behavior).
// See applicationReport/route.js:216-217 which rewrites the app-report root to
// applicationReport.policy — mirror that here.
router.urlService.rules.when('/hostedRepositoryComponentReport/{hrcId}/{scanId}', (matchValues, _urlParts, router) =>
  router.stateService.go('hostedRepositoryComponentReport.policy', matchValues)
);

router.urlService.rules.when(
  '/hostedRepositoryComponentReport/{hrcId}/{scanId}/componentDetails/{hash}',
  (matchValues, _urlParts, router) =>
    router.stateService.go('hostedRepositoryComponentReport.componentDetails.overview', matchValues)
);
