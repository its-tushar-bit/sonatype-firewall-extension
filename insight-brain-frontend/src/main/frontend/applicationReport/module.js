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

import applicationReport from './applicationReport';
import applicationReportResults from './results/applicationReportResults';
import applicationReportActions from './applicationReportActions';
import reevaluationErrorModal from './reevaluationErrorModal/reevaluationErrorModal';
import applicationReportRoot from './applicationReportRoot';
import rawLicenseDisplay from './rawData/rawLicenseDisplay/rawLicenseDisplay';
import applicationReportRawData from './rawData/applicationReportRawData';
import applicationReportVulnerabilities from './vulnerabilities/ApplicationReportVulnerabilities';
import applicationReportFilter from './applicationReportFilter';

export default angular
  .module('applicationReportModule', [
    cipModalModule.name,
    CLMLocationsModule.name,
    utilityModule.name,
    utilityDirectivesModule.name,
    ComponentDisplayModule.name,
    selectedComponentServiceModule.name,
    'ngRedux',
  ])
  .component('applicationReport', applicationReport)
  .component('applicationReportFilter', applicationReportFilter)
  .component('applicationReportRoot', applicationReportRoot)
  .component('applicationReportResults', applicationReportResults)
  .component('reevaluationErrorModal', reevaluationErrorModal)
  .component('rawLicenseDisplay', rawLicenseDisplay)
  .component('applicationReportRawData', applicationReportRawData)
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
        tabId: 'remediation',
      },
    })
    .state('applicationReport.componentDetails.remediation', {
      url: '/remediation',
      params: {
        tabId: 'remediation',
      },
    })
    .state('applicationReport.componentDetails.info', {
      url: '/info',
      params: {
        tabId: 'info',
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
    });

  $urlRouterProvider
    .when('/applicationReport/{publicId}/{scanId}?unknownjs', '/applicationReport/{publicId}/{scanId}/policy?unknownjs')
    .when(
      '/applicationReport/{publicId}/{scanId}/componentDetails/{hash}',
      '/applicationReport/{publicId}/{scanId}/componentDetails/{hash}/remediation'
    );
}

routes.$inject = ['$stateProvider', '$urlRouterProvider'];
