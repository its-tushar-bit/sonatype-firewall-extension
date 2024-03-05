/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import DevelopmentReport from 'MainRoot/development/developmentReport/DevelopmentReport';

const developmentReportModule = angular
  .module('developmentReportModule', ['ngRedux'])
  .component('developmentReport', iqReact2Angular(DevelopmentReport, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('development', {
    url: '/development/priorities/{appId}/{scanId}',
    component: 'developmentReport',
    data: {
      title: 'Sonatype Development Report',
    },
  });
}

routes.$inject = ['$stateProvider'];

export default developmentReportModule;
