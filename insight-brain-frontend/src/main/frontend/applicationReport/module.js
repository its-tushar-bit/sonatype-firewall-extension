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
import applicationReportReducer from './applicationReportReducer';
import applicationReportActions from './applicationReportActions';
import reevaluationErrorModal from './reevaluationErrorModal/reevaluationErrorModal';
import applicationReportRoot from './applicationReportRoot';
import rawLicenseDisplay from './rawData/rawLicenseDisplay/rawLicenseDisplay';
import applicationReportRawData from './rawData/applicationReportRawData';
import applicationReportVulnerabilities from './vulnerabilities/ApplicationReportVulnerabilities';

export default angular.module('applicationReportModule',
    [
      cipModalModule.name, CLMLocationsModule.name, utilityModule.name, utilityDirectivesModule.name,
      ComponentDisplayModule.name, selectedComponentServiceModule.name, 'ngRedux'
    ])
    .component('applicationReport', applicationReport)
    .component('applicationReportRoot', applicationReportRoot)
    .component('applicationReportResults', applicationReportResults)
    .component('reevaluationErrorModal', reevaluationErrorModal)
    .component('rawLicenseDisplay', rawLicenseDisplay)
    .component('applicationReportRawData', applicationReportRawData)
    .component('applicationReportVulnerabilities', react2angular(applicationReportVulnerabilities, [],
        ['$ngRedux', '$state', 'applicationReportActions']))
    .value('applicationReportReducer', applicationReportReducer) // add to angular so we can test it
    .factory('applicationReportActions', applicationReportActions)
    .config(routes);

function routes($stateProvider, $urlRouterProvider) {
  $stateProvider
      .state('applicationReport', {
        url: '/applicationReport/{publicId}/{scanId}?unknownjs&embeddable',
        abstract: true,
        component: 'applicationReportRoot'
      })
      .state('applicationReport.policy', {
        url: '/policy',
        component: 'applicationReport',
        data: {
          title: 'Application Report'
        }
      })
      .state('applicationReport.rawData', {
        url: '/raw',
        component: 'applicationReportRawData',
        data: {
          title: 'Application Report Raw Data'
        }
      })
      .state('applicationReport.vulnerabilities', {
        url: '/vulnerabilities',
        component: 'applicationReportVulnerabilities',
        data: {
          title: 'Application Report Vulnerabilities List'
        }
      });

  $urlRouterProvider.when('/applicationReport/{publicId}/{scanId}?unknownjs',
      '/applicationReport/{publicId}/{scanId}/policy?unknownjs');
}

routes.$inject = ['$stateProvider', '$urlRouterProvider'];
