/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import cipModalModule from './results/cipModal/module';
import CLMLocationsModule from '../util/CLMLocation';
import utilityModule from '../utility/utility.module';
import utilityDirectivesModule from '../utility/directives/utility.directives.module';
import ComponentDisplayModule from '../ComponentDisplay/module';
import selectedComponentServiceModule from '../services/selectedComponentService';
import withStoreProvider from '../reactAdapter/StoreProvider';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';
import waiversModule from '../waivers/module';

import applicationReport from './applicationReport';
import applicationReportResults from './results/applicationReportResults';
import applicationReportActions from './applicationReportActions';
import reevaluationErrorModal from './reevaluationErrorModal/reevaluationErrorModal';
import applicationReportRoot from './applicationReportRoot';
import applicationReportVulnerabilities from './vulnerabilities/ApplicationReportVulnerabilities';
import applicationReportFilter from './applicationReportFilter';
import ApplicationReportRawDataContainer from './rawData/ApplicationReportRawDataContainer';

export default angular
  .module('applicationReportModule', [
    cipModalModule.name,
    CLMLocationsModule.name,
    utilityModule.name,
    utilityDirectivesModule.name,
    ComponentDisplayModule.name,
    selectedComponentServiceModule.name,
    waiversModule.name,
    'ngRedux',
  ])
  .component('applicationReport', applicationReport)
  .component('applicationReportFilter', applicationReportFilter)
  .component('applicationReportRoot', applicationReportRoot)
  .component('applicationReportResults', applicationReportResults)
  .component('reevaluationErrorModal', reevaluationErrorModal)
  .component(
    'applicationReportRawData',
    react2angular(
      withStoreProvider(withRouterStateProvider(ApplicationReportRawDataContainer)),
      [],
      ['$ngRedux', '$state']
    )
  )
  .component(
    'applicationReportVulnerabilities',
    react2angular(applicationReportVulnerabilities, [], ['$ngRedux', '$state', 'applicationReportActions'])
  )
  .factory('applicationReportActions', applicationReportActions)
  .config(routes);

function routes($stateProvider, $urlRouterProvider) {
  $stateProvider
    .state('applicationReport', {
      url: '/applicationReport/{publicId}/{scanId}?unknownjs&embeddable&policyViolationId',
      abstract: true,
      component: 'applicationReportRoot',
      params: {
        policyViolationId: { dynamic: true },
      },
    })
    .state('applicationReport.policy', {
      url: '/policy?componentDetailsEnabled',
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
    .state('applicationReport.violationWaivers', {
      url: '/{hash}/waivers/{violationId}',
      component: 'listWaiversPage',
    });

  $urlRouterProvider
    .when('/applicationReport/{publicId}/{scanId}?unknownjs', '/applicationReport/{publicId}/{scanId}/policy?unknownjs')
    .when(
      '/applicationReport/{publicId}/{scanId}/componentDetails/{hash}',
      '/applicationReport/{publicId}/{scanId}/componentDetails/{hash}/overview'
    );
}

routes.$inject = ['$stateProvider', '$urlRouterProvider'];
