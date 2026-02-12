/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import EnterpriseReportingLandingPage from 'MainRoot/enterpriseReporting/EnterpriseReportingLandingPage';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';
import HeroDevsEolPage from 'MainRoot/enterpriseReporting/HeroDevsEolPage';

export default angular
  .module('embeddedLookerDashboard', [])
  .component(
    'enterpriseReportingLandingPage',
    iqReact2Angular(withStoreProvider(EnterpriseReportingLandingPage), [], ['$state'])
  )
  .component(
    'enterpriseReportingDashboardPage',
    iqReact2Angular(withStoreProvider(EnterpriseReportingDashboardPage), ['clmServerVersion'], ['$state'])
  )
  .component('heroDevsEolPage', iqReact2Angular(withStoreProvider(HeroDevsEolPage), [], ['$state']))
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
    .state('heroDevsEol', {
      url: '/enterpriseReportingDashboard/herodevs_eol',
      component: 'heroDevsEolPage',
      data: {
        title: 'HeroDevs End Of Life Components',
        authenticationRequired: true,
      },
    })
    .state('enterpriseReportingDashboardGroup', {
      url: '/enterpriseReportingDashboard/{groupId}/{id}',
      component: 'enterpriseReportingDashboardPage',
      data: {
        title: 'Enterprise Reporting Dashboard',
        authenticationRequired: true,
      },
    })
    .state('enterpriseReportingDashboard', {
      url: '/enterpriseReportingDashboard/{id}',
      component: 'enterpriseReportingDashboardPage',
      data: {
        title: 'Enterprise Reporting Dashboard',
        authenticationRequired: true,
      },
    });
}

routes.$inject = ['$stateProvider'];
