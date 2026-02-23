/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import ReportPage from './ReportPage';
import BulkWaivePage from 'MainRoot/waivers/BulkWaivePage';
import WaiverConfigurationPage from 'MainRoot/waivers/WaiverConfigurationPage';
import WaiverConfirmationPage from 'MainRoot/waivers/WaiverConfirmationPage';
import ApplicationReportRawDataContainer from './rawData/ApplicationReportRawDataContainer';
import ApplicationReportVulnerabilities from './vulnerabilities/ApplicationReportVulnerabilities';
import ApplicationReportRoot from './ApplicationReportRoot';
import ComponentDetails from 'MainRoot/componentDetails/ComponentDetails';
import ListWaiversTable from 'MainRoot/waivers/ListWaiversTable';
import VulnerabilityCustomize from 'MainRoot/vulnerabilityCustomize/VulnerabilityCustomize';
import ComponentLegalOverviewContainer from 'MainRoot/legal/ComponentLegalOverviewContainer';
import DependencyTreePage from 'MainRoot/DependencyTree/DependencyTreePage';

// Abstract parent state
router.stateRegistry.register({
  name: 'applicationReport',
  url: '/applicationReport/{publicId}/{scanId}?unknownjs&embeddable&policyViolationId',
  abstract: true,
  component: ApplicationReportRoot,
  params: {
    policyViolationId: { dynamic: true },
  },
});

router.stateRegistry.register({
  name: 'applicationReport.dependencyTree',
  url: '/dependencyTree',
  component: DependencyTreePage,
  data: {
    title: 'Dependency Tree',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.policy',
  url: '/policy?roarelSaysCip&componentHash&tabId',
  component: ReportPage,
  data: {
    title: 'Application Report',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.bulkWaive',
  url: '/bulkWaive',
  component: BulkWaivePage,
  data: {
    title: 'Bulk Waive',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.cdpBulkWaive',
  url: '/{hash}/bulkWaive',
  component: BulkWaivePage,
  data: {
    title: 'Bulk Waive',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.waiverConfiguration',
  url: '/waiverConfiguration',
  component: WaiverConfigurationPage,
  data: {
    title: 'Waiver Configuration',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.cdpWaiverConfiguration',
  url: '/{hash}/waiverConfiguration',
  component: WaiverConfigurationPage,
  data: {
    title: 'Waiver Configuration',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.waiverConfirmation',
  url: '/waiverConfirmation',
  component: WaiverConfirmationPage,
  data: {
    title: 'Waiver Confirmation',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.cdpWaiverConfirmation',
  url: '/{hash}/waiverConfirmation',
  component: WaiverConfirmationPage,
  data: {
    title: 'Waiver Confirmation',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.rawData',
  url: '/raw',
  component: ApplicationReportRawDataContainer,
  data: {
    title: 'Application Report Raw Data',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.vulnerabilities',
  url: '/vulnerabilities',
  component: ApplicationReportVulnerabilities,
  data: {
    title: 'Application Report Vulnerabilities List',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.componentDetails',
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
  name: 'applicationReport.componentDetails.overview',
  url: '/overview',
  params: {
    tabId: 'overview',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.componentDetails.violations',
  url: '/violations',
  params: {
    tabId: 'violations',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.componentDetails.security',
  url: '/security',
  params: {
    tabId: 'security',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.componentDetails.legal',
  url: '/legal',
  params: {
    tabId: 'legal',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.componentDetails.audit',
  url: '/audit',
  params: {
    tabId: 'audit',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.componentDetails.claim',
  url: '/claim',
  params: {
    tabId: 'claim',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.componentDetails.labels',
  url: '/labels',
  params: {
    tabId: 'labels',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.violationWaivers',
  url: '/{hash}/waivers/{violationId}',
  component: ListWaiversTable,
});

router.stateRegistry.register({
  name: 'applicationReport.vulnerabilityCustomize',
  url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
  component: VulnerabilityCustomize,
  data: {
    title: 'Customize Vulnerability Details',
  },
});

router.stateRegistry.register({
  name: 'applicationReport.applicationStageTypeComponentOverview',
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}?scanId&tabId',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

// URL rewrites
router.urlService.rules.when('/applicationReport/{publicId}/{scanId}?unknownjs', (matchValues, _urlParts, router) =>
  router.stateService.go('applicationReport.policy', matchValues)
);

router.urlService.rules.when(
  '/applicationReport/{publicId}/{scanId}/componentDetails/{hash}',
  (matchValues, _urlParts, router) => router.stateService.go('applicationReport.componentDetails.overview', matchValues)
);
