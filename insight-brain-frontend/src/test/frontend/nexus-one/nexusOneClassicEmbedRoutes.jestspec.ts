/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
jest.mock('MainRoot/util/permissionService', () => ({
  isAuthorized: jest.fn(),
}));

jest.mock('MainRoot/productFeatures/productFeaturesSelectors', () => ({
  ...jest.requireActual('MainRoot/productFeatures/productFeaturesSelectors'),
  selectIsEmailConfigurationEnabled: jest.fn(),
}));

import router from 'MainRoot/router/routerInstance';
import {
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
  comingSoonStateName,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import {
  embeddedHref,
  isNativeClassicEmbedSlug,
  NATIVE_CLASSIC_EMBED_SLUGS,
  usesEmbeddedHrefPrimary,
} from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import { LEGAL_DEEP_LINK_STATES } from 'MainRoot/legal/legalDeepLinkStates';
import {
  ORGS_AND_POLICIES_STATES,
  ORGS_AND_POLICIES_CHROME_COMPONENTS,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStates';
import OwnersTreePage from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnersTreePage';
import PolicyEditor from 'MainRoot/OrgsAndPolicies/policyEditor/PolicyEditor';
import { NEXUS_ONE_APPLICATION_REPORT_STATE } from 'MainRoot/nexus-one/nexusOneApplicationReportStates';
import { isAuthorized } from 'MainRoot/util/permissionService';
import { selectIsEmailConfigurationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { NEXUS_ONE_VIOLATION_DETAIL_STATE } from 'MainRoot/nexus-one/nexusOneViolationDetailStates';
import {
  NEXUS_ONE_VIOLATIONS_STATE_NAME,
  NEXUS_ONE_VIOLATIONS_URL,
} from 'MainRoot/nosc/violations/violationsRoute';
import rootReducer from 'MainRoot/reduxConfig/reducers';
import 'MainRoot/nexus-one/routes';

// Walks the state.data.isDirty path array against the real rootReducer initial state
// and asserts the terminal field is a boolean. Guards against slice-restructuring
// silently breaking the dirty guard: a typo or moved field fails here rather than
// silently disabling the unsaved-changes modal at runtime.
function expectIsDirtyPathResolvesToBoolean(isDirtyPath: ReadonlyArray<string> | undefined) {
  const rootState = rootReducer(undefined, { type: '@@INIT' });
  const [slice, field] = isDirtyPath ?? [];
  expect(typeof (rootState as Record<string, Record<string, unknown>>)[slice]?.[field]).toBe('boolean');
}

describe('nexusOneClassicEmbedRoutes', () => {
  it('registers native Classic mounts / redirects for embedded slugs and Coming Soon stubs for the rest', () => {
    COMING_SOON_MODULE_ORDER.forEach((slug) => {
      const state = router.stateRegistry.get(comingSoonStateName(slug));
      expect(state).toBeDefined();
      const expectedPrimaryUrl = usesEmbeddedHrefPrimary(slug) ? embeddedHref(slug) : comingSoonHref(slug);
      expect(state?.url).toBe(expectedPrimaryUrl);

      if (!isNativeClassicEmbedSlug(slug)) {
        expect(state?.component).toBe(ComingSoonRoute);
      } else if (slug === 'legal') {
        // Clean /legal is owned by nexusOneLegal (LEGAL_VIOLATION triage). Coming Soon entry redirects there.
        expect(state?.component).toBeUndefined();
        expect(state?.redirectTo).toBe('nexusOneLegal');
      } else if (slug === 'orgs-and-policies' || slug === 'policies') {
        // Orgs and Policies / Policies entries redirect into the embedded management tree.
        expect(state?.component).toBeUndefined();
        expect(state?.redirectTo).toEqual({
          state: 'management.view.organization',
          params: { organizationId: 'ROOT_ORGANIZATION_ID' },
        });
      } else if (slug === 'repositories') {
        // Dedicated native route owns /repositories; Coming Soon entry stays the bookmark alias.
        expect(state?.redirectTo).toBe('nexusOneRepositories');
      } else if (slug === 'waiver-requests') {
        expect(state?.component).toBeUndefined();
        expect(state?.redirectTo).toBe('nexusOneWaivers');
      } else {
        expect(state?.component).not.toBe(ComingSoonRoute);
      }
    });
  });

  it('redirects legacy /coming-soon/ paths for embed slugs whose primary URL moved', () => {
    NATIVE_CLASSIC_EMBED_SLUGS.filter(usesEmbeddedHrefPrimary).forEach((slug) => {
      const legacy = router.stateRegistry.get(`${comingSoonStateName(slug)}LegacyPath`);
      expect(legacy?.url).toBe(comingSoonHref(slug));
      expect(legacy?.redirectTo).toBe(comingSoonStateName(slug));
      expect(router.stateRegistry.get(comingSoonStateName(slug))?.url).toBe(embeddedHref(slug));
    });
  });

  it('does not register LegacyPath aliases for stubs or repositories', () => {
    COMING_SOON_MODULE_ORDER.filter((slug) => !usesEmbeddedHrefPrimary(slug)).forEach((slug) => {
      expect(router.stateRegistry.get(`${comingSoonStateName(slug)}LegacyPath`)).toBeFalsy();
    });
  });

  it('keeps repositories Coming Soon entry as alias to nexusOneRepositories (no second /repositories owner)', () => {
    const comingSoon = router.stateRegistry.get(comingSoonStateName('repositories'));
    expect(comingSoon?.url).toBe(comingSoonHref('repositories'));
    expect(comingSoon?.redirectTo).toBe('nexusOneRepositories');
    expect(router.stateRegistry.get('nexusOneRepositories')?.url).toBe('/repositories');
    expect(router.stateRegistry.get(`${comingSoonStateName('repositories')}LegacyPath`)).toBeFalsy();
  });

  it('redirects the Legal Coming Soon entry to nexusOneLegal (LEGAL_VIOLATION triage)', () => {
    // Clean /legal is owned by Nexus One Legal V1 (CLM-43207). Classic ALP dashboard remains at
    // legal.applicationsDashboard for deep links / review workflows.
    const state = router.stateRegistry.get(comingSoonStateName('legal'));
    expect(state?.url).toBe(comingSoonHref('legal'));
    expect(state?.component).toBeUndefined();
    expect(state?.redirectTo).toBe('nexusOneLegal');
    expect(router.stateRegistry.get('nexusOneLegal')?.url).toContain('/legal');
  });

  it('registers Legal tab-switch states so in-page tab clicks resolve in-shell', () => {
    expect(router.stateRegistry.get('legal')?.abstract).toBe(true);

    const applicationsTab = router.stateRegistry.get('legal.applicationsDashboard');
    expect(applicationsTab?.url).toBe('/legal/applicationsDashboard');
    expect(applicationsTab?.data?.activeTab).toBe('applications');
    expect(applicationsTab?.component).toBeDefined();

    const componentsTab = router.stateRegistry.get('legal.componentsDashboard');
    expect(componentsTab?.url).toBe('/legal/componentsDashboard');
    expect(componentsTab?.data?.activeTab).toBe('components');
    expect(componentsTab?.component).toBeDefined();
  });

  it('mounts both Legal tab states with the same component reference', () => {
    // Regression guard: a distinct mountClassicComponent(...) wrapper per state would force
    // React to fully unmount/remount LegalDashboardContainer on every tab switch (different
    // component reference == different type at the same UIView position). Both children of the
    // abstract 'legal' state must share one reference, exactly like Classic's own legal/route.js
    // reuses one LegalDashboardContainer import across its sibling states.
    const applicationsComponent = router.stateRegistry.get('legal.applicationsDashboard')?.component;
    const componentsComponent = router.stateRegistry.get('legal.componentsDashboard')?.component;

    expect(applicationsComponent).toBeDefined();
    expect(componentsComponent).toBe(applicationsComponent);
  });

  it('mounts Application Details and Component Overview in-shell for row clicks', () => {
    // LegalDashboardApplicationRow / LegalDashboardComponentRow stateGo() to these exact names —
    // without a registration here, that stateGo silently fails (routerMiddleware.js's STATE_GO
    // handler has no .catch()). Both containers read params from Redux state.router.currentParams
    // (see their mapStateToProps), not from injected props, so mountClassicComponent works.
    const applicationDetails = router.stateRegistry.get('legal.applicationDetails');
    expect(applicationDetails?.url).toBe('/legal/application/{applicationPublicId}/stage/{stageTypeId}');
    expect(applicationDetails?.component).toBeDefined();

    const componentOverview = router.stateRegistry.get('legal.componentOverview');
    expect(componentOverview?.url).toBe('/legal/component/{hash}');
    expect(componentOverview?.component).toBeDefined();
  });

  it('registers every state in LEGAL_DEEP_LINK_STATES with a matching url and a defined component', () => {
    // Data-driven so this test can't drift from the shared table it's verifying — adding a state
    // to legalDeepLinkStates.ts automatically gets covered here, no manual update needed.
    LEGAL_DEEP_LINK_STATES.forEach((stateDef) => {
      const registered = router.stateRegistry.get(stateDef.name);
      expect(registered?.url).toBe(stateDef.url);
      expect(registered?.component).toBeDefined();
      if (stateDef.abstract) {
        expect(registered?.abstract).toBe(true);
      }
    });
  });

  it('registers legal.dashboard (Classic nav-entry target) so Back-button $state.href resolves', () => {
    const state = router.stateRegistry.get('legal.dashboard');
    expect(state?.url).toBe('/legal/dashboard');
    expect(state?.data?.activeTab).toBe('applications');
    // Shares the same mount as the two tab states — same reasoning as the sibling-tab test above.
    expect(state?.component).toBe(router.stateRegistry.get('legal.applicationsDashboard')?.component);
  });

  it('shares one mounted reference across every component-overview entry-point shape', () => {
    // legal.componentOverview plus every by-identifier / by-owner variant all render the same
    // ComponentLegalOverviewContainer — navigating between them (e.g. a Back link) must not remount.
    const names = [
      'legal.componentOverview',
      'legal.componentOverviewByComponentIdentifier',
      'legal.applicationComponentOverviewByComponentIdentifier',
      'legal.organizationComponentOverview',
      'legal.applicationComponentOverview',
      'legal.applicationStageTypeComponentOverview',
    ];
    const components = names.map((name) => router.stateRegistry.get(name)?.component);
    expect(components[0]).toBeDefined();
    components.forEach((component) => expect(component).toBe(components[0]));
  });

  it('mounts each abstract detail-parent once and leaves its dotted child unwrapped', () => {
    // legal.componentNoticeDetails is abstract with a real Classic component (ComponentNoticeDetails,
    // which itself renders a nested <UIView />) — the parent gets wrapped in ClassicComponentMount;
    // the child (.noticeDetails) must render with its own bare component, or the page double-wraps.
    const parentEntry = LEGAL_DEEP_LINK_STATES.find((s) => s.name === 'legal.componentNoticeDetails')!;
    const childEntry = LEGAL_DEEP_LINK_STATES.find((s) => s.name === 'legal.componentNoticeDetails.noticeDetails')!;

    const registeredParent = router.stateRegistry.get('legal.componentNoticeDetails');
    const registeredChild = router.stateRegistry.get('legal.componentNoticeDetails.noticeDetails');

    expect(registeredParent?.component).not.toBe(parentEntry.component); // wrapped, not the raw Classic component
    expect(registeredChild?.component).toBe(childEntry.component); // bare — renders in the parent's own UIView

    // The three notice-details abstract parents all render ComponentNoticeDetails — they must
    // share the same wrapped reference too.
    const otherNoticeParent = router.stateRegistry.get('legal.noticeFilesByComponentIdentifier');
    expect(otherNoticeParent?.component).toBe(registeredParent?.component);
  });

  it('redirects the Orgs and Policies Coming Soon entry to the root org summary (CLM-42161)', () => {
    // organizationId is a required param, so the entry redirects to an object target rather than a
    // bare state name like Legal's. 'ROOT_ORGANIZATION_ID' mirrors the classicHref in comingSoonModules.ts.
    const state = router.stateRegistry.get(comingSoonStateName('orgs-and-policies'));
    expect(state?.component).toBeUndefined();
    expect(state?.redirectTo).toEqual({
      state: 'management.view.organization',
      params: { organizationId: 'ROOT_ORGANIZATION_ID' },
    });
  });

  it('registers every state in ORGS_AND_POLICIES_STATES with a matching url and a defined component', () => {
    // Data-driven drift guard, mirroring the LEGAL_DEEP_LINK_STATES test above - adding a management
    // state to orgsAndPoliciesStates.ts is automatically covered here.
    ORGS_AND_POLICIES_STATES.forEach((stateDef) => {
      const registered = router.stateRegistry.get(stateDef.name);
      expect(registered).toBeDefined();
      if (stateDef.url !== undefined) {
        expect(registered?.url).toBe(stateDef.url);
      }
      expect(registered?.component).toBeDefined();
      if (stateDef.abstract) {
        expect(registered?.abstract).toBe(true);
      }
    });
  });

  it('wraps only the chrome-carrying management states, leaving deeper states unwrapped', () => {
    // management.view / management.tree / management.edit.{ownerType} carry their own sidebar +
    // breadcrumb + nested <UIView /> and get wrapped in ClassicComponentMount; the pages that render
    // inside those UIViews (org summary, policy editor, ...) register with their bare component.
    ORGS_AND_POLICIES_STATES.forEach((stateDef) => {
      const registered = router.stateRegistry.get(stateDef.name);
      if (ORGS_AND_POLICIES_CHROME_COMPONENTS.has(stateDef.component)) {
        expect(registered?.component).not.toBe(stateDef.component); // wrapped
      } else {
        expect(registered?.component).toBe(stateDef.component); // bare
      }
    });

    // Anchor against concrete components so an empty/incorrect ORGS_AND_POLICIES_CHROME_COMPONENTS
    // set (which would make the loop above pass vacuously) still fails: management.tree must be
    // wrapped, and a deep editor state must stay bare.
    const treeComponent = router.stateRegistry.get('management.tree')?.component;
    expect(treeComponent).toBeDefined();
    expect(treeComponent).not.toBe(OwnersTreePage);
    expect(router.stateRegistry.get('management.edit.organization.policy')?.component).toBe(PolicyEditor);
  });

  it('shares one mounted reference per chrome component across owner types', () => {
    // Every management.edit.{ownerType} renders OwnerManagerEditWrapper; a distinct wrapper per state
    // would remount the whole edit shell when switching owner types. They must share one reference.
    const editWrapperComponents = ORGS_AND_POLICIES_STATES.filter((s) => /^management\.edit\.[^.]+$/.test(s.name)).map(
      (s) => router.stateRegistry.get(s.name)?.component
    );

    expect(editWrapperComponents.length).toBeGreaterThan(1);
    editWrapperComponents.forEach((component) => {
      expect(component).toBeDefined();
      expect(component).toBe(editWrapperComponents[0]);
    });
  });

  it('registers the InnerSource / Artifactory repository config states the owner-summary Edit buttons target', () => {
    // The tiles stateGo to these by name; without registration the Edit buttons do nothing (CLM-42161).
    ['repositoryBaseConfigurations', 'artifactoryRepositoryBaseConfigurations'].forEach((abstractName) => {
      expect(router.stateRegistry.get(abstractName)?.component).toBeDefined();

      const orgState = router.stateRegistry.get(`${abstractName}.organization`);
      expect(orgState?.url).toBe(`/organization/{organizationId}/${abstractName}`);

      const appState = router.stateRegistry.get(`${abstractName}.application`);
      expect(appState?.url).toBe(`/application/{applicationId}/${abstractName}`);
    });
  });

  it('registers the embedded application report state (CLM-41538)', () => {
    expect(router.stateRegistry.get(NEXUS_ONE_APPLICATION_REPORT_STATE)?.url).toBe(
      '/applications/{publicId}/report/{scanId}?componentHash&tabId',
    );
  });

  it('registers the embedded violation detail state (CLM-42256)', () => {
    expect(router.stateRegistry.get(NEXUS_ONE_VIOLATION_DETAIL_STATE)?.url).toBe(
      '/violations/{id}?type&sidebarReference&sidebarId&page',
    );
  });

  it('registers reporting deep-link states so embedded card links resolve in-shell', () => {
    const react2shell = router.stateRegistry.get('react2ShellReport');
    expect(react2shell?.url).toBe('/reports/react2shell');
    expect(react2shell?.component).toBeDefined();

    expect(router.stateRegistry.get('enterpriseReportingDashboardGroup')?.url).toBe(
      '/enterpriseReportingDashboard/{groupId}/{id}',
    );
    expect(router.stateRegistry.get('enterpriseReportingDashboard')?.url).toBe('/enterpriseReportingDashboard/{id}');

    expect(router.stateRegistry.get('enterpriseReporting')?.redirectTo).toBe(
      comingSoonStateName('reports'),
    );

    const twoSegmentMatch = router.urlService.match({ path: '/enterpriseReportingDashboard/security/sbom-scorecard' });
    expect(twoSegmentMatch?.rule?.state?.name).toBe('enterpriseReportingDashboardGroup');
  });

  it('registers Success Metrics detail and Classic alias states', () => {
    expect(router.stateRegistry.get('labs')?.abstract).toBe(true);
    expect(router.stateRegistry.get('labs.successMetricsReport')?.url).toBe(
      '/success-metrics/{successMetricsReportId}',
    );
    const legacyReport = router.stateRegistry.get('labs.successMetricsReportLegacyPath');
    expect(legacyReport?.url).toBe('/coming-soon/success-metrics/{successMetricsReportId}');
    expect(typeof legacyReport?.redirectTo).toBe('function');
    expect(router.stateRegistry.get('labs.successMetrics')?.redirectTo).toBe(comingSoonStateName('success-metrics'));
    expect(router.stateRegistry.get('dashboard.overview.violations')?.redirectTo).toBe('nexusOneDashboard.violations');
    // /violations is now the Martha V1 Violations card list (CLM-42257), not a redirect to the
    // dashboard tab. It is a sibling of the embedded detail state at /violations/{id} (CLM-42256).
    // The optional query params persist search + filters + page in the hash (CLM-42260).
    const violations = router.stateRegistry.get(NEXUS_ONE_VIOLATIONS_STATE_NAME);
    expect(violations?.url).toBe(NEXUS_ONE_VIOLATIONS_URL);
    expect(violations?.redirectTo).toBeUndefined();
    expect(violations?.component).toBeDefined();
    // /components is the Martha V1 Components list (CLM-42214 / CLM-42760), not a Coming Soon stub.
    const components = router.stateRegistry.get('nexusOneComponents');
    expect(components?.redirectTo).toBeUndefined();
    expect(components?.component).toBeDefined();
  });

  it('registers the dedicated Hosted Repos native route the LeftNav links to', () => {
    const state = router.stateRegistry.get('nexusOneRepositories');
    expect(state).toBeDefined();
    expect(state?.url).toBe('/repositories');
    // Native embed, not a Coming Soon stub.
    expect(state?.component).toBeDefined();
    expect(state?.component).not.toBe(ComingSoonRoute);
  });

  it('registers the Hosted Repos detail (repositories-in-a-manager) native route', () => {
    const state = router.stateRegistry.get('nexusOneRepositoriesDetail');
    expect(state).toBeDefined();
    expect(state?.url).toBe('/repositories/{repositoryManagerId}');
    expect(state?.component).toBeDefined();
    expect(state?.component).not.toBe(ComingSoonRoute);
  });

  it('registers the Hosted Repos components (components-in-a-repo) native route', () => {
    const state = router.stateRegistry.get('nexusOneRepositoriesComponents');
    expect(state).toBeDefined();
    expect(state?.url).toBe('/repositories/{repositoryManagerId}/{repositoryId}/components?{repositoryPublicId}');
    expect(state?.component).toBeDefined();
    expect(state?.component).not.toBe(ComingSoonRoute);
  });

  // Guards the Classic-embed admin route's dirty-guard wiring: a typo in the
  // `data.isDirty` path array would silently disable the unsaved-changes
  // prompt, and the only other coverage is heavyweight Playwright suites.
  describe.each([
    [
      'successMetricsConfiguration',
      '/successMetricsConfiguration',
      ['successMetricsConfiguration', 'viewState', 'isDirty'],
    ],
    ['baseUrlConfiguration', '/baseUrl', ['baseUrlConfiguration', 'isDirty']],
    [
      'systemNoticeConfiguration',
      '/systemNoticeConfiguration',
      ['systemNoticeConfiguration', 'viewState', 'isDirty'],
    ],
    ['saml', '/saml', ['samlConfiguration', 'isDirty']],
    ['userTokensConfiguration', '/userTokensConfiguration', ['userTokensConfiguration', 'isDirty']],
    [
      'advancedSearchConfig',
      '/advancedSearchConfig',
      ['advancedSearchConfig', 'viewState', 'isDirty'],
    ],
  ])('%s Classic-embed admin route', (stateName, url, isDirtyPath) => {
    const state = () => router.stateRegistry.get(stateName);
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it(`is registered at ${url}`, () => {
      expect(state()?.url).toBe(url);
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('wires the dirty guard through the exact state-path array the router selector reads', () => {
      expect(state()?.data?.isDirty).toEqual(isDirtyPath);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      const rootState = rootReducer(undefined, { type: '@@INIT' });
      const path = state()?.data?.isDirty as string[];
      // Handle both 2-level [slice, field] and 3-level [slice, viewState, field] paths
      let value: unknown;
      if (path.length === 2) {
        value = (rootState as Record<string, Record<string, unknown>>)[path[0]]?.[path[1]];
      } else if (path.length === 3) {
        value = (rootState as Record<string, Record<string, Record<string, unknown>>>)[path[0]]?.[path[1]]?.[path[2]];
      }
      expect(typeof value).toBe('boolean');
    });
  });

  describe('waivedComponentUpgradesConfiguration Classic-embed admin route (CLM-42468)', () => {
    const state = () => router.stateRegistry.get('waivedComponentUpgradesConfiguration');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /waivedComponentUpgradesConfiguration', () => {
      expect(state()?.url).toBe('/waivedComponentUpgradesConfiguration');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('wires the dirty guard through the exact state-path array the router selector reads', () => {
      expect(state()?.data?.isDirty).toEqual(['waivedComponentUpgradesConfiguration', 'isDirty']);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      const rootState = rootReducer(undefined, { type: '@@INIT' });
      const [slice, field] = state()?.data?.isDirty ?? [];
      expect(typeof (rootState as Record<string, Record<string, unknown>>)[slice]?.[field]).toBe('boolean');
    });
  });

  // State name is `ldap-list` (not `ldapServers`) so it matches the shell nav
  // and Classic — gear menu → hrefFromStateName('ldap-list') resolves here.
  describe('ldap-list Classic-embed admin route', () => {
    const state = () => router.stateRegistry.get('ldap-list');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /ldap-servers', () => {
      expect(state()?.url).toBe('/ldap-servers');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('wires the dirty guard through the exact state-path array the router selector reads', () => {
      expect(state()?.data?.isDirty).toEqual(['ldapList', 'isDirty']);
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      expectIsDirtyPathResolvesToBoolean(state()?.data?.isDirty);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  describe('productlicense Classic-embed admin route (CLM-42466)', () => {
    const state = () => router.stateRegistry.get('productlicense');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /productlicense', () => {
      expect(state()?.url).toBe('/productlicense');
    });

    it('mounts ProductLicenseContainer via mountClassicComponent', () => {
      expect(state()?.component).toBeDefined();
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // Create / edit sub-pages the LDAP list navigates to via stateGo(...).
  describe.each<[string, string]>([
    ['create-ldap', '/ldap/create'],
    ['edit-ldap-connection', '/ldap/edit/{ldapId}'],
    ['edit-ldap-usermapping', '/ldap/edit/{ldapId}/userMapping'],
  ])('%s Classic-embed sub-route', (stateName, expectedUrl) => {
    const state = () => router.stateRegistry.get(stateName);
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it(`is registered at ${expectedUrl}`, () => {
      expect(state()?.url).toBe(expectedUrl);
    });

    it('carries the same ldapConfig dirty path so the shell dirty guard fires on nav', () => {
      expect(state()?.data?.isDirty).toEqual(['ldapConfig', 'isDirty']);
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      expectIsDirtyPathResolvesToBoolean(state()?.data?.isDirty);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // CLM-42466: Getting Started page — registered so ProductLicense.jsx's
  // first-install redirect (#/gettingStarted) lands here instead of the NOUX
  // dashboard fallback.
  describe('gettingStarted Classic-embed admin route (CLM-42466)', () => {
    const state = () => router.stateRegistry.get('gettingStarted');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /gettingStarted', () => {
      expect(state()?.url).toBe('/gettingStarted');
    });

    it('mounts GettingStartedContainer via mountClassicComponent', () => {
      expect(state()?.component).toBeDefined();
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  it('/ldap/edit/{ldapId}/userMapping resolves to edit-ldap-usermapping, not edit-ldap-connection', () => {
    // The `{ldapId}` param is single-segment ([^/]+) so it cannot capture the `.../userMapping`
    // suffix — that leaves only the longer /ldap/edit/{ldapId}/userMapping pattern as a match.
    const match = router.urlService.match({ path: '/ldap/edit/some-ldap-id/userMapping' });
    expect(match?.rule?.state?.name).toBe('edit-ldap-usermapping');
  });

  describe('users Classic-embed admin route (CLM-42465)', () => {
    const state = () => router.stateRegistry.get('users');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /users', () => {
      expect(state()?.url).toBe('/users');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // Activity tab is a child of `users`. Classic renders the parent's
  // UserManagementContainer; the container selects Activity by checking
  // router.currentState.name === 'users.activity' (not data.activeTab).
  describe('users.activity child state', () => {
    const state = () => router.stateRegistry.get('users.activity');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered as a child of users at /activity', () => {
      expect(state()?.url).toBe('/activity');
    });

    it('gates access via an async redirectTo function', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);
      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);
      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // Row clicks in the Activity view dispatch stateGo('userActivityDetails', {username}).
  describe('userActivityDetails Classic-embed route', () => {
    const state = () => router.stateRegistry.get('userActivityDetails');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /users/activity/{username}', () => {
      expect(state()?.url).toBe('/users/activity/{username}');
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM + ACCESS_AUDIT_LOG', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM', 'ACCESS_AUDIT_LOG']);
    });

    it('redirects to nexusOneDashboard.violations when authorization fails', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // Create / edit user pages the Users list navigates to via stateGo(...).
  describe.each<[string, string]>([
    ['createUser', '/users/_new_'],
    ['editUser', '/users/{userId}'],
  ])('%s Classic-embed sub-route', (stateName, expectedUrl) => {
    const state = () => router.stateRegistry.get(stateName);
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it(`is registered at ${expectedUrl}`, () => {
      expect(state()?.url).toBe(expectedUrl);
    });

    it('carries the userConfiguration dirty path so the shell dirty guard fires on nav', () => {
      expect(state()?.data?.isDirty).toEqual(['userConfiguration', 'isDirty']);
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      expectIsDirtyPathResolvesToBoolean(state()?.data?.isDirty);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when authorization fails', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // Standalone User Activity — shown when user-management is disabled but
  // activity tracking is on. UserManagementContainer renders only
  // UserActivityOverviewContainer when showActivityOnly is true.
  describe('userActivity Classic-embed route', () => {
    const state = () => router.stateRegistry.get('userActivity');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /user-activity', () => {
      expect(state()?.url).toBe('/user-activity');
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  it('/users/activity/{user} resolves to userActivityDetails, not editUser (static segment wins)', () => {
    const match = router.urlService.match({ path: '/users/activity/some-user' });
    expect(match?.rule?.state?.name).toBe('userActivityDetails');
  });

  it('/users/_new_ resolves to createUser, not editUser (static segment wins)', () => {
    const match = router.urlService.match({ path: '/users/_new_' });
    expect(match?.rule?.state?.name).toBe('createUser');
  });


  // CLM-42464: Administrators list page (read-only, no form, no dirty guard)
  describe('administrators Classic-embed admin route', () => {
    const state = () => router.stateRegistry.get('administrators');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /administrators', () => {
      expect(state()?.url).toBe('/administrators');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('omits isDirty data property (list page with no form)', () => {
      expect(state()?.data?.isDirty).toBeUndefined();
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // CLM-42464: Administrators edit page (has isDirty dirty-guard wiring)
  describe('administratorsEdit Classic-embed admin route', () => {
    const state = () => router.stateRegistry.get('administratorsEdit');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /administrators/{roleId}', () => {
      expect(state()?.url).toBe('/administrators/{roleId}');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('carries the isDirty data path so the shell dirty guard fires on nav', () => {
      expect(state()?.data?.isDirty).toEqual(['administratorsConfig', 'isDirty']);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      expectIsDirtyPathResolvesToBoolean(state()?.data?.isDirty);
    });
  });

  describe('mailConfig Classic-embed admin route (CLM-42875)', () => {
    const state = () => router.stateRegistry.get('mailConfig');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => {
      (isAuthorized as jest.Mock).mockReset();
      (selectIsEmailConfigurationEnabled as jest.Mock).mockReturnValue(true);
    });

    it('is registered at /mailConfig', () => {
      expect(state()?.url).toBe('/mailConfig');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('carries the isDirty data path so the shell dirty guard fires on nav', () => {
      expect(state()?.data?.isDirty).toEqual(['mailConfig', 'isDirty']);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM and email is enabled', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });

    it('redirects to nexusOneDashboard.violations when the email feature is disabled', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);
      (selectIsEmailConfigurationEnabled as jest.Mock).mockReturnValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      const rootState = rootReducer(undefined, { type: '@@INIT' });
      const [slice, field] = state()?.data?.isDirty ?? [];
      expect(typeof (rootState as Record<string, Record<string, unknown>>)[slice]?.[field]).toBe('boolean');
    });
  });

  // CLM-42196: Roles list page (read-only, no form, no dirty guard)
  describe('rolesList Classic-embed admin route', () => {
    const state = () => router.stateRegistry.get('rolesList');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /roles', () => {
      expect(state()?.url).toBe('/roles');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('omits isDirty data property (list page with no form)', () => {
      expect(state()?.data?.isDirty).toBeUndefined();
    });

    it('resolves to undefined when the user has VIEW_ROLES', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['VIEW_ROLES']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks VIEW_ROLES', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // CLM-42196: Create / edit role pages the Roles list navigates to via stateGo(...).
  describe.each<[string, string]>([
    ['addRole', '/roles/_new_'],
    ['editRole', '/roles/{roleId}'],
  ])('%s Classic-embed sub-route', (stateName, expectedUrl) => {
    const state = () => router.stateRegistry.get(stateName);
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it(`is registered at ${expectedUrl}`, () => {
      expect(state()?.url).toBe(expectedUrl);
    });

    it('carries the roleEditor dirty path so the shell dirty guard fires on nav', () => {
      expect(state()?.data?.isDirty).toEqual(['roleEditor', 'isDirty']);
    });

    it('resolves to undefined when the user has VIEW_ROLES', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['VIEW_ROLES']);
    });

    it('redirects to nexusOneDashboard.violations when authorization fails', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      const rootState = rootReducer(undefined, { type: '@@INIT' });
      const [slice, field] = state()?.data?.isDirty ?? [];
      expect(typeof (rootState as Record<string, Record<string, unknown>>)[slice]?.[field]).toBe('boolean');
    });
  });

  it('/roles/_new_ resolves to addRole, not editRole (static segment wins)', () => {
    const match = router.urlService.match({ path: '/roles/_new_' });
    expect(match?.rule?.state?.name).toBe('addRole');
  });

  // CLM-42961: Webhooks list page (read-only, no form, no dirty guard)
  describe('listWebhooks Classic-embed admin route', () => {
    const state = () => router.stateRegistry.get('listWebhooks');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /webhooks/list', () => {
      expect(state()?.url).toBe('/webhooks/list');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('omits isDirty data property (list page with no form)', () => {
      expect(state()?.data?.isDirty).toBeUndefined();
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });
  });

  // CLM-42961: Create / edit webhook pages the Webhooks list navigates to via stateGo(...).
  describe.each<[string, string]>([
    ['addWebhook', '/webhooks/create'],
    ['editWebhook', '/webhooks/{webhookId}'],
  ])('%s Classic-embed sub-route', (stateName, expectedUrl) => {
    const state = () => router.stateRegistry.get(stateName);
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it(`is registered at ${expectedUrl}`, () => {
      expect(state()?.url).toBe(expectedUrl);
    });

    it('carries the webhooks dirty path so the shell dirty guard fires on nav', () => {
      expect(state()?.data?.isDirty).toEqual(['webhooks', 'isDirty']);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when authorization fails', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      const rootState = rootReducer(undefined, { type: '@@INIT' });
      const [slice, field] = state()?.data?.isDirty ?? [];
      expect(typeof (rootState as Record<string, Record<string, unknown>>)[slice]?.[field]).toBe('boolean');
    });
  });

  describe('proxyConfig Classic-embed admin route (CLM-42876)', () => {
    const state = () => router.stateRegistry.get('proxyConfig');
    const redirectTo = () => state()?.redirectTo as () => Promise<string | undefined>;

    beforeEach(() => (isAuthorized as jest.Mock).mockReset());

    it('is registered at /proxyConfig', () => {
      expect(state()?.url).toBe('/proxyConfig');
    });

    it('gates access via an async redirectTo function (not a static state string)', () => {
      expect(typeof state()?.redirectTo).toBe('function');
    });

    it('is wrapped in mountClassicComponent (shell offsets applied)', () => {
      // mountClassicComponent returns a named function `MountedClassicComponent`
      // (see nexus-one/ClassicComponentMount.tsx). Asserting the wrapper is in
      // place guards against a future refactor that drops it — the inline
      // comment at routes.tsx explains that skipping the wrapper causes .nx-page
      // content to underrun the LeftNav.
      const component = state()?.component as { name?: string } | undefined;
      expect(component).toBeDefined();
      expect(component?.name).toBe('MountedClassicComponent');
    });

    it('carries the isDirty data path so the shell dirty guard fires on nav', () => {
      expect(state()?.data?.isDirty).toEqual(['proxyConfig', 'isDirty']);
    });

    it('resolves to undefined when the user has CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(true);

      await expect(redirectTo()()).resolves.toBeUndefined();
      expect(isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM']);
    });

    it('redirects to nexusOneDashboard.violations when the user lacks CONFIGURE_SYSTEM', async () => {
      (isAuthorized as jest.Mock).mockResolvedValueOnce(false);

      await expect(redirectTo()()).resolves.toBe('nexusOneDashboard.violations');
    });

    it('isDirty path resolves to a boolean in rootReducer initial state', () => {
      const isDirty = state()?.data?.isDirty as ReadonlyArray<string> | undefined;
      // Fail early if the guard entry is missing — without this the destructure
      // below silently produces two undefineds and typeof undefined !== 'boolean'
      // would still fail, but with a confusing "expected boolean, got undefined"
      // instead of "isDirty is missing".
      expect(isDirty).toEqual(['proxyConfig', 'isDirty']);
      const rootState = rootReducer(undefined, { type: '@@INIT' });
      const [slice, field] = isDirty as ReadonlyArray<string>;
      expect(typeof (rootState as Record<string, Record<string, unknown>>)[slice]?.[field]).toBe('boolean');
    });
  });
});
