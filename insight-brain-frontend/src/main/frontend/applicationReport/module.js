/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import CLMLocationsModule from '../util/CLMLocation';
import utilityModule from '../utility/utility.module';
import selectedComponentServiceModule from '../services/selectedComponentService';
import waiversModule from '../waivers/module';

import ReportPage from './ReportPage';
import applicationReportActions from './applicationReportActions';
import applicationReportRoot from './applicationReportRoot';
import applicationReportVulnerabilities from './vulnerabilities/ApplicationReportVulnerabilities';
import ApplicationReportRawDataContainer from './rawData/ApplicationReportRawDataContainer';

export default angular
  .module('applicationReportModule', [
    CLMLocationsModule.name,
    utilityModule.name,
    selectedComponentServiceModule.name,
    waiversModule.name,
    'ngRedux',
  ])
  .component('applicationReport', iqReact2Angular(ReportPage, [], ['$ngRedux', '$state']))
  .component('applicationReportRoot', applicationReportRoot)
  .component('applicationReportRawData', iqReact2Angular(ApplicationReportRawDataContainer, [], ['$ngRedux', '$state']))
  .component(
    'applicationReportVulnerabilities',
    iqReact2Angular(applicationReportVulnerabilities, [], ['$ngRedux', '$state'])
  )
  .factory('applicationReportActions', applicationReportActions)
  .config(routes);

function routes($stateProvider, $urlServiceProvider) {
  $stateProvider
    .state('applicationReport', {
      url: '/applicationReport/{publicId}/{scanId}?unknownjs&embeddable&policyViolationId',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state('applicationReport.dependencyTree', {
      url: '/dependencyTree',
      component: 'dependencyTree',
      data: {
        title: 'Dependency Tree',
      },
    })
    .state('applicationReport.policy', {
      url: '/policy?roarelSaysCip&componentHash&tabId',
      component: 'applicationReport',
      data: {
        title: 'Application Report',
      },
    })
    .state('applicationReport.rawData', {
      url: '/raw',
      component: 'applicationReportRawData',
      data: {
        title: 'Application Report Raw Data',
      },
    })
    .state('applicationReport.vulnerabilities', {
      url: '/vulnerabilities',
      component: 'applicationReportVulnerabilities',
      data: {
        title: 'Application Report Vulnerabilities List',
      },
    })
    .state('applicationReport.componentDetails', {
      url: '/componentDetails/{hash}',
      component: 'componentDetails',
      data: {
        title: 'Component Details',
      },
      params: {
        tabId: 'overview',
      },
    })
    .state('applicationReport.componentDetails.overview', {
      url: '/overview',
      params: {
        tabId: 'overview',
      },
    })
    .state('applicationReport.componentDetails.violations', {
      url: '/violations',
      params: {
        tabId: 'violations',
      },
    })
    .state('applicationReport.componentDetails.security', {
      url: '/security',
      params: {
        tabId: 'security',
      },
    })
    .state('applicationReport.componentDetails.legal', {
      url: '/legal',
      params: {
        tabId: 'legal',
      },
    })
    .state('applicationReport.componentDetails.audit', {
      url: '/audit',
      params: {
        tabId: 'audit',
      },
    })
    .state('applicationReport.componentDetails.claim', {
      url: '/claim',
      params: {
        tabId: 'claim',
      },
    })
    .state('applicationReport.componentDetails.labels', {
      url: '/labels',
      params: {
        tabId: 'labels',
      },
    })
    .state('applicationReport.violationWaivers', {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    })
    .state('applicationReport.vulnerabilityCustomize', {
      url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
      component: 'vulnerabilityCustomize',
      data: {
        title: 'Customize Vulnerability Details',
      },
    })
    .state('applicationReport.applicationStageTypeComponentOverview', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}?scanId&tabId',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    });

  $urlServiceProvider.rules.when('/applicationReport/{publicId}/{scanId}?unknownjs', (matchValues, _urlParts, router) =>
    router.stateService.go('applicationReport.policy', matchValues)
  );
  $urlServiceProvider.rules.when(
    '/applicationReport/{publicId}/{scanId}/componentDetails/{hash}',
    (matchValues, _urlParts, router) =>
      router.stateService.go('applicationReport.componentDetails.overview', matchValues)
  );
}

routes.$inject = ['$stateProvider', '$urlServiceProvider'];
