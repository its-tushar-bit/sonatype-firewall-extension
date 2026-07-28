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
import ProductLicenseContainer from 'MainRoot/configuration/license/ProductLicenseContainer';
import GettingStartedContainer from 'MainRoot/configuration/gettingStarted/GettingStartedContainer';
import UserManagementContainer from 'MainRoot/security/users/UserManagementContainer';
import UserAddContainer from 'MainRoot/security/users/userConfiguration/UserAddContainer';
import UserEditContainer from 'MainRoot/security/users/userConfiguration/UserEditContainer';
import UserActivityDetailsContainer from 'MainRoot/configuration/userActivityOverview/UserActivityDetailsContainer';
import SystemNoticeConfigurationContainer from 'MainRoot/configuration/systemNoticeConfiguration/SystemNoticeConfigurationContainer';
import AuthorizedAdvancedSearchConfig from 'MainRoot/nexus-one/AuthorizedAdvancedSearchConfig';
import AdministratorsConfig from 'MainRoot/configuration/administrators/config/AdministratorsConfig';
import AdministratorsEdit from 'MainRoot/configuration/administrators/edit/AdministratorsEdit';
import ApiPage from 'MainRoot/api/ApiPage';
import HostedReposPage from 'MainRoot/hostedRepos/HostedReposPage';
import HostedReposListPage from 'MainRoot/hostedRepos/HostedReposListPage';
import RepositoryComponentsList from 'MainRoot/hostedRepos/RepositoryComponentsList';
import LegalDashboardContainer from 'MainRoot/legal/dashboard/LegalDashboardContainer';
import {
  LEGAL_APPLICATIONS_DASHBOARD_DATA,
  LEGAL_APPLICATIONS_DASHBOARD_URL,
  LEGAL_COMPONENTS_DASHBOARD_DATA,
  LEGAL_COMPONENTS_DASHBOARD_URL,
} from 'MainRoot/legal/dashboard/legalDashboardRouteData';
import { LEGAL_DEEP_LINK_STATES } from 'MainRoot/legal/legalDeepLinkStates';
import {
  ORGS_AND_POLICIES_STATES,
  ORGS_AND_POLICIES_CHROME_COMPONENTS,
  toManagementStateRegistration,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStates';
import { mountOrgsAndPoliciesChrome } from 'MainRoot/nexus-one/OrgsAndPoliciesEmbedMount';
import InnerSourceRepositoryBaseConfigurations from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryBaseConfigurations';
import ArtifactoryRepositoryBaseConfigurations from 'MainRoot/artifactoryRepositoryConfiguration/ArtifactoryRepositoryBaseConfigurations';
import React2ShellPage from 'MainRoot/report/react2shell/React2ShellPage';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';
import { ReportingRoute } from 'MainRoot/nexus-one/ReportingRoute';
import { ClassicComponentMount, mountClassicComponent } from 'MainRoot/nexus-one/ClassicComponentMount';
import {
  NATIVE_CLASSIC_EMBED_SLUGS,
  embeddedHref,
  isNativeClassicEmbedSlug,
  usesEmbeddedHrefPrimary,
} from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import { NEXUS_ONE_DEFAULT_PATH } from 'MainRoot/nosc/routing/classicPreviewMap';
import PreviewUiSettingsPage from 'MainRoot/nosc/settings/PreviewUiSettingsPage';
import { nexusOneDashboardStates } from 'MainRoot/nexus-one/nexusOneDashboardStates';
import { nexusOneApplicationDetailStates } from 'MainRoot/nexus-one/nexusOneApplicationDetailStates';
import { nexusOneApplicationReportStates } from 'MainRoot/nexus-one/nexusOneApplicationReportStates';
import { nexusOneViolationDetailStates } from 'MainRoot/nexus-one/nexusOneViolationDetailStates';
import { nexusOneComponentDetailStates } from 'MainRoot/nexus-one/nexusOneComponentDetailStates';
import { nexusOneVulnerabilityDetailStates } from 'MainRoot/nexus-one/nexusOneVulnerabilityDetailStates';
import BaseUrlConfiguration from 'MainRoot/configuration/baseUrl/BaseUrlConfiguration';
// NOTE: the three side-import route modules below call router.stateRegistry.register(...) at import
// time with no stateRegistry.get() idempotency guard, so this module (and any test) must import each
// exactly once. That holds today because they are only pulled into the single nexus-one bundle entry;
// a second import in the same runtime (e.g. a jest that imports both nexus-one/routes and one of these
// modules directly) would throw a duplicate-registration error. Keep them single-entry.
// Classic applicationReport child states (component details, raw data, etc.) so
// in-report links from the embedded ReportPage resolve inside the Nexus One bundle.
import 'MainRoot/applicationReport/route';
import { setApplicationReportRootWrapper } from 'MainRoot/applicationReport/applicationReportNexusOneShell';
// Classic Component Details / Application Report styles (License Detections hanging
// indent, etc.) — N1 does not load scss/scss.scss. Same pattern as orgsAndPoliciesEmbed.
import 'MainRoot/scss/applicationReportEmbed.scss';
// Offset applicationReport.* (Component Details Legal, etc.) inside NOUX chrome —
// without this, `.nx-page-main` underruns LeftNav when deep-linking into the tree.
setApplicationReportRootWrapper((node) => <ClassicComponentMount>{node}</ClassicComponentMount>);
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
import PreviewLegalList from 'MainRoot/nosc/legal/LegalList';
import PreviewComponentsList from 'MainRoot/nosc/componentsList/ComponentsList';
import PreviewVulnerabilitiesList from 'MainRoot/nosc/vulnerabilities/VulnerabilitiesList';
import {
  NEXUS_ONE_VIOLATIONS_STATE_NAME,
  NEXUS_ONE_VIOLATIONS_URL,
} from 'MainRoot/nosc/violations/violationsRoute';
import {
  NEXUS_ONE_LEGAL_STATE_NAME,
  NEXUS_ONE_LEGAL_URL,
} from 'MainRoot/nosc/legal/legalRoute';
import {
  NEXUS_ONE_COMPONENTS_STATE_NAME,
  NEXUS_ONE_COMPONENTS_URL,
} from 'MainRoot/nosc/componentsList/componentsRoute';
import {
  NEXUS_ONE_VULNERABILITIES_STATE_NAME,
  NEXUS_ONE_VULNERABILITIES_URL,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';
import {
  WaiversListPage as PreviewWaiversList,
  WaiverDetailPage as PreviewWaiverDetail,
} from 'MainRoot/nosc/waivers';
import { isAuthorized } from 'MainRoot/util/permissionService';
import { selectIsHostedRepositoryEvaluationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsLicensed } from 'MainRoot/productFeatures/productLicenseSelectors';

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
  url: '/applications?q&sort&page&stage&org&app&threat',
  component: PreviewApplicationsList,
  data: {
    title: 'Nexus One — Applications',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneRepositories',
  url: '/repositories',
  component: mountClassicComponent(HostedReposRoute),
  data: {
    title: 'Nexus One — Repositories',
    featureEnabled: selectIsHostedRepositoryEvaluationEnabled,
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneRepositoriesDetail',
  url: '/repositories/{repositoryManagerId}',
  component: mountClassicComponent(HostedReposListPage),
  data: {
    title: 'Nexus One — Repository',
    featureEnabled: selectIsHostedRepositoryEvaluationEnabled,
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'nexusOneRepositoriesComponents',
  url: '/repositories/{repositoryManagerId}/{repositoryId}/components?{repositoryPublicId}',
  component: mountClassicComponent(RepositoryComponentsList),
  data: {
    title: 'Nexus One — Repository Components',
    featureEnabled: selectIsHostedRepositoryEvaluationEnabled,
  },
} as ReactStateDeclaration);

// Native Component detail (CLM-42767) — register before app-detail tab children
// are matched for the same /applications/{id}/components prefix where possible.
nexusOneComponentDetailStates().forEach((state) => {
  router.stateRegistry.register(state);
});

// Application detail: abstract parent shell + one child state per tab (CLM-40901).
nexusOneApplicationDetailStates().forEach((state) => {
  router.stateRegistry.register(state);
});

// Embedded Classic application policy report (CLM-41538).
nexusOneApplicationReportStates().forEach((state) => {
  router.stateRegistry.register(state);
});

// Native Nexus One violation detail (CLM-42256 / CLM-42765).
nexusOneViolationDetailStates().forEach((state) => {
  router.stateRegistry.register(state);
});

// Native Vulnerability detail (CLM-42769).
nexusOneVulnerabilityDetailStates().forEach((state) => {
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
// (CLM-42254). Sibling of the native violation detail state registered at /violations/{id}
// (nexusOneViolationDetail, CLM-42256) — the card drill-in target. The optional query params persist
// search + sidebar filters + page in the hash for bookmarks/back-forward (CLM-42260); name + url come
// from violationsRoute.ts so the container's round-trip go(...) calls can't drift from this state.
router.stateRegistry.register({
  name: NEXUS_ONE_VIOLATIONS_STATE_NAME,
  url: NEXUS_ONE_VIOLATIONS_URL,
  component: PreviewViolationsList,
  data: { title: 'Nexus One — Violations' },
} as ReactStateDeclaration);

// Nexus One Legal V1 — LEGAL_VIOLATION license-risk triage at /legal (CLM-43207).
// Owns the clean path; Classic ALP dashboard remains at legal.applicationsDashboard.
router.stateRegistry.register({
  name: NEXUS_ONE_LEGAL_STATE_NAME,
  url: NEXUS_ONE_LEGAL_URL,
  component: PreviewLegalList,
  data: { title: 'Nexus One — Legal' },
} as ReactStateDeclaration);

// Martha V1 Vulnerabilities list, wired to POST /rest/dashboard/vulnerabilities/list.
// Sibling of native detail at /vulnerabilities/{vulnId} (nexusOneVulnerabilityDetail).
router.stateRegistry.register({
  name: NEXUS_ONE_VULNERABILITIES_STATE_NAME,
  url: NEXUS_ONE_VULNERABILITIES_URL,
  component: PreviewVulnerabilitiesList,
  data: { title: 'Nexus One — Vulnerabilities' },
} as ReactStateDeclaration);

// Martha V1 Components portfolio list (CLM-42214). Query params match Applications so
// search/sort/filters/page can persist in the hash in follow-up slices.
router.stateRegistry.register({
  name: NEXUS_ONE_COMPONENTS_STATE_NAME,
  url: NEXUS_ONE_COMPONENTS_URL,
  component: PreviewComponentsList,
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

function HostedReposRoute(): JSX.Element {
  return (
    <main className="nx-page-main">
      <HostedReposPage />
    </main>
  );
}

// HostedReposListPage and RepositoryComponentsList render their own `.nx-page-main`
// (HostedReposListPage's <main>, RepositoryComponentsList's <NxPageMain>), so they mount
// directly — wrapping them in another <main> nested two landmarks (invalid HTML + WCAG).
// HostedReposRoute above stays wrapped because HostedReposPage renders a bare <div>. CLM-42184.

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
type ClassicEmbedRedirect = string | { readonly state: string; readonly params: Record<string, string> };

const NATIVE_CLASSIC_EMBED_REDIRECTS: Partial<Record<ComingSoonModuleSlug, ClassicEmbedRedirect>> = {
  // LeftNav / deep-dive target /legal (nexusOneLegal LEGAL_VIOLATION triage). Stale /coming-soon/legal
  // bookmarks redirect there; Classic ALP dashboard stays at legal.applicationsDashboard. CLM-43207.
  legal: NEXUS_ONE_LEGAL_STATE_NAME,
  // LeftNav targets /repositories (nexusOneRepositories); the /coming-soon/repositories entry
  // redirects to that canonical route so stale Coming Soon bookmarks land on the highlighted,
  // feature-gated page instead of a parallel mount. CLM-42184.
  repositories: 'nexusOneRepositories',
  // Orgs and Policies embeds the Classic `management.*` tree (see ORGS_AND_POLICIES_STATES below).
  // Its entry redirects to the root org's summary page; organizationId is a required param, so this
  // is an object redirect rather than a bare state name like Legal's. 'ROOT_ORGANIZATION_ID' mirrors
  // the classicHref in comingSoonModules.ts (the legacy UI uses the literal, not the guide constant,
  // to avoid a guide <-> IQ-UI cross-import).
  'orgs-and-policies': { state: 'management.view.organization', params: { organizationId: 'ROOT_ORGANIZATION_ID' } },
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
  if (!isNativeClassicEmbedSlug(slug as ComingSoonModuleSlug)) {
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

// Register the Classic Orgs and Policies (`management.*`) state tree in the Nexus One bundle's own
// router instance from the shared source of truth (orgsAndPoliciesStates.ts), so the embedded org
// pages and all their in-page navigation (org tree, policies, access, monitoring, source control,
// ...) resolve in-shell instead of bouncing to Classic. selectRoutePrefix returns '' for these
// canonical `management.*` names, so ownerSideNav / breadcrumb links target these registrations.
// Chrome-carrying states (the ORGS_AND_POLICIES_CHROME_COMPONENTS set - see its JSDoc in
// orgsAndPoliciesStates.ts) mount inside the embed chrome, memoized per component so switching owner
// types never remounts it; every deeper state registers with its bare component.
const mountedOrgsAndPoliciesComponents = new Map<React.ComponentType, React.ComponentType>();

function mountOrgsAndPoliciesChromeOnce(Component: React.ComponentType): React.ComponentType {
  let mounted = mountedOrgsAndPoliciesComponents.get(Component);
  if (!mounted) {
    mounted = mountOrgsAndPoliciesChrome(Component);
    mountedOrgsAndPoliciesComponents.set(Component, mounted);
  }
  return mounted;
}

ORGS_AND_POLICIES_STATES.forEach((stateDef) => {
  const component = ORGS_AND_POLICIES_CHROME_COMPONENTS.has(stateDef.component)
    ? mountOrgsAndPoliciesChromeOnce(stateDef.component)
    : stateDef.component;
  router.stateRegistry.register(toManagementStateRegistration(stateDef, component));
});

// The owner summary's InnerSource / Artifactory tile "Edit" buttons stateGo to these sibling state
// trees (repositoryBaseConfigurations.* / artifactoryRepositoryBaseConfigurations.*, defined in the
// Classic <feature>/route.js files). Register them here, wrapped in the embed mount so the standalone
// edit pages render in-shell; without them the Edit buttons stateGo to an unregistered state and do
// nothing. The abstract parent renders the page component; its per-owner-type children only add the
// url params, mirroring the Classic registrations - including each parent's `isDirty` metadata so
// installDirtyGuard opens UnsavedChangesModal on leave (repositoryBaseConfigurations' dirty flag
// lives in the innerSourceRepositoryBaseConfigurations slice, matching Classic).
[
  {
    abstractName: 'repositoryBaseConfigurations',
    component: InnerSourceRepositoryBaseConfigurations,
    title: 'Repository Configurations',
    childSuffix: 'repositoryBaseConfigurations',
    isDirtyStateKey: 'innerSourceRepositoryBaseConfigurations',
  },
  {
    abstractName: 'artifactoryRepositoryBaseConfigurations',
    component: ArtifactoryRepositoryBaseConfigurations,
    title: 'Artifactory Repository Configurations',
    childSuffix: 'artifactoryRepositoryBaseConfigurations',
    isDirtyStateKey: 'artifactoryRepositoryBaseConfigurations',
  },
].forEach(({ abstractName, component, title, childSuffix, isDirtyStateKey }) => {
  router.stateRegistry.register({
    name: abstractName,
    abstract: true,
    url: '/management/edit',
    component: mountClassicComponent(component),
    data: { title, isDirty: [isDirtyStateKey, 'isDirty'] },
  } as ReactStateDeclaration);
  ['organization', 'application'].forEach((ownerType) => {
    router.stateRegistry.register({
      name: `${abstractName}.${ownerType}`,
      url: `/${ownerType}/{${ownerType}Id}/${childSuffix}`,
    } as ReactStateDeclaration);
  });
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
  url: '/success-metrics/{successMetricsReportId}',
  component: SuccessMetricsReportContainer,
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'labs.successMetricsReportLegacyPath',
  url: '/coming-soon/success-metrics/{successMetricsReportId}',
  redirectTo: (trans) => ({
    state: 'labs.successMetricsReport',
    params: { successMetricsReportId: trans.params().successMetricsReportId },
  }),
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'labs.successMetrics',
  url: '/_classic-aliases/labs/successMetrics',
  redirectTo: comingSoonStateName('success-metrics'),
  data: { title: 'Success Metrics' },
} as ReactStateDeclaration);

const requireConfigureSystem = async () => {
  const authorized = await isAuthorized(['CONFIGURE_SYSTEM']);
  return authorized ? undefined : 'nexusOneDashboard.violations';
};

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
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Success Metrics Configuration',
    isDirty: ['successMetricsConfiguration', 'viewState', 'isDirty'],
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'productlicense',
  url: '/productlicense',
  // mountClassicComponent applies shell offsets — see successMetricsConfiguration above.
  component: mountClassicComponent(ProductLicenseContainer),
  // CONFIGURE_SYSTEM is the only gate here: a system admin is who installs a license,
  // and license state is intentionally NOT checked (mirroring Classic's NON_PROTECTED_PATHS
  // in RouteProductLicenseValidator, which lets an authenticated-but-unlicensed admin
  // reach this page to install one). ensureNexusOneShellAccess gates the shell on
  // authenticated + `preview-nexus-one-ui` but does not verify a license.
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Product License',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  // Registered so ProductLicense.jsx's first-install redirect
  // (`window.location.href = '#/gettingStarted'`) lands on the embedded page
  // instead of falling through the NOUX otherwise-rule to `/dashboard`.
  name: 'gettingStarted',
  url: '/gettingStarted',
  // mountClassicComponent applies shell offsets — see successMetricsConfiguration above.
  component: mountClassicComponent(GettingStartedContainer),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Getting Started',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'users',
  url: '/users',
  component: mountClassicComponent(UserManagementContainer),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Users',
  },
} as ReactStateDeclaration);

// Activity tab lives at /users/activity. Classic registers it as a child of `users`
// (see security/route.js), so UI-Router keeps the parent's mounted
// UserManagementContainer; the container selects the Activity tab by checking
// that router.currentState.name === 'users.activity'.
// redirectTo is required here: @uirouter/core's onStart hook reads trans.to().redirectTo
// (the leaf state only), so the parent `users` guard does NOT fire for a direct nav to
// this child state.
router.stateRegistry.register({
  name: 'users.activity',
  url: '/activity',
  redirectTo: requireConfigureSystem,
  data: {
    title: 'User Activity',
  },
} as ReactStateDeclaration);

// Clicking a row in the Activity view dispatches
// stateGo('userActivityDetails', {username}). This state uses its own container.
// UserActivityDetailsContainer declares isAuthorized as a required prop; users who
// reach this state have already passed the redirectTo gate so it is always true.
// Note: Classic resolves isAuthorized and passes it as a prop, showing an inline
// error for users with CONFIGURE_SYSTEM but not ACCESS_AUDIT_LOG. NOUX redirects
// those partial-permission users to nexusOneDashboard.violations instead — a
// deliberate UX improvement over the inline error banner.
const AuthorizedUserActivityDetails = () => <UserActivityDetailsContainer isAuthorized={true} />;
router.stateRegistry.register({
  name: 'userActivityDetails',
  url: '/users/activity/{username}',
  component: mountClassicComponent(AuthorizedUserActivityDetails),
  redirectTo: async () => {
    const authorized = await isAuthorized(['CONFIGURE_SYSTEM', 'ACCESS_AUDIT_LOG']);
    return authorized ? undefined : 'nexusOneDashboard.violations';
  },
  data: {
    title: 'User Activity Details',
  },
} as ReactStateDeclaration);

// UserList's "Create User" button dispatches stateGo('createUser').
router.stateRegistry.register({
  name: 'createUser',
  url: '/users/_new_',
  component: mountClassicComponent(UserAddContainer),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Add New User',
    isDirty: ['userConfiguration', 'isDirty'],
  },
} as ReactStateDeclaration);

// UserListItem links to editUser via history.href(prefixRoute('editUser'), {userId}).
router.stateRegistry.register({
  name: 'editUser',
  url: '/users/{userId}',
  component: mountClassicComponent(UserEditContainer),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Edit User',
    isDirty: ['userConfiguration', 'isDirty'],
  },
} as ReactStateDeclaration);

// Standalone User Activity view — shown when isUserActivityTrackingEnabled but
// !isUserManagementEnabled (see PreviewSystemPreferencesMenu.tsx gear entry).
// UserManagement.jsx computes showActivityOnly = isUserActivityTrackingEnabled && !isUserManagementEnabled
// from selectors and renders only UserActivityOverviewContainer when true.
router.stateRegistry.register({
  name: 'userActivity',
  url: '/user-activity',
  component: mountClassicComponent(UserManagementContainer),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'User Activity',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'baseUrlConfiguration',
  url: '/baseUrl',
  // mountClassicComponent applies shell offsets — see successMetricsConfiguration above.
  component: mountClassicComponent(BaseUrlConfiguration),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Base URL Configuration',
    isDirty: ['baseUrlConfiguration', 'isDirty'],
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'systemNoticeConfiguration',
  url: '/systemNoticeConfiguration',
  // mountClassicComponent applies shell offsets — see successMetricsConfiguration above.
  component: mountClassicComponent(SystemNoticeConfigurationContainer),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'System Notice',
    isDirty: ['systemNoticeConfiguration', 'viewState', 'isDirty'],
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'administrators',
  url: '/administrators',
  // mountClassicComponent applies shell offsets — see successMetricsConfiguration above.
  component: mountClassicComponent(AdministratorsConfig),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Administrator Config',
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'administratorsEdit',
  url: '/administrators/{roleId}',
  // mountClassicComponent applies shell offsets — see successMetricsConfiguration above.
  component: mountClassicComponent(AdministratorsEdit),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Administrator Edit',
    isDirty: ['administratorsConfig', 'isDirty'],
  },
} as ReactStateDeclaration);

router.stateRegistry.register({
  name: 'advancedSearchConfig',
  url: '/advancedSearchConfig',
  component: mountClassicComponent(AuthorizedAdvancedSearchConfig),
  redirectTo: requireConfigureSystem,
  data: {
    title: 'Advanced Search Configuration',
    isDirty: ['advancedSearchConfig', 'viewState', 'isDirty'],
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
  // Legacy alias exists iff the primary URL moved off /coming-soon/.
  const usesClean = usesEmbeddedHrefPrimary(slug);
  const name = comingSoonStateName(slug);
  const primaryUrl = usesClean ? embeddedHref(slug) : comingSoonHref(slug);

  const redirectTo = NATIVE_CLASSIC_EMBED_REDIRECTS[slug];
  const ClassicComponent = NATIVE_CLASSIC_COMPONENTS[slug];
  const registration = (
    redirectTo
      ? { name, url: primaryUrl, redirectTo, data: { title: module.label } }
      : ClassicComponent
        ? { name, url: primaryUrl, component: ClassicComponent, data: { title: module.label } }
        : { name, url: primaryUrl, component: ComingSoonRoute, data: { title: 'Nexus One' } }
  ) as ReactStateDeclaration;

  router.stateRegistry.register(registration);

  if (usesClean) {
    router.stateRegistry.register({
      name: `${name}LegacyPath`,
      url: comingSoonHref(slug),
      redirectTo: name,
      data: { title: module.label },
    } as ReactStateDeclaration);
  }
});

router.urlService.rules.otherwise(NEXUS_ONE_DEFAULT_PATH);
