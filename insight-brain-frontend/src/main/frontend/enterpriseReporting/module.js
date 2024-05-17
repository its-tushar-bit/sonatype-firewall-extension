/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import EnterpriseReportingLandingPage from 'MainRoot/enterpriseReporting/EnterpriseReportingLandingPage';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';

export default angular
  .module('embeddedLookerDashboard', [])
  .component(
    'enterpriseReportingLandingPage',
    react2angular(withStoreProvider(EnterpriseReportingLandingPage), [], ['$ngRedux'])
  )
  .component(
    'enterpriseReportingDashboardPage',
    react2angular(withStoreProvider(EnterpriseReportingDashboardPage), [], ['$ngRedux'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('enterpriseReporting', {
      url: '/enterpriseReportingLandingPage',
      component: 'enterpriseReportingLandingPage',
      data: {
        title: 'Enterprise Data Insights',
        authenticationRequired: true,
      },
    })
    .state('enterpriseReportingDashboard', {
      url: '/enterpriseReportingDashboard',
      component: 'enterpriseReportingDashboardPage',
      redirectTo: function (transition) {
        const injector = transition.injector();
        const $ngRedux = injector.get('$ngRedux');
        const state = $ngRedux.getState();
        if (!state?.enterpriseReportingDashboard?.selectedDashboard?.dashboardId) {
          return 'enterpriseReporting';
        }
        return null;
      },
      data: {
        title: 'Enterprise Reporting Dashboard',
        authenticationRequired: true,
      },
    });
}

routes.$inject = ['$stateProvider'];
