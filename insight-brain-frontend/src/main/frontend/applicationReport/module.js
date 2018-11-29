/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import cipModalModule from './results/cipModal/module';
import CLMLocationsModule from '../util/CLMLocation';
import utilityModule from '../utility/utility.module';
import ComponentDisplayModule from '../ComponentDisplay/module';

import applicationReport from './applicationReport';
import applicationReportResults from './results/applicationReportResults';
import applicationReportReducer from './applicationReportReducer';
import applicationReportActions from './applicationReportActions';
import reevaluationErrorModal from './reevaluationErrorModal/reevaluationErrorModal';

export default angular.module('applicationReportModule',
    [
      cipModalModule.name, CLMLocationsModule.name, utilityModule.name, ComponentDisplayModule.name
    ])
    .component('applicationReport', applicationReport)
    .component('applicationReportResults', applicationReportResults)
    .component('reevaluationErrorModal', reevaluationErrorModal)
    .value('applicationReportReducer', applicationReportReducer) // add to angular so we can test it
    .factory('applicationReportActions', applicationReportActions)
    .config(routes);

function routes($stateProvider) {
  $stateProvider.state('applicationReport', {
    url: '/applicationReport/{publicId}/{scanId}?unknownjs',
    component: 'applicationReport',
    data: {
      title: 'Application Report'
    }
  });
}

routes.$inject = ['$stateProvider'];
