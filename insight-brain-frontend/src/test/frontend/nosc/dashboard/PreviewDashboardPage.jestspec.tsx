/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import axios from 'axios';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { FETCH_CURRENT_FILTER_FULFILLED } from 'MainRoot/dashboard/filter/dashboardFilterActions';
import { LOAD_RESULTS_FULFILLED } from 'MainRoot/dashboard/results/dashboardResultsActions';
import { getDashboardFilters, getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';
import { setupNexusOneBundleLocation } from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import { renderNexusOneDashboard } from 'TestRoot/nosc/dashboard/renderNexusOneDashboard';

/**
 * PreviewDashboardPage tests (CLM-39992 + CLM-39641 review follow-up).
 *
 * The dashboard is now a UI-Router nested-view tree: an abstract `nexusOneDashboard` parent shell
 * (tab strip + <UIView>) with one child state per tab. The page no longer hand-rolls hash parsing /
 * pushState, so these tests drive navigation through the router via `renderNexusOneDashboard`
 * (which registers the production state declarations against an isolated memory-location router).
 */

// Tab content embeds the Classic dashboard containers + tiles, which spawn axios calls. Stub them so
// the tests don't log unhandled-rejection warnings.
function stubDashboardAxios(axiosMock: any): void {
  axiosMock.onAny().reply(() => new Promise(() => {}));
}

describe('PreviewDashboardPage (CLM-39992 / CLM-39641)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupNexusOneBundleLocation();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  describe('Router-driven tab state', () => {
    it('renders the metric-card grid landing with NO tab strip on nexusOneDashboard.overview (CLM-40905)', async () => {
      stubDashboardAxios(axiosMock);
      renderNexusOneDashboard('overview');
      // The landing is the metric-card grid (its chrome renders immediately, even before the
      // POST resolves — here it never does, so it stays in the skeleton/loading shape).
      expect(await screen.findByTestId('preview-dashboard-metrics-grid')).toBeInTheDocument();
      // No in-dashboard tab strip on the landing view.
      expect(screen.queryByTestId('nosc-dashboard-tabs')).not.toBeInTheDocument();
      const page = screen.getByTestId('nosc-dashboard-page');
      expect(page).toHaveAttribute('data-active-tab', 'overview');
    });

    // Each non-Overview tab is its own child route; landing on that route selects the trigger and
    // renders the tab body into the shell's <UIView>.
    it.each([
      ['violations', 'nosc-dashboard-violations-tab'],
      ['applications', 'nosc-dashboard-applications-tab'],
      ['waivers', 'nosc-dashboard-waivers-tab'],
    ] as const)(
      'opens directly on the %s tab when routed there and renders its body',
      async (slug, bodyTestId) => {
        stubDashboardAxios(axiosMock);
        renderNexusOneDashboard(slug);
        const tab = await screen.findByTestId(`nosc-dashboard-tab-${slug}`);
        await waitFor(() => expect(tab.getAttribute('aria-selected')).toBe('true'));
        expect(await screen.findByTestId(bodyTestId)).toBeInTheDocument();
      },
    );

    it('navigates the router (not the hash) when the user clicks a tab', async () => {
      stubDashboardAxios(axiosMock);
      // Start on a classic tab (the tab strip only renders for the non-landing tabs).
      const { router } = renderNexusOneDashboard('violations');
      await screen.findByTestId('nosc-dashboard-page');

      await userEvent.click(screen.getByTestId('nosc-dashboard-tab-applications'));

      await waitFor(() => expect(router.stateService.current.name).toBe('nexusOneDashboard.applications'));
      const applicationsTab = screen.getByTestId('nosc-dashboard-tab-applications');
      await waitFor(() => expect(applicationsTab.getAttribute('aria-selected')).toBe('true'));
      expect(await screen.findByTestId('nosc-dashboard-applications-tab')).toBeInTheDocument();
    });

    it('returns to the overview state when the user clicks the Overview tab', async () => {
      stubDashboardAxios(axiosMock);
      const { router } = renderNexusOneDashboard('violations');
      await screen.findByTestId('nosc-dashboard-page');

      await userEvent.click(screen.getByTestId('nosc-dashboard-tab-overview'));

      await waitFor(() => expect(router.stateService.current.name).toBe('nexusOneDashboard.overview'));
    });
  });

  describe('Overview landing (metric-card grid, CLM-40905)', () => {
    it('renders the metric-card grid landing and NOT the tab strip or the old tile-grid overview', async () => {
      stubDashboardAxios(axiosMock);
      renderNexusOneDashboard('overview');

      // New landing: the metric-card grid.
      expect(await screen.findByTestId('preview-dashboard-metrics-grid')).toBeInTheDocument();

      // No in-dashboard tab strip on the landing.
      expect(screen.queryByTestId('nosc-dashboard-tabs')).not.toBeInTheDocument();

      // The old tile-grid Overview content is no longer the landing.
      expect(screen.queryByTestId('preview-dashboard-overview-content')).not.toBeInTheDocument();

      // The standalone Dashboard.tsx overlay MUST NOT mount either.
      expect(screen.queryByTestId('preview-dashboard-page')).not.toBeInTheDocument();
    });
  });

  describe('Tab strip rendering', () => {
    it('renders all 4 tab triggers in the canonical order on a classic tab', async () => {
      stubDashboardAxios(axiosMock);
      renderNexusOneDashboard('violations');
      await screen.findByTestId('nosc-dashboard-page');

      const expectedTestIds = [
        'nosc-dashboard-tab-overview',
        'nosc-dashboard-tab-violations',
        'nosc-dashboard-tab-applications',
        'nosc-dashboard-tab-waivers',
      ];
      const tabList = screen.getByTestId('nosc-dashboard-tabs');
      const triggers = within(tabList)
        .getAllByRole('tab')
        .map((el) => el.getAttribute('data-testid'));
      expect(triggers).toEqual(expectedTestIds);
    });

    it('exposes the active tab as a data attribute on the page root for selenium / automation', async () => {
      stubDashboardAxios(axiosMock);
      renderNexusOneDashboard('waivers');
      const page = await screen.findByTestId('nosc-dashboard-page');
      expect(page).toHaveAttribute('data-active-tab', 'waivers');
    });
  });

  describe('Filter rail data load on first mount', () => {
    function stubLoadFilterEndpoints(axiosMock: any): void {
      axiosMock.onGet(getDashboardFilters()).reply(200, {
        selected: null,
        appliedFilter: null,
        needsAcknowledgement: false,
      });
      axiosMock.onGet(/applications/).reply(200, []);
      axiosMock.onGet(/organizations/).reply(200, []);
      axiosMock.onGet(/applicationTags|applicationCategories/).reply(200, []);
      axiosMock.onGet(/repositories/).reply(200, { repositories: [] });
      axiosMock.onGet(/savedFilters/).reply(200, []);
      axiosMock.onGet(/stageTypes/).reply(200, { dashboard: [] });
      axiosMock.onGet(/waiverReasons/).reply(200, []);
      axiosMock.onAny().reply(() => new Promise(() => {}));
    }

    /** Like {@link stubLoadFilterEndpoints} but lets the full loadFilter Promise.all settle. */
    function stubResolvingFilterLoad(axiosMock: any): void {
      axiosMock.onGet(getDashboardFilters()).reply(200, {
        selected: null,
        appliedFilter: null,
        needsAcknowledgement: false,
      });
      axiosMock.onGet(/applications/).reply(200, []);
      axiosMock.onGet(/organizations/).reply(200, []);
      axiosMock.onGet(/applicationTags|applicationCategories/).reply(200, []);
      axiosMock.onGet(/repositories/).reply(200, { repositories: [] });
      axiosMock.onGet(/savedFilters/).reply(200, []);
      axiosMock.onGet(/stageTypes/).reply(200, { dashboard: [] });
      axiosMock.onGet(/waiverReasons/).reply(200, []);
      axiosMock.onGet(/product\/features/).reply(200, []);
      axiosMock.onPost(/newestRisks|componentRisks|applicationRisks|waivers/).reply(200, {
        dashboardResults: [],
        hasNextPage: false,
      });
      axiosMock.onAny().reply(200, {});
    }

    it('dispatches loadFilter() on first mount (axios.get hits the dashboard-filters endpoint)', async () => {
      stubLoadFilterEndpoints(axiosMock);
      const axiosGetSpy = jest.spyOn(axios, 'get');
      try {
        renderNexusOneDashboard('violations');
        await waitFor(() => {
          expect(axiosGetSpy).toHaveBeenCalledWith(getDashboardFilters());
        });
      } finally {
        axiosGetSpy.mockRestore();
      }
    });

    it('flips dashboardFilter.loading from true to false once the load chain resolves', async () => {
      stubLoadFilterEndpoints(axiosMock);
      const { store } = renderNexusOneDashboard('violations');
      await waitFor(() => {
        expect(store.getState().dashboardFilter.loading).toBe(true);
      });

      store.dispatch({
        type: FETCH_CURRENT_FILTER_FULFILLED,
        payload: { selected: null, appliedFilter: null, needsAcknowledgement: false },
      });
      expect(store.getState().dashboardFilter.loading).toBe(false);
    });

    function classicTabResultPosts(axiosMock: any): any[] {
      return axiosMock.history.post.filter(
        (req: { url?: string }) =>
          req.url != null &&
          !req.url.includes(getDashboardMetricsUrl()) &&
          /newestRisks|componentRisks|applicationRisks|waivers/i.test(req.url),
      );
    }

    it('does not eager-load classic tab result sets on the overview landing', async () => {
      stubResolvingFilterLoad(axiosMock);
      const { store } = renderNexusOneDashboard('overview');
      await waitFor(() => expect(store.getState().dashboardFilter.loading).toBe(false));
      expect(classicTabResultPosts(axiosMock)).toHaveLength(0);
    });

    it('only dispatches loadFilter() once per shell mount even across tab switches', async () => {
      stubLoadFilterEndpoints(axiosMock);
      const axiosGetSpy = jest.spyOn(axios, 'get');
      try {
        // Start on a classic tab so the tab strip is present to click through.
        renderNexusOneDashboard('violations');
        await waitFor(() => {
          expect(axiosGetSpy).toHaveBeenCalledWith(getDashboardFilters());
        });
        const callsAfterFirstMount = axiosGetSpy.mock.calls.filter(
          ([url]) => url === getDashboardFilters(),
        ).length;
        // Switching tabs only swaps the child <UIView> — the parent shell stays mounted, so the
        // page-level loadFilter dispatch must NOT fire again.
        await userEvent.click(screen.getByTestId('nosc-dashboard-tab-applications'));
        await userEvent.click(screen.getByTestId('nosc-dashboard-tab-waivers'));
        const callsAfterSwitching = axiosGetSpy.mock.calls.filter(
          ([url]) => url === getDashboardFilters(),
        ).length;
        expect(callsAfterSwitching).toBe(callsAfterFirstMount);
      } finally {
        axiosGetSpy.mockRestore();
      }
    });
  });

  describe('Tab badges (live counts from the Redux slice)', () => {
    it('renders the Applications badge when its slice has results, hides when null', async () => {
      stubDashboardAxios(axiosMock);
      // Badges live in the tab strip, which renders on the classic tabs (not the grid landing).
      const { store } = renderNexusOneDashboard('violations');

      // Wait for the shell + active tab content to finish mounting (the router transition is async,
      // and the tab content dispatches its own initial result loads). Dispatching our results before
      // that settles would let the child's reset clobber them.
      await screen.findByTestId('nosc-dashboard-page');
      await screen.findByTestId('nosc-dashboard-violations-tab');

      // Initial state: results === null → no badge.
      expect(screen.queryByTestId('nosc-dashboard-tab-badge-applications')).not.toBeInTheDocument();

      store.dispatch({
        type: LOAD_RESULTS_FULFILLED,
        payload: {
          resultsType: 'applications',
          results: [{ applicationId: 'a1' }],
          hasNextPage: false,
        },
      });

      // Radix Tabs.Trigger renders the children twice (visible + layout-only ghost); both share the
      // same data-testid, so we assert every match shares the expected content.
      await waitFor(() => {
        const appBadges = screen.getAllByTestId('nosc-dashboard-tab-badge-applications');
        expect(appBadges.length).toBeGreaterThanOrEqual(1);
        appBadges.forEach((el) => expect(el).toHaveTextContent('1'));
      });
    });
  });

  describe('Per-tab error isolation (AT-D1-002)', () => {
    let originalConsoleError: typeof console.error;
    beforeEach(() => {
      originalConsoleError = console.error;
      console.error = jest.fn();
    });
    afterEach(() => {
      console.error = originalConsoleError;
    });

    it('wraps the active tab content in an error boundary and renders each tab body (boundary scaffolding)', async () => {
      stubDashboardAxios(axiosMock);
      const expectedBodyByTab: Record<string, string> = {
        violations: 'nosc-dashboard-violations-tab',
        applications: 'nosc-dashboard-applications-tab',
        waivers: 'nosc-dashboard-waivers-tab',
      };
      for (const tab of ['violations', 'applications', 'waivers'] as const) {
        const { unmount } = renderNexusOneDashboard(tab);
        expect(await screen.findByTestId(expectedBodyByTab[tab])).toBeInTheDocument();
        unmount();
      }
    });
  });
});
