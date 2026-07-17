/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
jest.mock('MainRoot/util/permissionService', () => ({
  isAuthorized: jest.fn(),
}));

import router from 'MainRoot/router/routerInstance';
import {
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
  comingSoonStateName,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import { isNativeClassicEmbedSlug } from 'MainRoot/nexus-one/nativeClassicEmbedSlugs';
import { LEGAL_DEEP_LINK_STATES } from 'MainRoot/legal/legalDeepLinkStates';
import {
  ORGS_AND_POLICIES_STATES,
  ORGS_AND_POLICIES_CHROME_COMPONENTS,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStates';
import OwnersTreePage from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnersTreePage';
import PolicyEditor from 'MainRoot/OrgsAndPolicies/policyEditor/PolicyEditor';
import { NEXUS_ONE_APPLICATION_REPORT_STATE } from 'MainRoot/nexus-one/nexusOneApplicationReportStates';
import { isAuthorized } from 'MainRoot/util/permissionService';
import { NEXUS_ONE_VIOLATION_DETAIL_STATE } from 'MainRoot/nexus-one/nexusOneViolationDetailStates';
import {
  NEXUS_ONE_VIOLATIONS_STATE_NAME,
  NEXUS_ONE_VIOLATIONS_URL,
} from 'MainRoot/nosc/violations/violationsRoute';
import 'MainRoot/nexus-one/routes';

describe('nexusOneClassicEmbedRoutes', () => {
  it('registers native Classic mounts / redirects for embedded slugs and Coming Soon stubs for the rest', () => {
    COMING_SOON_MODULE_ORDER.forEach((slug) => {
      const state = router.stateRegistry.get(comingSoonStateName(slug));
      expect(state).toBeDefined();
      expect(state?.url).toBe(comingSoonHref(slug));

      if (!isNativeClassicEmbedSlug(slug)) {
        expect(state?.component).toBe(ComingSoonRoute);
      } else if (slug === 'legal') {
        // Legal's entry redirects rather than mounting a component of its own — see the
        // dedicated tests below for why.
        expect(state?.component).toBeUndefined();
        expect(state?.redirectTo).toBe('legal.applicationsDashboard');
      } else if (slug === 'orgs-and-policies') {
        // Orgs and Policies' entry redirects into the embedded management tree - see below.
        expect(state?.component).toBeUndefined();
        expect(state?.redirectTo).toEqual({
          state: 'management.view.organization',
          params: { organizationId: 'ROOT_ORGANIZATION_ID' },
        });
      } else {
        expect(state?.component).not.toBe(ComingSoonRoute);
      }
    });
  });

  it('redirects the Legal Coming Soon entry straight to the Applications tab', () => {
    // The entry never mounts LegalDashboardMount itself: it's a flat top-level state, one UIView
    // level shallower than legal.applicationsDashboard (a child of the abstract 'legal' state).
    // Mounting it directly there would change the root UIView's resolved component type from
    // LegalDashboardMount to UIView on the very first tab transition, forcing an unmount/remount
    // regardless of the shared reference asserted below. Redirecting sidesteps that entirely —
    // the user always lands directly on a legal.*Dashboard child.
    const state = router.stateRegistry.get(comingSoonStateName('legal'));
    expect(state?.component).toBeUndefined();
    expect(state?.redirectTo).toBe('legal.applicationsDashboard');
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
      '/coming-soon/success-metrics/{successMetricsReportId}'
    );
    expect(router.stateRegistry.get('labs.successMetrics')?.redirectTo).toBe(comingSoonStateName('success-metrics'));
    expect(router.stateRegistry.get('dashboard.overview.violations')?.redirectTo).toBe('nexusOneDashboard.violations');
    // /violations is now the Martha V1 Violations card list (CLM-42257), not a redirect to the
    // dashboard tab. It is a sibling of the embedded detail state at /violations/{id} (CLM-42256).
    // The optional query params persist search + filters + page in the hash (CLM-42260).
    const violations = router.stateRegistry.get(NEXUS_ONE_VIOLATIONS_STATE_NAME);
    expect(violations?.url).toBe(NEXUS_ONE_VIOLATIONS_URL);
    expect(violations?.redirectTo).toBeUndefined();
    expect(violations?.component).toBeDefined();
    expect(router.stateRegistry.get('nexusOneComponents')?.redirectTo).toBe(comingSoonStateName('components'));
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
  });
});
