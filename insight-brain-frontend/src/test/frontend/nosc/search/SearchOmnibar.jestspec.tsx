/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import { SearchOmnibar } from 'MainRoot/nosc/search/SearchOmnibar';
import router from 'MainRoot/router/routerInstance';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { SuggestResponse } from 'MainRoot/nosc/search/searchTypes';
import { recentSearchesStorageKey } from 'MainRoot/nosc/search/useRecentSearches';
import { registerNexusOneApplicationDetailStatesForHref } from 'TestRoot/nosc/search/registerNexusOneApplicationDetailStatesForHref';
import { registerNexusOneVulnerabilityDetailStatesForHref } from 'TestRoot/nosc/search/registerNexusOneVulnerabilityDetailStatesForHref';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

/**
 * Tests for the expand-in-place SearchOmnibar.
 *
 * The structural contract under test (mirroring the Nexus One prototype):
 *   - CLOSED, the omnibar is the search field ALONE: no data-source select and
 *     no filter toggle, plus the "/" and Cmd-K shortcut chips.
 *   - FOCUSED, the same surface expands into a panel card whose header carries
 *     the data-source select + input + filter toggle.
 *   - The panel body renders exactly one view per panel state: recent searches,
 *     placeholder, or the results view (horizontal tabs + mixed rows).
 */

// Click targets use the real UI-Router singleton (Nexus One states registered
// below) and the real bundleIndexUrl (Classic deep-links) with a test base URL.
const CLASSIC = 'http://localhost/assets/index.html';

function registerState(name: string, url: string): void {
  if (!router.stateRegistry.get(name)) {
    router.stateRegistry.register({ name, url });
  }
}

const SEARCH_QUERY = 'log4j';
const SUGGEST_RE = /\/rest\/search\/suggest/;

/**
 * Seed the product-features slice so the CATALOG_FEDERATION selector is populated
 * without a network fetch. `catalog-federation` gates the "Sonatype Catalog" option.
 */
/** Signed-in user these tests run as; recent searches are stored under this account's key. */
const TEST_USERNAME = 'test-user';

/** The recent-searches localStorage key for the signed-in test user. */
const RECENT_KEY = recentSearchesStorageKey(TEST_USERNAME) as string;

function featuresState({ catalogFederation = true }: { catalogFederation?: boolean } = {}) {
  const productFeatures: Record<string, boolean> = {};
  if (catalogFederation) productFeatures['catalog-federation'] = true;
  return {
    productFeatures: { productFeatures, loading: false, loadError: null },
    // Recent searches are keyed by account, so the omnibar needs a signed-in user
    // to read or write any history.
    userSession: { data: { username: TEST_USERNAME }, loading: false, error: null },
  };
}

/**
 * The query input and (once open) the data-source Select trigger both expose
 * role=combobox, so target the input by its accessible name to disambiguate.
 */
function getSearchInput(): HTMLElement {
  return screen.getByRole('combobox', {
    name: /search applications, components, violations, and vulnerabilities/i,
  });
}

/**
 * The expanded panel surface. The surface is deliberately role-less (it is the
 * combobox's popup, not a dialog), so open-ness is read from data-open.
 */
function getPanelSurface(): HTMLElement {
  return screen.getByTestId('nosc-search-omnibar-surface');
}

async function findOpenPanel(): Promise<HTMLElement> {
  const surface = getPanelSurface();
  await waitFor(() => expect(surface).toHaveAttribute('data-open', 'true'));
  return surface;
}

function expectPanelClosed(): void {
  expect(getPanelSurface()).not.toHaveAttribute('data-open');
}

const FIXTURE_RESPONSE: SuggestResponse = {
  bestMatch: {
    id: 'CVE-2021-44228',
    type: 'VULNERABILITY',
    source: 'local',
    title: 'CVE-2021-44228',
    subtitle: 'Log4Shell — Critical RCE in log4j',
  },
  groups: [
    { type: 'VULNERABILITY', source: 'local', results: [] },
    {
      type: 'COMPONENT',
      source: 'local',
      results: [{ id: 'c-1', type: 'COMPONENT', source: 'local', title: 'log4j-core', subtitle: 'maven · 2.14.1' }],
    },
    {
      type: 'APPLICATION',
      source: 'local',
      results: [
        // Real wire shape: id is the internal application id; subtitle carries the public id.
        { id: 'internal-app-42', type: 'APPLICATION', source: 'local', title: 'Webgoat', subtitle: 'webgoat-public' },
      ],
    },
    {
      type: 'VIOLATION',
      source: 'local',
      results: [
        { id: 'pv-1', type: 'VIOLATION', source: 'local', title: 'Security-Critical', subtitle: 'Webgoat violation' },
      ],
    },
    { type: 'WAIVER', source: 'local', results: [] },
  ],
};

const ALL_EMPTY_GROUPS: SuggestResponse = {
  bestMatch: null,
  groups: [
    { type: 'VULNERABILITY', source: 'local', results: [] },
    { type: 'COMPONENT', source: 'local', results: [] },
    { type: 'APPLICATION', source: 'local', results: [] },
    { type: 'VIOLATION', source: 'local', results: [] },
    { type: 'WAIVER', source: 'local', results: [] },
  ],
};

describe('SearchOmnibar', () => {
  const originalLocation = window.location;
  let assignMock: jest.Mock;
  // The results page is an in-app UI-Router state, so entering it goes through the
  // router rather than a full-page location.assign.
  let goSpy: jest.SpyInstance;
  let mock: MockAdapter;

  // The data-source Select and the filter menus are Radix poppers that need the
  // jsdom shims (ResizeObserver, pointer capture, scrollIntoView) to open.
  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    window.localStorage.clear();
    assignMock = jest.fn();
    delete (window as any).location;
    (window as any).location = { ...originalLocation, assign: assignMock, hash: '' };
    mock = new MockAdapter(axios);
    mock.onGet(SUGGEST_RE).reply(200, FIXTURE_RESPONSE);
    goSpy = jest.spyOn(router.stateService, 'go').mockResolvedValue(undefined as never);

    registerNexusOneApplicationDetailStatesForHref();
    registerNexusOneVulnerabilityDetailStatesForHref();
    registerState('nexusOneSearch', '/search?q&tab&source');
    registerState('platformHome', '/home');
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    (window as any).location = originalLocation;
    goSpy.mockRestore();
    mock.restore();
  });

  const renderInTheme = (opts: { catalogFederation?: boolean } = {}) =>
    render(
      <Theme>
        <SearchOmnibar />
      </Theme>,
      { preloadedState: featuresState(opts) }
    );

  // -------------------------------------------------------------------------
  // Collapsed structure
  // -------------------------------------------------------------------------
  describe('collapsed (unfocused)', () => {
    it('renders the search field alone, with no data-source select and no filter toggle', () => {
      renderInTheme({ catalogFederation: true });
      expect(getSearchInput()).toHaveValue('');
      expect(screen.queryByTestId('nosc-search-datasource')).not.toBeInTheDocument();
      expect(screen.queryByTestId('nosc-search-filter-toggle')).not.toBeInTheDocument();
    });

    it('shows the "/" and Cmd-K shortcut hints on the resting field', () => {
      renderInTheme();
      const hints = screen.getByTestId('nosc-search-shortcut-hints');
      expect(hints).toBeInTheDocument();
      expect(hints.textContent).toContain('/');
      expect(hints.textContent).toContain('K');
    });

    it('does not render the panel body or footer while closed', () => {
      renderInTheme();
      expect(screen.queryByTestId('nosc-search-panel-body')).not.toBeInTheDocument();
      expect(screen.queryByTestId('nosc-search-panel-footer')).not.toBeInTheDocument();
      expectPanelClosed();
    });

    it('exposes an explicit accessible name on the search input (WCAG 2.4.6)', () => {
      renderInTheme();
      expect(
        screen.getByLabelText(/search applications, components, violations, and vulnerabilities/i)
      ).toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // Focus expands the surface into the panel
  // -------------------------------------------------------------------------
  describe('focus expands into the panel', () => {
    it('reveals the data-source select and filter toggle inside the expanded card', async () => {
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: true });
      await user.click(getSearchInput());

      const dialog = await findOpenPanel();
      // Both controls belong to the expanded card's header, not the nav row.
      expect(screen.getByTestId('nosc-search-datasource')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-filter-toggle')).toBeInTheDocument();
      expect(dialog).toContainElement(screen.getByTestId('nosc-search-datasource'));
      expect(dialog).toContainElement(screen.getByTestId('nosc-search-filter-toggle'));
    });

    it('hides the shortcut hints once the panel is open', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await findOpenPanel();
      expect(screen.queryByTestId('nosc-search-shortcut-hints')).not.toBeInTheDocument();
    });

    it('renders the panel footer with the syntax link', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await screen.findByTestId('nosc-search-panel-footer');
      expect(screen.getByTestId('nosc-search-syntax-link')).toBeInTheDocument();
    });

    it('omits the data-source select when CATALOG_FEDERATION is disabled', async () => {
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: false });
      await user.click(getSearchInput());
      await findOpenPanel();
      expect(screen.queryByTestId('nosc-search-datasource')).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // focused-empty / focused-short → Recent Searches
  // -------------------------------------------------------------------------
  describe('recent searches view', () => {
    it('shows the empty recent-searches message on focus with no history', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      expect(await screen.findByTestId('nosc-search-recent-empty')).toBeInTheDocument();
    });

    it('lists persisted recent searches with clock rows', async () => {
      window.localStorage.setItem(
        RECENT_KEY,
        JSON.stringify([
          { q: 'log4j', ts: 2 },
          { q: 'itemType:APPLICATION', ts: 1 },
        ])
      );
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());

      await screen.findByTestId('nosc-search-recent');
      expect(screen.getByText('log4j')).toBeInTheDocument();
      expect(screen.getByText('itemType:APPLICATION')).toBeInTheDocument();
    });

    it('keeps showing recent searches while the query is below the minimum length', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), 'l');
      expect(await screen.findByTestId('nosc-search-recent-empty')).toBeInTheDocument();
      expect(screen.queryByTestId('nosc-search-results-view')).not.toBeInTheDocument();
    });

    it('records the submitted query so it becomes a recent search', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');
      await user.keyboard('{Enter}');

      const stored = JSON.parse(window.localStorage.getItem(RECENT_KEY) ?? '[]');
      expect(stored[0].q).toBe(SEARCH_QUERY);
    });

    it('activating a recent search navigates to the results page for that query', async () => {
      window.localStorage.setItem(RECENT_KEY, JSON.stringify([{ q: 'past-query', ts: 1 }]));
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      const row = await screen.findByTestId('nosc-search-recent-row-0');
      await user.click(row);
      expect(goSpy).toHaveBeenCalledWith('nexusOneSearch', { q: 'past-query' });
    });
  });

  // -------------------------------------------------------------------------
  // loaded → tabs + mixed rows
  // -------------------------------------------------------------------------
  describe('results view', () => {
    it('renders the "Show results for" row above the rows', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-show-results-row');
      expect(screen.getByText(/show results for/i)).toBeInTheDocument();
    });

    it('renders a horizontal tab strip with per-type count badges', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);

      await screen.findByTestId('nosc-search-panel-tabs');
      expect(screen.getByTestId('nosc-search-panel-tab-ALL')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-panel-tab-APPLICATION')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-panel-tab-COMPONENT')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-panel-tab-VIOLATION')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-panel-tab-VULNERABILITY')).toBeInTheDocument();
    });

    it('labels the violations tab "Violations" so the strip fits the panel width', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      const tab = await screen.findByTestId('nosc-search-panel-tab-VIOLATION');
      expect(tab.textContent).toContain('Violations');
      expect(tab.textContent).not.toContain('Policy Violations');
    });

    it('renders the filter category pills on a single row inside the panel', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await user.click(await screen.findByTestId('nosc-search-filter-toggle'));
      const toolbar = await screen.findByRole('toolbar', { name: 'Search filters' });
      // A trigger chevron on each pill pushes the eight categories past the
      // panel width and wraps them onto a second row.
      expect(toolbar.querySelectorAll('button svg')).toHaveLength(0);
    });

    it('renders ONE mixed row list rather than per-type grouped sections', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');

      // Best match leads, then one row per remaining type — all in a single list.
      expect(screen.getByText('CVE-2021-44228')).toBeInTheDocument();
      expect(screen.getByText('log4j-core')).toBeInTheDocument();
      expect(screen.getByText('Webgoat')).toBeInTheDocument();
      expect(screen.getByText('Security-Critical')).toBeInTheDocument();
      // The old grouped layout's section eyebrows must be gone.
      expect(screen.queryByText(/best match/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/^Components \(/)).not.toBeInTheDocument();
      expect(screen.queryByText(/^Applications \(/)).not.toBeInTheDocument();
    });

    it('keeps the ARIA tree valid: options only inside listboxes, listboxes only holding options', async () => {
      // Regression (axe critical): the "Show results for" and "View more" options
      // sat outside any listbox (aria-required-parent); a first fix then nested
      // the tab strip inside the listbox, which broke aria-required-children
      // because a listbox admits only options.
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');

      const listboxes = screen.getAllByRole('listbox');
      for (const option of screen.getAllByRole('option')) {
        expect(listboxes.some((listbox) => listbox.contains(option))).toBe(true);
      }
      for (const listbox of listboxes) {
        for (const child of Array.from(listbox.children)) {
          expect(child.getAttribute('role')).toBe('option');
        }
      }
    });

    it('gives each listbox a unique id and names both on the input\u2019s aria-controls', async () => {
      // The rows listbox holds the actual result rows, so AT following
      // aria-controls must reach it and not just the lead "Show results for"
      // option. Each listbox carries its own unique id.
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');

      const ids = screen.getAllByRole('listbox').map((listbox) => listbox.id);
      expect(ids.every(Boolean)).toBe(true);
      expect(new Set(ids).size).toBe(ids.length);

      const controls = getSearchInput().getAttribute('aria-controls')?.split(/\s+/) ?? [];
      expect(controls.length).toBe(2);
      for (const id of controls) {
        expect(document.getElementById(id)).not.toBeNull();
      }
      // Every option resolves through one of the named listboxes.
      const named = controls.map((id) => document.getElementById(id));
      for (const option of screen.getAllByRole('option')) {
        expect(named.some((listbox) => listbox?.contains(option))).toBe(true);
      }
    });

    it('names only the lead listbox when the recent view is the only list', async () => {
      window.localStorage.setItem(
        RECENT_KEY,
        JSON.stringify([{ q: 'spring', ts: 1 }])
      );
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await screen.findByTestId('nosc-search-recent-row-0');

      const controls = getSearchInput().getAttribute('aria-controls') ?? '';
      expect(controls.split(/\s+/)).toHaveLength(1);
      expect(document.getElementById(controls)).not.toBeNull();
    });

    it('still resolves aria-controls when there is no history to list', async () => {
      // role="combobox" requires aria-controls, so the empty recent message
      // carries the lead id: the reference resolves without asserting a listbox
      // that has no options.
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      const emptyMessage = await screen.findByTestId('nosc-search-recent-empty');

      const controls = getSearchInput().getAttribute('aria-controls') ?? '';
      expect(controls.split(/\s+/)).toHaveLength(1);
      expect(document.getElementById(controls)).toBe(emptyMessage);
      expect(screen.queryByRole('listbox')).toBeNull();
    });

    it('gives every tab a resolvable aria-controls, or none at all', async () => {
      // Regression (axe critical): a Radix Tabs.Trigger emits an aria-controls
      // pointing at a Tabs.Content this panel never renders.
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-panel-tabs');

      for (const tab of screen.getAllByRole('tab')) {
        const controls = tab.getAttribute('aria-controls');
        if (controls) expect(document.getElementById(controls)).not.toBeNull();
      }
    });

    it('shows a searching line while the request is in flight', async () => {
      const user = userEvent.setup();
      // Hold the response open so the loading state is observable.
      mock.reset();
      mock.onGet(SUGGEST_RE).reply(() => new Promise(() => undefined));
      renderInTheme();

      await user.type(getSearchInput(), SEARCH_QUERY);

      await screen.findByTestId('nosc-search-placeholder-loading');
      expect(screen.getByTestId('nosc-search-loading-text')).toHaveTextContent(/searching/i);
    });

    it('narrows the rows to one type when a type tab is selected', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-panel-tab-COMPONENT');

      await user.click(screen.getByTestId('nosc-search-panel-tab-COMPONENT'));

      await waitFor(() => expect(screen.getByText('log4j-core')).toBeInTheDocument());
      expect(screen.queryByText('Webgoat')).not.toBeInTheDocument();
      expect(screen.queryByText('CVE-2021-44228')).not.toBeInTheDocument();
    });

    it('hides the tab strip when the query already carries an itemType: token', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), 'itemType:APPLICATION');
      await screen.findByTestId('nosc-search-results-view');
      expect(screen.queryByTestId('nosc-search-panel-tabs')).not.toBeInTheDocument();
    });

    it('issues a single request to the suggest endpoint', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0));
      expect(mock.history.get).toHaveLength(1);
      expect(mock.history.get[0].url ?? '').toContain('/rest/search/suggest');
    });

    it('shows the placeholder view with no rows when nothing matched', async () => {
      mock.reset();
      mock.onGet(SUGGEST_RE).reply(200, ALL_EMPTY_GROUPS);
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), 'no-matches-query');
      expect(await screen.findByTestId('nosc-search-placeholder-empty')).toBeInTheDocument();
      expect(screen.queryByTestId('nosc-search-result-row-0')).not.toBeInTheDocument();
    });

    it('surfaces a backend failure in the placeholder view', async () => {
      mock.reset();
      mock.onGet(SUGGEST_RE).reply(500, 'boom');
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      expect(await screen.findByTestId('nosc-search-placeholder-error')).toBeInTheDocument();
    });
  });

  // -------------------------------------------------------------------------
  // Filter bar lives INSIDE the panel
  // -------------------------------------------------------------------------
  describe('filter bar', () => {
    it('is hidden until the filter toggle is pressed', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await screen.findByTestId('nosc-search-filter-toggle');
      expect(screen.queryByRole('toolbar', { name: 'Search filters' })).not.toBeInTheDocument();
    });

    it('renders the category pill row inside the panel when toggled on', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await user.click(await screen.findByTestId('nosc-search-filter-toggle'));

      const toolbar = await screen.findByRole('toolbar', { name: 'Search filters' });
      const dialog = getPanelSurface();
      // The pills must live INSIDE the panel, not in a detached page-width row.
      expect(dialog).toContainElement(toolbar);
      expect(screen.getByTestId('nosc-search-filter-category-type')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-filter-category-application')).toBeInTheDocument();
    });

    it('marks the toggle expanded while the filter bar is open', async () => {
      // A disclosure button, so aria-expanded alone carries the state; aria-pressed
      // would additionally announce it as a toggle button.
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      const toggle = await screen.findByTestId('nosc-search-filter-toggle');
      expect(toggle).toHaveAttribute('aria-expanded', 'false');
      expect(toggle).not.toHaveAttribute('aria-pressed');
      expect(toggle).toHaveAttribute('aria-controls', 'nosc-search-filter-bar');

      await user.click(toggle);
      const expanded = screen.getByTestId('nosc-search-filter-toggle');
      expect(expanded).toHaveAttribute('aria-expanded', 'true');
      expect(expanded).not.toHaveAttribute('aria-pressed');
    });

    it('inserts a chosen leaf syntax into the query', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await user.click(await screen.findByTestId('nosc-search-filter-toggle'));
      await user.click(await screen.findByTestId('nosc-search-filter-category-type'));
      await user.click(await screen.findByTestId('nosc-search-filter-leaf-itemType:APPLICATION'));

      await waitFor(() => expect(getSearchInput()).toHaveValue('itemType:APPLICATION'));
    });
  });

  // -------------------------------------------------------------------------
  // Row activation
  // -------------------------------------------------------------------------
  describe('row activation', () => {
    it('navigates to native vulnerability detail for a CVE row', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await user.click(await screen.findByTestId('nosc-search-result-row-0'));
      expect(assignMock).toHaveBeenCalledWith('#/vulnerabilities/CVE-2021-44228/security-details');
    });

    it('navigates by public id when an application row is activated', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');
      await user.click(screen.getByText('Webgoat'));
      expect(assignMock).toHaveBeenCalledWith('#/applications/webgoat-public');
    });

    it('does not record the typed fragment as a recent search', async () => {
      // Activating a row jumps to an entity; it is not a search the user performed, so
      // the partial text in the input must not be persisted as a recent query.
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await user.click(await screen.findByTestId('nosc-search-result-row-0'));

      const stored = JSON.parse(window.localStorage.getItem(RECENT_KEY) ?? '[]');
      expect(stored).toEqual([]);
    });

    it('navigates to the Classic violation sidebar for a violation row', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');
      await user.click(screen.getByText('Security-Critical'));
      expect(assignMock).toHaveBeenCalledWith(`${CLASSIC}#/violation/pv-1`);
    });

    it('"Show results for" navigates to the full results page', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await user.click(await screen.findByTestId('nosc-search-show-results-row'));
      expect(goSpy).toHaveBeenCalledWith('nexusOneSearch', { q: SEARCH_QUERY });
    });

    it('does not navigate on a right-click, leaving the context menu alone', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      const row = await screen.findByTestId('nosc-search-result-row-0');

      await user.pointer({ target: row, keys: '[MouseRight]' });

      expect(assignMock).not.toHaveBeenCalled();
    });

    it('does not navigate on a middle-click', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      const row = await screen.findByTestId('nosc-search-result-row-0');

      await user.pointer({ target: row, keys: '[MouseMiddle]' });

      expect(assignMock).not.toHaveBeenCalled();
    });

    it('keeps focus on the input after a right-click so the composite survives', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      const row = await screen.findByTestId('nosc-search-result-row-0');

      await user.pointer({ target: row, keys: '[MouseRight]' });

      expect(getSearchInput()).toHaveFocus();
    });

    it('still navigates on a normal left-click of a recent search', async () => {
      window.localStorage.setItem(
        RECENT_KEY,
        JSON.stringify([{ q: 'spring', ts: 1 }])
      );
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());

      await user.click(await screen.findByTestId('nosc-search-recent-row-0'));

      expect(goSpy).toHaveBeenCalledWith('nexusOneSearch', { q: 'spring' });
    });

    it('does not activate a recent search on a right-click', async () => {
      window.localStorage.setItem(
        RECENT_KEY,
        JSON.stringify([{ q: 'spring', ts: 1 }])
      );
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      const row = await screen.findByTestId('nosc-search-recent-row-0');

      await user.pointer({ target: row, keys: '[MouseRight]' });

      expect(assignMock).not.toHaveBeenCalled();
    });
  });

  // -------------------------------------------------------------------------
  // Keyboard
  // -------------------------------------------------------------------------
  describe('keyboard', () => {
    it('bare Enter falls back to the full results page', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');
      await user.keyboard('{Enter}');
      expect(goSpy).toHaveBeenCalledWith('nexusOneSearch', { q: SEARCH_QUERY });
    });

    it('ArrowDown then Enter activates the first result row', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-result-row-0');
      await user.keyboard('{ArrowDown}');
      await user.keyboard('{Enter}');
      expect(assignMock).toHaveBeenCalledWith('#/vulnerabilities/CVE-2021-44228/security-details');
    });

    it('tracks the highlighted option via aria-activedescendant', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-result-row-0');
      // Default highlight is the "Show results for" row so bare Enter is safe.
      expect(getSearchInput()).toHaveAttribute('aria-activedescendant', 'nosc-search-show-results');

      await user.keyboard('{ArrowDown}');
      expect(getSearchInput()).toHaveAttribute('aria-activedescendant', 'nosc-search-row-0');
    });

    it('Escape closes the panel but keeps the typed query', async () => {
      const user = userEvent.setup();
      renderInTheme();
      const input = getSearchInput();
      await user.type(input, SEARCH_QUERY);
      await findOpenPanel();

      await user.keyboard('{Escape}');

      await waitFor(() => expectPanelClosed());
      expect(getSearchInput()).toHaveValue(SEARCH_QUERY);
    });

    it('closes on a SINGLE Escape even though Escape returns focus to the input', async () => {
      // Regression: closePanel refocuses the input, whose focus handler reopens
      // the panel. Without suppressing that reopen the first Escape looked like
      // a no-op and the user had to press it twice.
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await findOpenPanel();

      await user.keyboard('{Escape}');

      await waitFor(() => expectPanelClosed());
      // Focus stays on the input so the user can keep typing.
      expect(getSearchInput()).toHaveFocus();
    });

    it('Escape also closes the filter bar so the toggle cannot stay stuck open', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await user.click(await screen.findByTestId('nosc-search-filter-toggle'));
      await screen.findByRole('toolbar', { name: 'Search filters' });

      await user.keyboard('{Escape}');

      await waitFor(() => expect(screen.queryByRole('toolbar', { name: 'Search filters' })).not.toBeInTheDocument());
    });

    it('Cmd-K focuses the input and opens the panel from anywhere', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.keyboard('{Meta>}k{/Meta}');
      await findOpenPanel();
      expect(getSearchInput()).toHaveFocus();
    });

    it('"/" focuses the input when focus is not already in a text field', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.keyboard('/');
      await findOpenPanel();
      expect(getSearchInput()).toHaveFocus();
      // The shortcut must not leak into the query.
      expect(getSearchInput()).toHaveValue('');
    });

    it('leaves Cmd-K to the open data-source select instead of stealing focus', async () => {
      // The popper bail-out must precede the shortcut branch: otherwise Cmd-K
      // refocuses the input and Radix dismisses the select the user is using.
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: true });
      await user.click(getSearchInput());
      await user.click(await screen.findByLabelText('Search data source'));
      const option = await screen.findByRole('option', { name: 'Sonatype Catalog' });

      await user.keyboard('{Meta>}k{/Meta}');

      // The select is still open and still owns focus. Queried by testid because
      // the open Select hides the rest of the page from the a11y tree.
      expect(option).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-input')).not.toHaveFocus();
    });

    it('leaves "/" to an open filter menu rather than refocusing the input', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await user.click(await screen.findByTestId('nosc-search-filter-toggle'));
      await user.click(await screen.findByTestId('nosc-search-filter-category-type'));
      await screen.findByTestId('nosc-search-filter-leaf-itemType:APPLICATION');

      await user.keyboard('/');

      expect(screen.getByTestId('nosc-search-filter-leaf-itemType:APPLICATION')).toBeInTheDocument();
      expect(getSearchInput()).not.toHaveFocus();
    });
  });

  // -------------------------------------------------------------------------
  // Feature gates + data source
  // -------------------------------------------------------------------------
  // -------------------------------------------------------------------------
  // Query-parser warnings
  // -------------------------------------------------------------------------
  describe('parser warnings', () => {
    it('shows no warning pill in the panel, which the suggest endpoint cannot feed', async () => {
      // /rest/search/suggest returns bestMatch/groups/catalogAvailable only, so a
      // pill here could never render. Warnings surface on the full results page.
      mock.reset();
      mock.onGet(SUGGEST_RE).reply(200, {
        ...FIXTURE_RESPONSE,
        warnings: ['Unknown filter "nope:" ignored'],
      });
      const user = userEvent.setup();
      renderInTheme();

      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');

      expect(screen.queryByTestId('nosc-search-warning-pill')).not.toBeInTheDocument();
    });
  });

  describe('preview shell mount', () => {
    it('renders the omnibar whenever the preview shell mounts it', () => {
      renderInTheme();
      expect(screen.getByTestId('nosc-search-omnibar')).toBeInTheDocument();
    });
  });

  describe('data source', () => {
    it('defaults the suggest fetch to source=local', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.type(getSearchInput(), SEARCH_QUERY);
      await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0));
      const lastUrl = mock.history.get[mock.history.get.length - 1].url ?? '';
      expect(lastUrl).toContain('source=local');
      expect(lastUrl).not.toContain('source=catalog');
    });

    it('keeps the panel open and re-queries source=catalog when the option is picked mid-search', async () => {
      // Regression: the Radix Select renders its options in a portal outside the
      // omnibar container. The outside-click handler must treat a click inside a
      // popper wrapper as "inside", otherwise picking the option closes the panel
      // before the selection registers and the source never switches.
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: true });

      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');

      await user.click(screen.getByLabelText('Search data source'));
      await user.click(await screen.findByRole('option', { name: 'Sonatype Catalog' }));

      await waitFor(() => {
        const lastUrl = mock.history.get[mock.history.get.length - 1]?.url ?? '';
        expect(lastUrl).toContain('source=catalog');
      });
      // The panel must NOT have closed as a side effect of the portaled click.
      expect(getPanelSurface()).toHaveAttribute('data-open', 'true');
    });

    it('explains the reduced tab set once catalog results are on screen', async () => {
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: true });

      await user.click(getSearchInput());
      await user.click(screen.getByLabelText('Search data source'));
      await user.click(await screen.findByRole('option', { name: 'Sonatype Catalog' }));
      await user.type(getSearchInput(), SEARCH_QUERY);

      const hint = await screen.findByTestId('nosc-search-catalog-hint');
      expect(hint).toHaveTextContent(/covers components and vulnerabilities/i);
    });

    it('does not show the catalog hint while searching my scan data', async () => {
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: true });
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');
      expect(screen.queryByTestId('nosc-search-catalog-hint')).not.toBeInTheDocument();
    });

    it('hides tabs and rows for entity types the catalog cannot serve', async () => {
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: true });

      await user.click(getSearchInput());
      await user.click(screen.getByLabelText('Search data source'));
      await user.click(await screen.findByRole('option', { name: 'Sonatype Catalog' }));
      await user.type(getSearchInput(), SEARCH_QUERY);

      await waitFor(() => expect(screen.getByText('log4j-core')).toBeInTheDocument());
      // Catalog serves only Component + Vulnerability.
      expect(screen.queryByText('Webgoat')).not.toBeInTheDocument();
      expect(screen.queryByText('Security-Critical')).not.toBeInTheDocument();
      expect(screen.queryByTestId('nosc-search-panel-tab-APPLICATION')).not.toBeInTheDocument();
      expect(screen.queryByTestId('nosc-search-panel-tab-WAIVER')).not.toBeInTheDocument();
    });

    it('carries source=catalog into the results-page state params', async () => {
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: true });

      await user.click(getSearchInput());
      await user.click(screen.getByLabelText('Search data source'));
      await user.click(await screen.findByRole('option', { name: 'Sonatype Catalog' }));
      await user.type(getSearchInput(), SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');
      await user.keyboard('{Enter}');

      expect(goSpy).toHaveBeenCalledWith('nexusOneSearch', { q: SEARCH_QUERY, source: 'catalog' });
    });

    it('falls back to All when an itemType: token names a type the catalog cannot serve', async () => {
      // buildPanelTabs drops APPLICATION for the catalog source, so the token has no
      // tab to select; the mixed All list must not render under a hidden tab strip
      // pretending the narrowing was applied.
      const user = userEvent.setup();
      renderInTheme({ catalogFederation: true });

      await user.click(getSearchInput());
      await user.click(screen.getByLabelText('Search data source'));
      await user.click(await screen.findByRole('option', { name: 'Sonatype Catalog' }));
      await user.type(getSearchInput(), 'itemType:APPLICATION');

      // The catalog serves components + vulnerabilities, so the All list still has rows
      // and none of them is an application row the unservable token asked for.
      await screen.findByTestId('nosc-search-results-view');
      expect(screen.queryByText('Webgoat')).not.toBeInTheDocument();
      // The strip stays visible: hiding it would advertise a narrowing that was dropped.
      expect(screen.getByTestId('nosc-search-panel-tab-ALL')).toBeInTheDocument();
    });
  });

  describe('filter builder', () => {
    it('shows the filter toggle only once the panel is open', async () => {
      const user = userEvent.setup();
      renderInTheme();
      expect(screen.queryByTestId('nosc-search-filter-toggle')).not.toBeInTheDocument();
      await user.click(screen.getByRole('combobox'));
      expect(screen.getByTestId('nosc-search-filter-toggle')).toBeInTheDocument();
    });

    it('toggle exposes aria-expanded / aria-controls (disclosure semantics only)', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(screen.getByRole('combobox'));
      const toggle = screen.getByTestId('nosc-search-filter-toggle');
      expect(toggle).toHaveAttribute('aria-expanded', 'false');
      expect(toggle).toHaveAttribute('aria-controls', 'nosc-search-filter-bar');
      await user.click(toggle);
      expect(toggle).toHaveAttribute('aria-expanded', 'true');
      // A show/hide control is a disclosure, not a toggle button: aria-expanded
      // alone carries the state, so aria-pressed must not also be set.
      expect(toggle).not.toHaveAttribute('aria-pressed');
    });

    it('toggling shows and hides the category row', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(screen.getByRole('combobox'));
      expect(screen.queryByRole('toolbar', { name: /search filters/i })).not.toBeInTheDocument();
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      expect(screen.getByRole('toolbar', { name: /search filters/i })).toBeInTheDocument();
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      expect(screen.queryByRole('toolbar', { name: /search filters/i })).not.toBeInTheDocument();
    });

    it('collapses the filter bar when the omnibar closes, so it does not reopen expanded', async () => {
      const user = userEvent.setup();
      renderInTheme();
      await user.click(getSearchInput());
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      expect(screen.getByRole('toolbar', { name: /search filters/i })).toBeInTheDocument();

      // Escape on the input dismisses the omnibar; the filter bar must not still be
      // expanded when the next session opens.
      const input = getSearchInput();
      input.focus();
      await user.keyboard('{Escape}');
      expect(screen.queryByRole('toolbar', { name: /search filters/i })).not.toBeInTheDocument();

      // Typing reopens the omnibar. The filter bar must come back collapsed: without
      // resetting filtersOpen on close it would render expanded with no user action.
      await user.type(input, 'log4j');
      expect(screen.getByTestId('nosc-search-filter-toggle')).toHaveAttribute('aria-expanded', 'false');
      expect(screen.queryByRole('toolbar', { name: /search filters/i })).not.toBeInTheDocument();
    });

    it('Type -> Application inserts itemType:APPLICATION into the input and does not re-run', async () => {
      const user = userEvent.setup();
      renderInTheme();
      const input = screen.getByRole('combobox') as HTMLInputElement;
      await user.click(input);
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      await user.click(screen.getByTestId('nosc-search-filter-category-type'));
      await user.click(await screen.findByTestId('nosc-search-filter-leaf-itemType:APPLICATION'));
      await waitFor(() => expect(input).toHaveValue('itemType:APPLICATION'));
      // Deferred: no navigation to the full results page on insert.
      expect(assignMock).not.toHaveBeenCalled();
      expect(goSpy).not.toHaveBeenCalled();
    });

    it('Escape inside a filter category menu returns focus to the search input', async () => {
      // Dismissing the category menu must not drop focus to document.body: the input is
      // the combobox's only Tab stop, so focus has to come back to it.
      const user = userEvent.setup();
      renderInTheme();
      const input = getSearchInput();
      await user.click(input);
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      await user.click(screen.getByTestId('nosc-search-filter-category-type'));
      await screen.findByTestId('nosc-search-filter-leaf-itemType:APPLICATION');

      await user.keyboard('{Escape}');

      await waitFor(() => expect(screen.getByTestId('nosc-search-input')).toHaveFocus());
    });

    it('an incomplete quoted leaf places the caret between the quotes and does not re-run', async () => {
      const user = userEvent.setup();
      renderInTheme();
      const input = screen.getByRole('combobox') as HTMLInputElement;
      await user.click(input);
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      await user.click(screen.getByTestId('nosc-search-filter-category-application'));
      await user.click(await screen.findByTestId('nosc-search-filter-leaf-applicationName:""'));
      await waitFor(() => expect(input).toHaveValue('applicationName:""'));
      await waitFor(() => {
        expect(input.selectionStart).toBe('applicationName:""'.length - 1);
      });
      expect(assignMock).not.toHaveBeenCalled();
    });

    it('an enum leaf flyout inserts field:value', async () => {
      const user = userEvent.setup();
      renderInTheme();
      const input = screen.getByRole('combobox') as HTMLInputElement;
      await user.click(input);
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      await user.click(screen.getByTestId('nosc-search-filter-category-vulnerability'));
      const stage = await screen.findByTestId('nosc-search-filter-leaf-policyEvaluationStage:');
      stage.focus();
      await user.keyboard('{ArrowRight}');
      const value = await screen.findByTestId('nosc-search-filter-value-policyEvaluationStage:build');
      value.focus();
      await user.keyboard('{Enter}');
      await waitFor(() => expect(input).toHaveValue('policyEvaluationStage:build'));
    });

    it('chaining a second filter inserts a single leading space', async () => {
      const user = userEvent.setup();
      renderInTheme();
      const input = screen.getByRole('combobox') as HTMLInputElement;
      await user.click(input);
      await user.type(input, 'log4j');
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      await user.click(screen.getByTestId('nosc-search-filter-category-type'));
      await user.click(await screen.findByTestId('nosc-search-filter-leaf-itemType:COMPONENT'));
      await waitFor(() => expect(input).toHaveValue('log4j itemType:COMPONENT'));
    });

    it('ArrowDown inside an open filter menu does not drive the results listbox', async () => {
      const user = userEvent.setup();
      renderInTheme();
      const input = screen.getByRole('combobox');
      await user.type(input, SEARCH_QUERY);
      await screen.findByTestId('nosc-search-results-view');
      await user.click(screen.getByTestId('nosc-search-filter-toggle'));
      await user.click(screen.getByTestId('nosc-search-filter-category-type'));
      await screen.findByTestId('nosc-search-filter-leaf-itemType:APPLICATION');
      const before = input.getAttribute('aria-activedescendant');
      // Focus is inside the Radix menu popper; the omnibar's arrow handler must bail
      // so the active option does not move from this keypress.
      await user.keyboard('{ArrowDown}');
      expect(input.getAttribute('aria-activedescendant')).toBe(before);
      expect(document.querySelectorAll('[role="option"][aria-selected="true"]')).toHaveLength(
        before ? 1 : 0
      );
    });
  });
});
