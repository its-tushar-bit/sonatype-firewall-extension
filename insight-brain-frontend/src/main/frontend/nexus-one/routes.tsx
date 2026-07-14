/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ReactStateDeclaration, UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import { PlatformHome } from 'MainRoot/nosc/platformHome/PlatformHome';
import {
  COMING_SOON_MODULES,
  COMING_SOON_MODULE_ORDER,
  ComingSoonModuleSlug,
  comingSoonHref,
  comingSoonStateName,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import SuccessMetricsReportListContainer from 'MainRoot/labs/successMetrics/SuccessMetricsReportListContainer';
import SuccessMetricsReportContainer from 'MainRoot/labs/successMetrics/successMetricsReport/SuccessMetricsReportContainer';
import ApiPage from 'MainRoot/api/ApiPage';
import React2ShellPage from 'MainRoot/report/react2shell/React2ShellPage';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';
import { ReportingRoute } from 'MainRoot/nexus-one/ReportingRoute';
import { ClassicComponentMount, mountClassicComponent } from 'MainRoot/nexus-one/ClassicComponentMount';
import { NATIVE_CLASSIC_EMBED_SLUGS } from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import { NEXUS_ONE_DEFAULT_PATH } from 'MainRoot/nosc/routing/classicPreviewMap';
import PreviewUiSettingsPage from 'MainRoot/nosc/settings/PreviewUiSettingsPage';
import { nexusOneDashboardStates } from 'MainRoot/nexus-one/nexusOneDashboardStates';
import { nexusOneApplicationDetailStates } from 'MainRoot/nexus-one/nexusOneApplicationDetailStates';
import { nexusOneApplicationReportStates } from 'MainRoot/nexus-one/nexusOneApplicationReportStates';
import { nexusOneViolationDetailStates } from 'MainRoot/nexus-one/nexusOneViolationDetailStates';
// NOTE: the three side-import route modules below call router.stateRegistry.register(...) at import
// time with no stateRegistry.get() idempotency guard, so this module (and any test) must import each
// exactly once. That holds today because they are only pulled into the single nexus-one bundle entry;
// a second import in the same runtime (e.g. a jest that imports both nexus-one/routes and one of these
// modules directly) would throw a duplicate-registration error. Keep them single-entry.
// Classic applicationReport child states (component details, raw data, etc.) so
// in-report links from the embedded ReportPage resolve inside the Nexus One bundle.
import 'MainRoot/applicationReport/route';
// Classic violation child states (sidebarView.violation, transitive violations)
// so in-detail links from the embedded ViolationPage resolve inside the Nexus One
// bundle. Registered at the singular /violation/{id} path — distinct from the
// nexusOneViolationDetail embed state at /violations/{id}.
import 'MainRoot/violation/route';
// Classic waiver states (addWaiver, requestWaiver, etc.) so the embedded
// ViolationPage's Add/Request Waiver actions resolve inside the Nexus One bundle
// (CLM-42256). ViolationDetailsTile stateGo's to these states directly.
import 'MainRoot/waivers/route';
import { SearchResultsPage } from 'MainRoot/nosc/searchResults/SearchResultsPage';
import PreviewApplicationsList from 'MainRoot/nosc/applications/ApplicationsList';
import {
  WaiversListPage as PreviewWaiversList,
  WaiverDetailPage as PreviewWaiverDetail,
} from 'MainRoot/nosc/waivers';
import { isAuthorized } from 'MainRoot/util/permissionService';

router.stateRegistry.register({
  name: 'root',
  url: '^',
  redirectTo: 'platformHome',
});

router.stateRegistry.register({
  name: 'home',
  url: '/',
  redirectTo: 'platformHome',
});

router.stateRegistry.register({
  name: 'platformHome',
  url: '/home',
  component: PlatformHome,
  data: {
    title: 'Nexus One',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneUiSettings',
  url: '/ui-settings',
  component: PreviewUiSettingsPage,
  data: {
    title: 'Nexus One UI Settings',
  },
  resolve: {
    isAuthorized: () => isAuthorized(['CONFIGURE_SYSTEM']),
  },
} as ReactStateDeclaration);

// Dashboard: abstract parent shell (tab strip + nested <UIView>) with one child state per tab.
nexusOneDashboardStates().forEach((state) => {
  router.stateRegistry.register(state);
});

router.stateRegistry.register({
  name: 'nexusOneApplications',
  url: '/applications',
  component: PreviewApplicationsList,
  data: {
    title: 'Nexus One — Applications',
  },
} as ReactStateDeclaration);

// Application detail: abstract parent shell + one child state per tab (CLM-40901).
nexusOneApplicationDetailStates().forEach((state) => {
  router.stateRegistry.register(state);
});

// Embedded Classic application policy report (CLM-41538).
nexusOneApplicationReportStates().forEach((state) => {
  router.stateRegistry.register(state);
});

// Embedded Classic violation detail (CLM-42256).
nexusOneViolationDetailStates().forEach((state) => {
  router.stateRegistry.register(state);
});

router.stateRegistry.register({
  name: 'nexusOneWaivers',
  url: '/waivers',
  component: PreviewWaiversList,
  data: {
    title: 'Nexus One — Waivers',
  },
} as ReactStateDeclaration);

// Interim top-level aliases until native entity-list pages own these paths (CLM-40905).
router.stateRegistry.register({
  name: 'nexusOneViolations',
  url: '/violations',
  redirectTo: 'nexusOneDashboard.violations',
  data: { title: 'Nexus One — Violations' },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneComponents',
  url: '/components',
  redirectTo: 'nexusOneDashboard.components',
  data: { title: 'Nexus One — Components' },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneWaiverDetail',
  // `?from` lets a caller (e.g. the Dashboard waivers tab) tell the detail page
  // where its back-link should return to.
  url: '/waivers/{ownerType}/{ownerId}/{waiverId}?from',
  component: PreviewWaiverDetail,
  data: {
    title: 'Nexus One — Waiver Detail',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneSearch',
  // `?q` carries the omnibar query; `?tab` selects the active results tab.
  url: '/search?q&tab',
  component: SearchResultsPage,
  data: {
    title: 'Nexus One — Search',
  },
} as ReactStateDeclaration);

// Deep links from the embedded Reporting pages target these Classic states by name
// (React2Shell card `href`, Enterprise dashboard cards `stateGo`). Register them in the
// Nexus One bundle so the links resolve and open the target page in-shell rather than a
// dead `#`. Both pages read their params from the Redux router state, so mountClassicComponent
// (which injects no props) is sufficient.
router.stateRegistry.register({
  name: 'react2ShellReport',
  url: '/reports/react2shell',
  component: mountClassicComponent(React2ShellPage),
  data: {
    title: 'React2Shell Vulnerability Report',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'enterpriseReportingDashboardGroup',
  url: '/enterpriseReportingDashboard/{groupId}/{id}',
  component: mountClassicComponent(EnterpriseReportingDashboardPage),
  data: {
    title: 'Enterprise Reporting Dashboard',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'enterpriseReportingDashboard',
  url: '/enterpriseReportingDashboard/{id}',
  component: mountClassicComponent(EnterpriseReportingDashboardPage),
  data: {
    title: 'Enterprise Reporting Dashboard',
  },
} as ReactStateDeclaration);

function SuccessMetricsRoute(): JSX.Element {
  return (
    <main className="nx-page-main">
      <SuccessMetricsReportListContainer />
    </main>
  );
}

const NATIVE_CLASSIC_COMPONENTS: Partial<Record<ComingSoonModuleSlug, React.ComponentType>> = {
  'success-metrics': mountClassicComponent(SuccessMetricsRoute),
  api: mountClassicComponent(ApiPage),
  reports: mountClassicComponent(ReportingRoute),
};

NATIVE_CLASSIC_EMBED_SLUGS.forEach((slug) => {
  if (!(slug in NATIVE_CLASSIC_COMPONENTS)) {
    throw new Error(`NATIVE_CLASSIC_EMBED_SLUGS lists '${slug}' but NATIVE_CLASSIC_COMPONENTS has no entry for it`);
  }
});

Object.keys(NATIVE_CLASSIC_COMPONENTS).forEach((slug) => {
  if (!NATIVE_CLASSIC_EMBED_SLUGS.includes(slug as ComingSoonModuleSlug)) {
    throw new Error(`NATIVE_CLASSIC_COMPONENTS has '${slug}' but NATIVE_CLASSIC_EMBED_SLUGS has no entry for it`);
  }
});

function LabsContainer(): JSX.Element {
  return (
    <ClassicComponentMount>
      <main className="nx-page-main">
        <UIView />
      </main>
    </ClassicComponentMount>
  );
}

router.stateRegistry.register({
  name: 'labs',
  abstract: true,
  url: '',
  component: LabsContainer,
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'labs.successMetricsReport',
  url: '/coming-soon/success-metrics/{successMetricsReportId}',
  component: SuccessMetricsReportContainer,
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'labs.successMetrics',
  url: '/_classic-aliases/labs/successMetrics',
  redirectTo: comingSoonStateName('success-metrics'),
  data: { title: 'Success Metrics' },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'enterpriseReporting',
  url: '/_classic-aliases/enterpriseReporting',
  redirectTo: comingSoonStateName('reports'),
  data: { title: 'Enterprise Reporting' },
} as ReactStateDeclaration);

router.stateRegistry.register({ name: 'dashboard', abstract: true, url: '' });
router.stateRegistry.register({ name: 'dashboard.overview', abstract: true, url: '' });
router.stateRegistry.register({
  name: 'dashboard.overview.violations',
  url: '/_classic-aliases/dashboard/overview/violations',
  redirectTo: 'nexusOneDashboard.violations',
  data: { title: 'Dashboard' },
} as ReactStateDeclaration);

COMING_SOON_MODULE_ORDER.forEach((slug) => {
  const ClassicComponent = NATIVE_CLASSIC_COMPONENTS[slug];
  if (ClassicComponent) {
    const module = COMING_SOON_MODULES[slug];
    router.stateRegistry.register({
      name: comingSoonStateName(slug),
      url: comingSoonHref(slug),
      component: ClassicComponent,
      data: {
        title: module.label,
      },
    } as ReactStateDeclaration);
    return;
  }

  router.stateRegistry.register({
    name: comingSoonStateName(slug),
    url: comingSoonHref(slug),
    component: ComingSoonRoute,
    data: {
      title: 'Nexus One',
    },
  } as ReactStateDeclaration);
});

router.urlService.rules.otherwise(NEXUS_ONE_DEFAULT_PATH);
