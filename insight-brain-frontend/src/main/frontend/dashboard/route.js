/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import DashboardLayout from './DashboardLayout';

import DashboardViolationsContainer from './results/violations/DashboardViolationsContainer';
import DashboardComponentsContainer from './results/components/DashboardComponentsContainer';
import ComponentRisk from './results/componentRisk/ComponentRisk';
import DashboardApplicationsContainer from './results/applications/DashboardApplicationsContainer';
import DashboardWaivers from './results/waivers/DashboardWaivers';
import DashboardResultsContainer from './results/DashboardResultsContainer';

// Dashboard parent state with layout
router.stateRegistry.register({
  name: 'dashboard',
  url: '/dashboard',
  abstract: true,
  component: DashboardLayout,
  data: {
    title: 'Dashboard',
    crumb: 'Dashboard',
  },
});

// Dashboard overview abstract state
router.stateRegistry.register({
  name: 'dashboard.overview',
  abstract: true,
  component: DashboardResultsContainer,
});

// Dashboard violations tab
router.stateRegistry.register({
  name: 'dashboard.overview.violations',
  url: '/violations',
  component: DashboardViolationsContainer,
  data: {
    title: 'Dashboard - Violations',
    exportTitle: 'Violations',
  },
});

// Dashboard components tab
router.stateRegistry.register({
  name: 'dashboard.overview.components',
  url: '/components',
  component: DashboardComponentsContainer,
  data: {
    title: 'Dashboard - Components',
    exportTitle: 'Components',
  },
});

// Dashboard applications tab
router.stateRegistry.register({
  name: 'dashboard.overview.applications',
  url: '/applications',
  component: DashboardApplicationsContainer,
  data: {
    title: 'Dashboard - Applications',
    exportTitle: 'Applications',
  },
});

// Dashboard waivers tab
router.stateRegistry.register({
  name: 'dashboard.overview.waivers',
  url: '/waivers',
  component: DashboardWaivers,
  data: {
    title: 'Dashboard - Waivers',
    exportTitle: 'Waivers',
  },
});

// Dashboard waiver requests tab
router.stateRegistry.register({
  name: 'dashboard.overview.waiverRequests',
  url: '/waiverRequests',
  component: DashboardWaivers,
  data: {
    title: 'Dashboard - Waiver Requests',
    exportTitle: 'Waiver Requests',
  },
});

// Dashboard component details
router.stateRegistry.register({
  name: 'dashboard.component',
  url: '/component/{hash}',
  component: ComponentRisk,
  data: {
    crumb: 'Component Details',
  },
});

// URL rewrites for backward compatibility
router.urlService.rules.when('/dashboard/newest-risk', (matchValues, _urlParts, router) =>
  router.stateService.go('dashboard.overview.violations', matchValues)
);

router.urlService.rules.when('/repositories/quarantinedComponent/{token}', (matchValues, _urlParts, router) =>
  router.stateService.go('firewall.quarantinedComponentReport', matchValues)
);
