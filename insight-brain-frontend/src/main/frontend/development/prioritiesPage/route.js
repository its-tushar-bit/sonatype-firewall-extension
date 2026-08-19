/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import PrioritiesPage from 'MainRoot/development/prioritiesPage/PrioritiesPage';
import ApplicationReportRoot from 'MainRoot/applicationReport/ApplicationReportRoot';
import DependencyTreePage from 'MainRoot/DependencyTree/DependencyTreePage';
import ComponentDetails from 'MainRoot/componentDetails/ComponentDetails';
import ListWaiversTable from 'MainRoot/waivers/ListWaiversTable';
import VulnerabilityCustomize from 'MainRoot/vulnerabilityCustomize/VulnerabilityCustomize';
import ComponentLegalOverviewContainer from 'MainRoot/legal/ComponentLegalOverviewContainer';
import BulkWaivePage from 'MainRoot/waivers/BulkWaivePage';
import WaiverConfigurationPage from 'MainRoot/waivers/WaiverConfigurationPage';
import WaiverConfirmationPage from 'MainRoot/waivers/WaiverConfirmationPage';

const cdpFromDashboard = 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard';
const cdpFromReports = 'componentDetailsPageWithinPrioritiesPageContainerFromReports';
const cdpFromIntegrations = 'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations';

// Standalone Developer Dashboard -> Priorities Page
router.stateRegistry.register({
  name: 'prioritiesPageFromDashboard',
  url: '/dashboard/developer/priorities/{publicAppId}/{scanId}?componentNameFilter&filterOnPolicyActions',
  component: PrioritiesPage,
  data: {
    title: 'Priorities',
    favicon: 'productIcons/Developer',
  },
});

// Standalone Developer Reports Page -> Priorities Page
router.stateRegistry.register({
  name: 'prioritiesPageFromReports',
  url: '/developer/priorities/{publicAppId}/{scanId}?componentNameFilter&filterOnPolicyActions',
  component: PrioritiesPage,
  data: {
    title: 'Priorities',
    favicon: 'productIcons/Developer',
  },
});

// Integrations -> Priorities Page
router.stateRegistry.register({
  name: 'prioritiesPageFromIntegrations',
  url: '/developer/integrations/{publicAppId}/{scanId}/{integrationType}?componentNameFilter&filterOnPolicyActions',
  component: PrioritiesPage,
  data: {
    title: 'Priorities',
    favicon: 'productIcons/Developer',
  },
});

// Helper function to register component details routes for a given base state
function registerComponentDetailsRoutes(baseName) {
  router.stateRegistry.register({
    name: `${baseName}.dependencyTree`,
    url: '/dependencyTree',
    component: DependencyTreePage,
    data: {
      title: 'Dependency Tree',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.componentDetails`,
    url: '/componentDetails/{hash}',
    component: ComponentDetails,
    data: {
      title: 'Component Details',
    },
    params: {
      tabId: 'overview',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.componentDetails.overview`,
    url: '/overview',
    params: {
      tabId: 'overview',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.componentDetails.violations`,
    url: '/violations',
    params: {
      tabId: 'violations',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.componentDetails.security`,
    url: '/security',
    params: {
      tabId: 'security',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.componentDetails.legal`,
    url: '/legal',
    params: {
      tabId: 'legal',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.componentDetails.audit`,
    url: '/audit',
    params: {
      tabId: 'audit',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.componentDetails.claim`,
    url: '/claim',
    params: {
      tabId: 'claim',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.componentDetails.labels`,
    url: '/labels',
    params: {
      tabId: 'labels',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.violationWaivers`,
    url: '/{hash}/waivers/{violationId}',
    component: ListWaiversTable,
  });

  router.stateRegistry.register({
    name: `${baseName}.vulnerabilityCustomize`,
    url: '/vulnerabilities/{ownerType}/{ownerId}/customize/{refId}?componentIdentifier&componentHash&tabId',
    component: VulnerabilityCustomize,
    data: {
      title: 'Customize Vulnerability Details',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.applicationStageTypeComponentOverview`,
    url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}?scanId&tabId',
    component: ComponentLegalOverviewContainer,
    data: {
      title: 'Component - Legal Overview',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.bulkWaive`,
    url: '/{hash}/bulkWaive',
    component: BulkWaivePage,
    data: {
      title: 'Bulk Waive',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.waiverConfiguration`,
    url: '/{hash}/waiverConfiguration',
    component: WaiverConfigurationPage,
    data: {
      title: 'Waiver Configuration',
    },
  });

  router.stateRegistry.register({
    name: `${baseName}.waiverConfirmation`,
    url: '/{hash}/waiverConfirmation',
    component: WaiverConfirmationPage,
    data: {
      title: 'Waiver Confirmation',
    },
  });
}

// Standalone Developer Dashboard -> Priorities Page -> Component Details Page
router.stateRegistry.register({
  name: cdpFromDashboard,
  url: '/dashboard/developer/priorities/report/{publicId}/{scanId}',
  abstract: true,
  component: ApplicationReportRoot,
  params: {
    policyViolationId: { dynamic: true },
  },
});
registerComponentDetailsRoutes(cdpFromDashboard);

// Standalone Developer Reports Page -> Priorities Page -> Component Details Page
router.stateRegistry.register({
  name: cdpFromReports,
  url: '/developer/priorities/report/{publicId}/{scanId}',
  abstract: true,
  component: ApplicationReportRoot,
  params: {
    policyViolationId: { dynamic: true },
  },
});
registerComponentDetailsRoutes(cdpFromReports);

// Integrations -> Priorities Page -> Component Details Page
router.stateRegistry.register({
  name: cdpFromIntegrations,
  url: '/developer/integrations/{publicId}/{scanId}',
  abstract: true,
  component: ApplicationReportRoot,
  params: {
    policyViolationId: { dynamic: true },
  },
});
registerComponentDetailsRoutes(cdpFromIntegrations);

// URL rewrite for backward compatibility
router.urlService.rules.when(
  '/dashboard/developer/priorities/{publicAppId}/{scanId}/',
  (matchValues, _urlParts, router) => router.stateService.go('prioritiesPageFromDashboard', matchValues)
);
