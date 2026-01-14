/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import OperationalReportingLandingPage from 'MainRoot/operationalReporting/OperationalReportingLandingPage';

export default angular
  .module('operationalReporting', [])
  .component(
    'operationalReportingLandingPage',
    iqReact2Angular(withStoreProvider(OperationalReportingLandingPage), [], ['$state'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('operationalReporting', {
    url: '/operationalReporting',
    component: 'operationalReportingLandingPage',
    data: {
      title: 'Operational Reporting',
    },
  });
}

routes.$inject = ['$stateProvider'];
