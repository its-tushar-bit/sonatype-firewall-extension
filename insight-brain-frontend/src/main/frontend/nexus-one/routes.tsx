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
import SuccessMetricsConfiguration from 'MainRoot/configuration/successMetricsConfiguration/SuccessMetricsConfiguration';
import ApiPage from 'MainRoot/api/ApiPage';
import LegalDashboardContainer from 'MainRoot/legal/dashboard/LegalDashboardContainer';
import {
  LEGAL_APPLICATIONS_DASHBOARD_DATA,
  LEGAL_APPLICATIONS_DASHBOARD_URL,
  LEGAL_COMPONENTS_DASHBOARD_DATA,
  LEGAL_COMPONENTS_DASHBOARD_URL,
} from 'MainRoot/legal/dashboard/legalDashboardRouteData';
import { LEGAL_DEEP_LINK_STATES } from 'MainRoot/legal/legalDeepLinkStates';
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
import PreviewViolationsList from 'MainRoot/nosc/violations/ViolationsList';
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

// Martha V1 Violations card list (CLM-42257), wired to POST /rest/dashboard/violations/list
// (CLM-42254). Sibling of the embedded violation detail state registered at /violations/{id}
// (nexusOneViolationDetail, CLM-42256) — the card drill-in target.
router.stateRegistry.register({
  name: 'nexusOneViolations',
  url: '/violations',
  component: PreviewViolationsList,
  data: { title: 'Nexus One — Violations' },
} as ReactStateDeclaration);

// Interim top-level aliases until native entity-list pages own these paths (CLM-40905).
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

// Single reference reused by both legal.*Dashboard states below. They're both children of
// the same abstract 'legal' UIView, so sharing one reference means the inner UIView's rendered
// element type stays identical across an Applications <-> Components tab switch and React
// updates in place instead of unmounting. This does NOT cover the Coming Soon entry itself —
// that's a separate, shallower UIView position (see NATIVE_CLASSIC_EMBED_REDIRECTS below for
// why the entry never mounts this component at all, avoiding that mismatch entirely).
const LegalDashboardMount = mountClassicComponent(LegalDashboardContainer);

const NATIVE_CLASSIC_COMPONENTS: Partial<Record<ComingSoonModuleSlug, React.ComponentType>> = {
  'success-metrics': mountClassicComponent(SuccessMetricsRoute),
  api: mountClassicComponent(ApiPage),
  reports: mountClassicComponent(ReportingRoute),
};

/**
 * Slugs whose Coming Soon entry redirects straight to another state instead of mounting a
 * component of its own. Legal's dashboard switches tabs via
 * stateGo('legal.applicationsDashboard' | 'legal.componentsDashboard') — both children of the
 * abstract 'legal' state, one UIView level deeper than the flat Coming Soon entry state. If the
 * entry mounted LegalDashboardMount directly (like the other native slugs do), the root UIView's
 * resolved component would change type from LegalDashboardMount to UIView on the very first tab
 * transition (entering the 'legal' parent adds a nesting level), forcing an unmount/remount
 * despite the shared reference above. Redirecting means the entry never renders anything itself,
 * so the user always lands directly on a legal.*Dashboard child — no such transition ever happens.
 */
const NATIVE_CLASSIC_EMBED_REDIRECTS: Partial<Record<ComingSoonModuleSlug, string>> = {
  legal: 'legal.applicationsDashboard',
};

NATIVE_CLASSIC_EMBED_SLUGS.forEach((slug) => {
  if (!(slug in NATIVE_CLASSIC_COMPONENTS) && !(slug in NATIVE_CLASSIC_EMBED_REDIRECTS)) {
    throw new Error(
      `NATIVE_CLASSIC_EMBED_SLUGS lists '${slug}' but neither NATIVE_CLASSIC_COMPONENTS nor ` +
        'NATIVE_CLASSIC_EMBED_REDIRECTS has an entry for it',
    );
  }
});

// Reverse of the guard above: catches a slug left behind in either map after being removed from
// NATIVE_CLASSIC_EMBED_SLUGS. The COMING_SOON_MODULE_ORDER loop below reads both maps directly by
// slug — it never consults NATIVE_CLASSIC_EMBED_SLUGS — so a stale entry in EITHER map (not just
// NATIVE_CLASSIC_COMPONENTS) still mounts/redirects for a slug the embed list says isn't embedded.
[...Object.keys(NATIVE_CLASSIC_COMPONENTS), ...Object.keys(NATIVE_CLASSIC_EMBED_REDIRECTS)].forEach((slug) => {
  if (!NATIVE_CLASSIC_EMBED_SLUGS.includes(slug as ComingSoonModuleSlug)) {
    throw new Error(
      `NATIVE_CLASSIC_COMPONENTS or NATIVE_CLASSIC_EMBED_REDIRECTS has an entry for '${slug}' but ` +
        'NATIVE_CLASSIC_EMBED_SLUGS does not list it',
    );
  }
});

// Register the same state names/urls/data Classic's own legal/route.js uses (see
// legalDashboardRouteData.ts) in the Nexus One bundle's own router instance, so Legal's in-page
// tab clicks resolve in-shell instead of failing silently. The dotted child names require this
// abstract 'legal' parent to exist first, same as Classic's own registration.
router.stateRegistry.register({
  name: 'legal',
  abstract: true,
  component: UIView,
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'legal.applicationsDashboard',
  url: LEGAL_APPLICATIONS_DASHBOARD_URL,
  component: LegalDashboardMount,
  data: LEGAL_APPLICATIONS_DASHBOARD_DATA,
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'legal.componentsDashboard',
  url: LEGAL_COMPONENTS_DASHBOARD_URL,
  component: LegalDashboardMount,
  data: LEGAL_COMPONENTS_DASHBOARD_DATA,
} as ReactStateDeclaration);

// Every other legal.* state — application details (the dashboard's Applications-tab row-click
// target), component overview (Components-tab row-click target) and its other entry-point
// shapes, attribution report generation, and the copyright/notice/license-file/license-details
// deep-link families — is defined once in legalDeepLinkStates.ts and shared with Classic's own
// legal/route.js, so the two can't drift apart.
//
// Memoize the mount per underlying Classic component so states that reuse one component (e.g.
// every component-overview entry-point shape, or the three notice-details abstract parents that
// all render ComponentNoticeDetails) share the same reference — same reasoning as
// LegalDashboardMount above: a distinct mountClassicComponent(...) wrapper per state would force
// a remount every time the user navigates between two states that render the same component.
//
// A 2-segment name ('legal.foo') is either a flat page or an abstract parent whose own component
// needs the ClassicComponentMount chrome. A 3-segment name ('legal.foo.bar') is always the dotted
// child of one of those abstract parents; its component renders inside the parent's own nested
// <UIView /> (confirmed for each in legalDeepLinkStates.ts's doc comment), so mounting it in a
// second ClassicComponentMount here would double-wrap the page.
const mountedLegalComponents = new Map<React.ComponentType, React.ComponentType>([
  [LegalDashboardContainer, LegalDashboardMount],
]);

function mountLegalComponentOnce(Component: React.ComponentType): React.ComponentType {
  let mounted = mountedLegalComponents.get(Component);
  if (!mounted) {
    mounted = mountClassicComponent(Component);
    mountedLegalComponents.set(Component, mounted);
  }
  return mounted;
}

LEGAL_DEEP_LINK_STATES.forEach((stateDef) => {
  const isDottedChildOfAbstractParent = stateDef.name.split('.').length > 2;
  router.stateRegistry.register({
    name: stateDef.name,
    url: stateDef.url,
    component: isDottedChildOfAbstractParent ? stateDef.component : mountLegalComponentOnce(stateDef.component),
    ...(stateDef.data ? { data: stateDef.data } : {}),
    ...(stateDef.abstract ? { abstract: true } : {}),
  } as ReactStateDeclaration);
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
  name: 'successMetricsConfiguration',
  url: '/successMetricsConfiguration',
  component: mountClassicComponent(SuccessMetricsConfiguration),
  // mountClassicComponent applies the shell's fixed-position offsets via
  // usePreviewShellOffsets — same primitive every native NOSC page uses to sit
  // clear of the LeftNav + TopNav. Without it, .nx-page-main's `grid-area`
  // declaration falls through (NOSC shell isn't a CSS Grid ancestor) and
  // content underruns the sidebar. Sibling api and labs.successMetrics routes
  // use the same wrapper.
  redirectTo: async () => {
    const authorized = await isAuthorized(['CONFIGURE_SYSTEM']);
    return authorized ? undefined : 'nexusOneDashboard.violations';
  },
  data: {
    title: 'Success Metrics Configuration',
    isDirty: ['successMetricsConfiguration', 'viewState', 'isDirty'],
  },
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
  const module = COMING_SOON_MODULES[slug];

  const redirectTo = NATIVE_CLASSIC_EMBED_REDIRECTS[slug];
  if (redirectTo) {
    router.stateRegistry.register({
      name: comingSoonStateName(slug),
      url: comingSoonHref(slug),
      redirectTo,
      data: { title: module.label },
    } as ReactStateDeclaration);
    return;
  }

  const ClassicComponent = NATIVE_CLASSIC_COMPONENTS[slug];
  if (ClassicComponent) {
    router.stateRegistry.register({
      name: comingSoonStateName(slug),
      url: comingSoonHref(slug),
      component: ClassicComponent,
      data: { title: module.label },
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
