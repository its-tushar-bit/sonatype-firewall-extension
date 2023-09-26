/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import EnterpriseReportingPage from 'MainRoot/enterpriseReporting/EnterpriseReportingPage';

export default angular
  .module('embeddedLookerDashboard', [])
  .component('enterpriseReportingPage', react2angular(withStoreProvider(EnterpriseReportingPage), [], ['$ngRedux']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('enterpriseReporting', {
    url: '/enterpriseReporting',
    component: 'enterpriseReportingPage',
    data: {
      title: 'Enterprise Reporting',
      authenticationRequired: true,
    },
  });
}

routes.$inject = ['$stateProvider'];
