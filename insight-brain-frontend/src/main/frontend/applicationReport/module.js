/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import CLMLocationsModule from '../util/CLMLocation';
import applicationReport from './applicationReport';
import applicationReportResults from './results/applicationReportResults';
import utilityModule from '../utility/utility.module';
import applicationReportReducer from './applicationReportReducer';
import applicationReportActions from './applicationReportActions';

export default angular.module('applicationReportModule', [CLMLocationsModule.name, utilityModule.name])
    .component('applicationReport', applicationReport)
    .component('applicationReportResults', applicationReportResults)
    .value('applicationReportReducer', applicationReportReducer) // add to angular so we can test it
    .factory('applicationReportActions', applicationReportActions)
    .config(routes);

function routes($stateProvider) {
  $stateProvider.state('applicationReport', {
    url: '/applicationReport/{publicId}/{scanId}',
    component: 'applicationReport',
    data: {
      title: 'Application Report'
    }
  });
}

routes.$inject = ['$stateProvider'];
