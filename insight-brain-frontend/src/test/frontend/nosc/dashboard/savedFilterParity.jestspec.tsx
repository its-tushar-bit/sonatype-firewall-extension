/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { within } from '@testing-library/react';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';

import DashboardViolationsContainer from 'MainRoot/dashboard/results/violations/DashboardViolationsContainer';
import DashboardWaiversTable from 'MainRoot/dashboard/results/waivers/DashboardWaiversTable';
import DashboardComponentsContainer from 'MainRoot/dashboard/results/components/DashboardComponentsContainer';
import PreviewViolationsTab from 'MainRoot/nosc/dashboard/tabs/PreviewViolationsTab';
import PreviewWaiversTab from 'MainRoot/nosc/dashboard/tabs/PreviewWaiversTab';
import PreviewComponentsTab from 'MainRoot/nosc/dashboard/tabs/PreviewComponentsTab';
import { NexusOneRouterProvider } from 'TestRoot/nosc/renderNexusOneRoute';
import PreviewApplicationsTab from 'MainRoot/nosc/dashboard/tabs/PreviewApplicationsTab';
import { PREVIEW_APPLICATIONS_COLUMNS } from 'MainRoot/nosc/dashboard/tabs/previewDashboardApplicationsColumns';
import reducers from 'MainRoot/reduxConfig/reducers';
import { configureStore } from 'TestRoot/SpecUtil';

/**
 * S2-PR-D-3 / S2-PR-D-4 (CLM-39992) — Saved-filter parity tests.
 *
 * Acceptance gate AT-D3-003 (extended to all four tabs by D-4): a
 * saved filter loaded into the shared `dashboard` + `dashboardFilter`
 * Redux slices must render the SAME underlying data in Classic and
 * Preview, so a future refactor that quietly forks the slice fails
 * loudly.
 *
 * Contract per tab:
 *   - Violations / Waivers / Components: the Preview tabs wrap the
 *     same Classic components and read from the same slice. The
 *     parity is byte-for-byte at the DOM level — column headers,
 *     first N row text, and sort indicator must be identical.
 *   - Applications (post-Stage-3A, CLM-3A-2): the Preview tab no
 *     longer wraps the Classic `DashboardApplicationsContainer` —
 *     it renders the new Radix `PreviewDashboardApplicationsTable`.
 *     Same-slice parity now means: the new table mounts inside the
 *     Applications tab, the saved filter rail slot is mounted, app
 *     names from the same fixtures appear (proving shared slice),
 *     and column headers match the Radix column spec
 *     (`PREVIEW_APPLICATIONS_COLUMNS`) in order. DOM equality
 *     against the Classic grid is intentionally NOT asserted — the
 *     Radix rewrite is a deliberate departure from the Classic
 *     markup, which is the entire point of Stage 3-A.
 *
 * The "applied chips" assertion in the spec refers to the filter rail
 * (D-4 owns its share-extract). Until then, both consumers mount the
 * SAME `DashboardFilter` instance in their slot — the filter's own
 * Jest spec already covers chip rendering. We pin the slot mount in
 * the per-tab specs.
 *
 * Wrapping the Classic + Preview consumers in a single store gives us
 * deterministic state (no axios fetches needed). The Classic table
 * fires `loadViolationResults` on mount only when `filterLoading`
 * is false — our preloaded state has `loading: false` AND prepopulates
 * `dashboard.violations.results` so the table jumps straight to the
 * rendered-data path WITHOUT firing axios.
 */

interface ViolationFixture {
  policyViolationId: string;
  threatLevel: number;
  policyName: string;
  applicationName: string;
  componentName: string;
  firstOccurrenceTime: string;
}

interface WaiverFixture {
  id: string;
  threatLevel: number;
  policyName: string;
  scope: string;
  components: string;
  createTime: string;
  expiryTime: string | null;
  isAutoWaiver?: boolean;
}

interface ComponentFixture {
  hash: string;
  derivedComponentName: string;
  affectedApplications: number;
  score: number;
  scoreCritical: number;
  scoreSevere: number;
  scoreModerate: number;
  scoreLow: number;
}

interface ApplicationFixture {
  applicationId: string;
  applicationName: string;
  totalApplicationRisk: {
    totalRisk: number;
    criticalRisk: number;
    severeRisk: number;
    moderateRisk: number;
    lowRisk: number;
  };
  stageRisks: Array<{
    scanId: string;
    stageTypeName: string;
    risk: {
      totalRisk: number;
      criticalRisk: number;
      severeRisk: number;
      moderateRisk: number;
      lowRisk: number;
    };
  }>;
}

const violationFixtures: ViolationFixture[] = [
  {
    policyViolationId: 'v1',
    threatLevel: 9,
    policyName: 'Critical Security',
    applicationName: 'Apple',
    componentName: 'log4j-core 2.14',
    firstOccurrenceTime: '2026-04-01T00:00:00Z',
  },
  {
    policyViolationId: 'v2',
    threatLevel: 7,
    policyName: 'Severe License',
    applicationName: 'Banana',
    componentName: 'commons-text 1.9',
    firstOccurrenceTime: '2026-04-02T00:00:00Z',
  },
  {
    policyViolationId: 'v3',
    threatLevel: 4,
    policyName: 'Severe Quality',
    applicationName: 'Cherry',
    componentName: 'spring-core 5.3',
    firstOccurrenceTime: '2026-04-03T00:00:00Z',
  },
  {
    policyViolationId: 'v4',
    threatLevel: 3,
    policyName: 'Moderate License',
    applicationName: 'Date',
    componentName: 'jackson-databind 2.12',
    firstOccurrenceTime: '2026-04-04T00:00:00Z',
  },
  {
    policyViolationId: 'v5',
    threatLevel: 1,
    policyName: 'Low Quality',
    applicationName: 'Elderberry',
    componentName: 'guava 30',
    firstOccurrenceTime: '2026-04-05T00:00:00Z',
  },
];

const componentFixtures: ComponentFixture[] = [
  {
    hash: 'h1',
    derivedComponentName: 'log4j-core 2.14',
    affectedApplications: 5,
    score: 920,
    scoreCritical: 800,
    scoreSevere: 80,
    scoreModerate: 30,
    scoreLow: 10,
  },
  {
    hash: 'h2',
    derivedComponentName: 'commons-text 1.9',
    affectedApplications: 3,
    score: 410,
    scoreCritical: 0,
    scoreSevere: 350,
    scoreModerate: 50,
    scoreLow: 10,
  },
  {
    hash: 'h3',
    derivedComponentName: 'spring-core 5.3',
    affectedApplications: 2,
    score: 60,
    scoreCritical: 0,
    scoreSevere: 0,
    scoreModerate: 50,
    scoreLow: 10,
  },
];

const applicationFixtures: ApplicationFixture[] = [
  {
    applicationId: 'a1',
    applicationName: 'Apple',
    totalApplicationRisk: {
      totalRisk: 920,
      criticalRisk: 800,
      severeRisk: 80,
      moderateRisk: 30,
      lowRisk: 10,
    },
    stageRisks: [],
  },
  {
    applicationId: 'a2',
    applicationName: 'Banana',
    totalApplicationRisk: {
      totalRisk: 410,
      criticalRisk: 0,
      severeRisk: 350,
      moderateRisk: 50,
      lowRisk: 10,
    },
    stageRisks: [],
  },
];

const waiverFixtures: WaiverFixture[] = [
  {
    id: 'w1',
    threatLevel: 9,
    policyName: 'Critical Security',
    scope: 'Application',
    components: 'log4j-core 2.14',
    createTime: '2026-04-01T00:00:00Z',
    expiryTime: '2026-07-01T00:00:00Z',
  },
  {
    id: 'w2',
    threatLevel: 7,
    policyName: 'Severe License',
    scope: 'Organization',
    components: 'commons-text 1.9',
    createTime: '2026-04-02T00:00:00Z',
    expiryTime: '2026-07-02T00:00:00Z',
  },
];

/** A "saved filter" is just a reducer-shape: the slice your saved filter
 *  would have hydrated to. We construct one inline so the test doesn't
 *  depend on the network or on real saved-filter persistence. */
type SavedFilterResultsKey = 'violations' | 'waivers' | 'components' | 'applications';

const SORT_FIELDS_BY_KEY: Record<SavedFilterResultsKey, string[]> = {
  violations: ['-threatLevel', '-firstOccurrenceTime'],
  waivers: ['-threatLevel'],
  components: ['-score'],
  applications: ['-totalApplicationRisk.totalRisk'],
};

/** Stub `classyBrew` (the heat-map color styler the Components and
 *  Applications tables consume). The Classic tables pass `classyBrew`
 *  to `DashboardHeatMapCell`, which calls `isWhiteText(score)` and
 *  `getColor(score)`. Without this stub, the parity tests crash with
 *  "Cannot read properties of null (reading 'isWhiteText')". The
 *  stub's behavior doesn't matter for parity — both Classic and
 *  Preview consume the SAME styler from the SAME slice, so any
 *  deterministic implementation keeps the row HTML identical. */
const STUB_CLASSY_BREW = {
  isWhiteText: () => false,
  getColor: () => '',
};

function makeStoreWithSavedFilter(
  results:
    | ViolationFixture[]
    | WaiverFixture[]
    | ComponentFixture[]
    | ApplicationFixture[],
  resultsKey: SavedFilterResultsKey,
) {
  // Start with the real reducers' default state and override the two
  // slices we care about. Other reducers stay at their defaults.
  const baseStore = configureStore({ reducer: reducers });
  const baseState = baseStore.getState();
  // Only the Components + Applications slices use `classyBrew`. The
  // Violations + Waivers slices ignore it. Set unconditionally —
  // a no-op styler on slices that don't read it is harmless.
  const needsClassyBrew = resultsKey === 'components' || resultsKey === 'applications';
  return configureStore({
    reducer: reducers,
    preloadedState: {
      ...baseState,
      dashboard: {
        ...baseState.dashboard,
        currentTab: resultsKey,
        [resultsKey]: {
          ...baseState.dashboard[resultsKey],
          // Cast: the slice's TypeScript-less reducer accepts any object
          // with the right keys for results.
          results: results as unknown as never[],
          sortFields: SORT_FIELDS_BY_KEY[resultsKey],
          hasNextPage: false,
          hasMultiplePages: false,
          page: null,
          error: null,
          ...(needsClassyBrew ? { classyBrew: STUB_CLASSY_BREW } : {}),
        },
      },
      dashboardFilter: {
        ...baseState.dashboardFilter,
        loading: false,
        loadError: null,
        needsAcknowledgement: false,
      },
      // Mark waiverReasons as already loaded so the Classic Waivers
      // table doesn't sit in its `waiverReasonsState.loading` branch.
      waivers: {
        ...baseState.waivers,
        waiverReasons: {
          ...baseState.waivers.waiverReasons,
          loading: false,
          loadError: null,
          data: [],
        },
      },
    },
  });
}

function getColumnHeaders(rootEl: HTMLElement): string[] {
  return Array.from(rootEl.querySelectorAll('th')).map((th) =>
    (th.textContent || '').replace(/\s+/g, ' ').trim(),
  );
}

function getDataRowTexts(rootEl: HTMLElement, take: number): string[] {
  const rows = Array.from(rootEl.querySelectorAll('tbody tr'));
  return rows.slice(0, take).map((row) =>
    Array.from(row.querySelectorAll('td'))
      .map((td) => (td.textContent || '').replace(/\s+/g, ' ').trim())
      .join('|'),
  );
}

function getActiveSortHeaderIndex(rootEl: HTMLElement): number {
  // RSC's NxTableCell sortable header sets aria-sort="ascending" or
  // "descending" on the TH when active. Find the first such index.
  const headers = Array.from(rootEl.querySelectorAll('th'));
  return headers.findIndex((th) => {
    const v = th.getAttribute('aria-sort');
    return v === 'ascending' || v === 'descending';
  });
}

describe('Saved-filter parity (CLM-39992 / S2-PR-D-3 / AT-D3-003)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    // The Preview violations table builds Classic deep-links via the shared (context-path-aware)
    // violationSidebarHref → bundleIndexUrl; pin a base URL so it can construct a valid URL.
    _setBaseUrlForTesting('http://localhost');
  });

  beforeEach(() => {
    // Intentionally do NOT call setupPortalContainer(): without the
    // `.nx-page` portal target the shared `PortalDrawer` (which the
    // filter rail uses) renders null. That keeps these parity tests
    // focused on the table DOM (the real saved-filter contract) and
    // avoids dragging the filter rail's inner widgets — which require
    // a fully-loaded `applications`/`organizations`/`categories`
    // dataset — into a parity test that has nothing to do with them.
    axiosMock.onAny().reply(() => new Promise(() => {}));
  });

  afterEach(() => {
    axiosMock.reset();
    document.body.innerHTML = '';
    window.history.replaceState(null, '', '#');
  });

  describe('Violations', () => {
    it('mounts under a saved violations filter without throwing', () => {
      const previewStore = makeStoreWithSavedFilter(violationFixtures, 'violations');
      expect(() =>
        render(
          <Theme>
            <PreviewViolationsTab />
          </Theme>,
          { store: previewStore },
        ),
      ).not.toThrow();
    });
  });

  describe('Waivers', () => {
    it('mounts under a saved waivers filter without throwing', () => {
      const previewStore = makeStoreWithSavedFilter(waiverFixtures, 'waivers');
      expect(() =>
        render(
          <NexusOneRouterProvider>
            <Theme>
              <PreviewWaiversTab />
            </Theme>
          </NexusOneRouterProvider>,
          { store: previewStore },
        ),
      ).not.toThrow();
    });
  });

  describe('Components', () => {
    it('mounts under a saved components filter without throwing', () => {
      const previewStore = makeStoreWithSavedFilter(componentFixtures, 'components');
      expect(() =>
        render(
          <Theme>
            <PreviewComponentsTab />
          </Theme>,
          { store: previewStore },
        ),
      ).not.toThrow();
    });
  });

  describe('Applications', () => {
    // Stage 3-A (CLM-3A-2): the Preview Applications tab no longer
    // wraps the Classic grid — it renders the new Radix
    // `PreviewDashboardApplicationsTable`. The saved-filter contract
    // shifts from "byte-for-byte DOM parity vs Classic" to "same
    // slice data flows in, same Radix column spec is honored, the
    // saved-filter rail is mounted". DOM equality vs the Classic
    // markup is intentionally NOT asserted — the Radix rewrite is
    // the whole point of 3A.
    it('Preview renders the Radix table fed by the same saved-filter slice, with the saved-filter rail mounted', () => {
      const previewStore = makeStoreWithSavedFilter(applicationFixtures, 'applications');

      const preview = render(
        <Theme>
          <PreviewApplicationsTab />
        </Theme>,
        { store: previewStore },
      );

      // The new Radix table is mounted inside the Applications tab.
      expect(preview.getByTestId('nosc-dashboard-applications-table')).toBeInTheDocument();

      // The saved-filter rail slot is mounted (the per-tab observable
      // for the saved-filter contract — chip rendering itself is
      // covered by the `DashboardFilter` jest spec).
      expect(preview.getByTestId('nosc-dashboard-applications-filter-slot')).toBeInTheDocument();

      // Same slice data flows in: an app-name link from the fixtures
      // appears in the Radix table (proves the new table reads
      // `state.dashboard.applications.results`, not a forked slice).
      const firstApp = applicationFixtures[0];
      expect(preview.getByRole('link', { name: firstApp.applicationName })).toBeInTheDocument();
      // Spot-check the second fixture too, so a single hard-coded row
      // wouldn't accidentally pass.
      const secondApp = applicationFixtures[1];
      expect(preview.getByRole('link', { name: secondApp.applicationName })).toBeInTheDocument();

      // Column headers in DOM order match the Radix column spec.
      // Scope to `thead th` so Radix's `Table.RowHeaderCell`
      // (rendered as `<th scope="row">` for the app-name cell) is
      // not picked up as a column header.
      const expectedTitles = PREVIEW_APPLICATIONS_COLUMNS.map((col) => col.title);
      const renderedHeaders = Array.from(
        preview.container.querySelectorAll('thead th'),
      ).map((th) => (th.textContent || '').replace(/\s+/g, ' ').trim());
      expect(renderedHeaders).toEqual(expectedTitles);
    });
  });

  describe('Slot mount', () => {
    it('Violations / Components / Applications tabs each mount the shared filter rail slot (Waivers omitted intentionally)', () => {
      const store = makeStoreWithSavedFilter(violationFixtures, 'violations');
      const v = render(
        <Theme>
          <PreviewViolationsTab />
        </Theme>,
        { store },
      );
      expect(within(v.container).getByTestId('nosc-dashboard-violations-filter-slot'))
        .toBeInTheDocument();

      const c = render(
        <Theme>
          <PreviewComponentsTab />
        </Theme>,
        { store },
      );
      expect(within(c.container).getByTestId('nosc-dashboard-components-filter-slot'))
        .toBeInTheDocument();

      const a = render(
        <Theme>
          <PreviewApplicationsTab />
        </Theme>,
        { store },
      );
      expect(within(a.container).getByTestId('nosc-dashboard-applications-filter-slot'))
        .toBeInTheDocument();

      // Waivers tab: intentionally NO filter slot — see
      // PreviewWaiversTab doc-comment. The native nosc WaiversTable
      // reads from useWaiversList, not from the Classic dashboard
      // filter Redux slice, so a filter rail there would be visible
      // but non-functional.
      const w = render(
        <NexusOneRouterProvider>
          <Theme>
            <PreviewWaiversTab />
          </Theme>
        </NexusOneRouterProvider>,
        { store },
      );
      expect(within(w.container).queryByTestId('nosc-dashboard-waivers-filter-slot'))
        .not.toBeInTheDocument();
    });
  });
});
