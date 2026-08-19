/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import EnterpriseReportingLandingPage from './EnterpriseReportingLandingPage';
import EnterpriseReportingDashboardPage from './dashboard/EnterpriseReportingDashboardPage';

router.stateRegistry.register({
  name: 'enterpriseReporting',
  url: '/enterpriseReportingLandingPage',
  component: withStoreProvider(EnterpriseReportingLandingPage),
  data: {
    title: 'Enterprise Data Insights',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'enterpriseReportingDashboardGroup',
  url: '/enterpriseReportingDashboard/{groupId}/{id}',
  component: withStoreProvider(EnterpriseReportingDashboardPage),
  data: {
    title: 'Enterprise Reporting Dashboard',
    authenticationRequired: true,
  },
});

router.stateRegistry.register({
  name: 'enterpriseReportingDashboard',
  url: '/enterpriseReportingDashboard/{id}',
  component: withStoreProvider(EnterpriseReportingDashboardPage),
  data: {
    title: 'Enterprise Reporting Dashboard',
    authenticationRequired: true,
  },
});
