/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'TestRoot/SpecUtil';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import { SearchResultsPage } from 'MainRoot/nosc/searchResults/SearchResultsPage';
import { ResultsResponse } from 'MainRoot/nosc/search/searchTypes';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

// The page reads route params via useCurrentStateAndParams and navigates via the
// routerInstance singleton. Mock both so a test can set params and assert the
// exact go() call (which URL param changed) without a real router.
let mockParams: Record<string, unknown> = {};
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: () => ({ params: mockParams }),
}));

const mockGo = jest.fn();
jest.mock('MainRoot/router/routerInstance', () => ({
  __esModule: true,
  default: { stateService: { go: (...args: unknown[]) => mockGo(...args) } },
}));

beforeAll(() => installRadixJsdomShims());

const RESULTS_RE = /\/rest\/search\/results/;

function resultRow(over: Partial<ResultsResponse['results'][number]> = {}) {
  return {
    id: 'c-1',
    type: 'COMPONENT' as const,
    source: 'local' as const,
    title: 'log4j-core',
    subtitle: 'maven · 2.14.1',
    fields: { ecosystem: 'maven', version: '2.14.1' },
    ...over,
  };
}

/** A full ResultsResponse with tabCounts, one component row by default. */
function response(over: Partial<ResultsResponse> = {}): ResultsResponse {
  return {
    tab: 'ALL',
    page: 1,
    pageSize: 25,
    totalEstimate: 3,
    results: [resultRow()],
    nextSearchAfter: null,
    warnings: [],
    catalogAvailable: true,
    tabCounts: {
      ALL: 3,
      APPLICATION: 1,
      COMPONENT: 1,
      VULNERABILITY: 1,
      VIOLATION: 0,
      WAIVER: 0,
    },
    ...over,
  };
}

function renderPage() {
  return render(
    <Theme>
      <SearchResultsPage />
    </Theme>
  );
}

describe('SearchResultsPage (CLM-42453 server pagination + tabCounts + inline filter)', () => {
  let mock: MockAdapter;
  const originalLocation = window.location;
  let assignMock: jest.Mock;

  beforeEach(() => {
    mockParams = { q: 'log4j' };
    mockGo.mockClear();
    mock = new MockAdapter(axios);
    mock.onGet(RESULTS_RE).reply(200, response());
    assignMock = jest.fn();
    delete (window as any).location;
    (window as any).location = { ...originalLocation, assign: assignMock, hash: '' };
  });

  afterEach(() => {
    mock.restore();
    (window as any).location = originalLocation;
  });

  it('fetches the results endpoint with tab + pageSize=25 (server pagination, no full-tenant fetch)', async () => {
    renderPage();
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0));
    const url = mock.history.get[0].url ?? '';
    expect(url).toContain('/rest/search/results');
    expect(url).toContain('tab=ALL');
    expect(url).toContain('pageSize=25');
    // Server pagination: exactly one call, never a fetch-all fan-out.
    expect(mock.history.get).toHaveLength(1);
  });

  it('populates all six tab badges from tabCounts in one call', async () => {
    renderPage();
    expect(await screen.findByTestId('nosc-search-results-list')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-search-tab-all')).toHaveTextContent('3');
    expect(screen.getByTestId('nosc-search-tab-APPLICATION')).toHaveTextContent('1');
    expect(screen.getByTestId('nosc-search-tab-COMPONENT')).toHaveTextContent('1');
    expect(screen.getByTestId('nosc-search-tab-VULNERABILITY')).toHaveTextContent('1');
    expect(screen.getByTestId('nosc-search-tab-VIOLATION')).toHaveTextContent('0');
    expect(screen.getByTestId('nosc-search-tab-WAIVER')).toHaveTextContent('0');
  });

  describe('includeTabCounts (sibling count probe is opt-in)', () => {
    it('ALL tab does not ask for tab counts — its packing pass already returns all six', async () => {
      mockParams = { q: 'log4j' };
      renderPage();
      await screen.findByTestId('nosc-search-results-list');
      expect(mock.history.get[0].url ?? '').not.toContain('includeTabCounts');
      // The badges still populate, from the ALL response's own tabCounts.
      expect(screen.getByTestId('nosc-search-tab-APPLICATION')).toHaveTextContent('1');
      expect(screen.getByTestId('nosc-search-tab-WAIVER')).toHaveTextContent('0');
    });

    it('an entity tab asks for tab counts on page 1 and renders every badge from them', async () => {
      mockParams = { q: 'log4j', tab: 'COMPONENT' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'COMPONENT' }));
      renderPage();
      await screen.findByTestId('nosc-search-results-list');
      expect(mock.history.get[0].url ?? '').toContain('includeTabCounts=true');
      // Sibling badges only exist because the probe ran.
      expect(screen.getByTestId('nosc-search-tab-all')).toHaveTextContent('3');
      expect(screen.getByTestId('nosc-search-tab-APPLICATION')).toHaveTextContent('1');
      expect(screen.getByTestId('nosc-search-tab-VULNERABILITY')).toHaveTextContent('1');
    });

    it('asks once per query and reuses the cached counts across a tab switch', async () => {
      // The probe costs one count-only search per sibling section, so switching tabs
      // within one query must not re-fire it: the badges learned on the first entity tab
      // are still valid for the same (query, source).
      const user = userEvent.setup();
      mockParams = { q: 'log4j', tab: 'COMPONENT' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply((cfg) => {
        const url = cfg.url ?? '';
        return [200, response({ tab: url.includes('tab=APPLICATION') ? 'APPLICATION' : 'COMPONENT' })];
      });
      const { rerender } = renderPage();
      await screen.findByTestId('nosc-search-results-list');
      expect(mock.history.get[0].url ?? '').toContain('includeTabCounts=true');

      await user.click(screen.getByTestId('nosc-search-tab-APPLICATION'));
      mockParams = { q: 'log4j', tab: 'APPLICATION' };
      rerender(
        <Theme>
          <SearchResultsPage />
        </Theme>
      );

      await waitFor(() =>
        expect(mock.history.get.some((r) => (r.url ?? '').includes('tab=APPLICATION'))).toBe(true)
      );
      const applicationRequest = mock.history.get.find((r) => (r.url ?? '').includes('tab=APPLICATION'));
      expect(applicationRequest?.url ?? '').not.toContain('includeTabCounts');
      // Badges survive the switch from the cached counts rather than a second fan-out.
      expect(screen.getByTestId('nosc-search-tab-VULNERABILITY')).toHaveTextContent('1');
    });

    it('re-asks for a new query, whose counts the cache cannot supply', async () => {
      mockParams = { q: 'log4j', tab: 'COMPONENT' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'COMPONENT' }));
      const { rerender } = renderPage();
      await screen.findByTestId('nosc-search-results-list');

      mockParams = { q: 'guava', tab: 'COMPONENT' };
      rerender(
        <Theme>
          <SearchResultsPage />
        </Theme>
      );

      await waitFor(() => expect(mock.history.get.some((r) => (r.url ?? '').includes('q=guava'))).toBe(true));
      const guavaRequest = mock.history.get.find((r) => (r.url ?? '').includes('q=guava'));
      expect(guavaRequest?.url ?? '').toContain('includeTabCounts=true');
    });

    it('does not ask on page 2, where the backend serves the active tab total anyway', async () => {
      const user = userEvent.setup();
      mockParams = { q: 'log4j', tab: 'COMPONENT' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({
        tab: 'COMPONENT',
        results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}` })),
        nextSearchAfter: 'CURSOR_P2',
      }));
      const { rerender } = renderPage();
      await user.click(await screen.findByTestId('nosc-search-results-next'));

      mockParams = { q: 'log4j', tab: 'COMPONENT', page: '2' };
      rerender(
        <Theme>
          <SearchResultsPage />
        </Theme>
      );

      await waitFor(() =>
        expect(mock.history.get.some((r) => (r.url ?? '').includes('searchAfter=CURSOR_P2'))).toBe(true)
      );
      const pageTwo = mock.history.get.find((r) => (r.url ?? '').includes('searchAfter=CURSOR_P2'));
      expect(pageTwo?.url ?? '').not.toContain('includeTabCounts');
    });
  });

  it('falls back to the active tab totalEstimate when tabCounts is absent (older backend)', async () => {
    mock.onGet(RESULTS_RE).reply(200, response({ tabCounts: undefined, totalEstimate: 42 }));
    renderPage();
    expect(await screen.findByTestId('nosc-search-results-list')).toBeInTheDocument();
    // Active ("all") tab shows the total; uncounted tabs show their label with
    // no numeric badge (suppressed rather than a misleading 0).
    expect(screen.getByTestId('nosc-search-tab-all')).toHaveTextContent('42');
    const componentTab = screen.getByTestId('nosc-search-tab-COMPONENT');
    expect(componentTab).toHaveTextContent('Components');
    expect(componentTab).not.toHaveTextContent('0');
  });

  it('advances to page 2 via Next (page param), resetting to page 1 on query change', async () => {
    const user = userEvent.setup();
    // The next-page cursor is what enables Next.
    mock
      .onGet(RESULTS_RE)
      .reply(200, response({
        results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}` })),
        nextSearchAfter: 'cursor-1',
      }));
    renderPage();
    const next = await screen.findByTestId('nosc-search-results-next');
    await user.click(next);
    expect(mockGo).toHaveBeenCalledWith('nexusOneSearch', expect.objectContaining({ page: '2' }), undefined);

    // Switching tab resets to page 1.
    mockGo.mockClear();
    await user.click(screen.getByTestId('nosc-search-tab-COMPONENT'));
    expect(mockGo).toHaveBeenCalledWith(
      'nexusOneSearch',
      expect.objectContaining({ tab: 'COMPONENT', page: undefined }),
      undefined
    );
  });

  it('offers no Next on an exactly-full last page (no cursor means no next page)', async () => {
    // Both index backends over-fetch by one row before minting nextSearchAfter, so it
    // is exact: absent on the last page even when that page is exactly pageSize rows.
    // Treating a full page as "more available" used to enable Next into a page with no
    // cursor, which the page-reset guard then bounced back to page 1 unexplained.
    mock
      .onGet(RESULTS_RE)
      .reply(200, response({
        results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}` })),
        nextSearchAfter: null,
      }));
    renderPage();
    // Wait for the full page of rows, so absence of Next is a settled state.
    await screen.findByTestId('nosc-search-results-list');
    expect(await screen.findAllByTestId('nosc-search-result-card-COMPONENT')).toHaveLength(25);
    expect(screen.queryByTestId('nosc-search-results-next')).not.toBeInTheDocument();
  });

  it('disables Previous on page 1 and enables it past page 1 (keyboard-operable, labelled)', async () => {
    const user = userEvent.setup();
    mock
      .onGet(RESULTS_RE)
      .reply(200, response({
        results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}` })),
        nextSearchAfter: 'cursor-1',
      }));
    const { rerender } = renderPage();
    const prevOnPage1 = await screen.findByTestId('nosc-search-results-prev');
    expect(prevOnPage1).toBeDisabled();

    // Walk forward through Next so page 2 has a cached cursor (a bare ?page=2
    // deep link is redirected back to page 1 — covered separately below).
    await user.click(screen.getByTestId('nosc-search-results-next'));
    mockParams = { q: 'log4j', page: '2' };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );

    const prev = await screen.findByTestId('nosc-search-results-prev');
    expect(prev).toHaveAttribute('aria-label', 'Previous page');
    expect(prev).not.toBeDisabled();
    expect(screen.getByTestId('nosc-search-results-next')).toHaveAttribute('aria-label', 'Next page');
  });

  it('deep-links page > 1 with no cached cursor: redirects to page 1 instead of mislabelling page-1 rows', async () => {
    // The cursor cache is in-memory, so a bookmarked/refreshed ?page=2 has no
    // cursor for page 2. The cursor-only backend read path would return page 1's
    // rows under a "Page 2" label, so the page snaps back to page 1 (replace) and
    // issues no request under the bogus page.
    mockParams = { q: 'log4j', page: '2' };
    renderPage();
    await waitFor(() =>
      expect(mockGo).toHaveBeenCalledWith(
        'nexusOneSearch',
        expect.objectContaining({ q: 'log4j', page: undefined }),
        { location: 'replace' }
      )
    );
    expect(mock.history.get.filter((r) => (r.url ?? '').includes('page=2'))).toHaveLength(0);
  });

  it('deep-links a source-hidden tab back to All (keeps the tablist keyboard-reachable)', async () => {
    // catalog hides APPLICATION. Left as-is, no rendered tab matches activeTab, so
    // every tab gets tabIndex=-1 (unreachable) and the tabpanel's aria-labelledby
    // dangles. activeTab is coerced to 'all' instead.
    mockParams = { q: 'log4j', tab: 'APPLICATION', source: 'catalog' };
    renderPage();
    const allTab = await screen.findByTestId('nosc-search-tab-all');
    expect(allTab).toHaveAttribute('aria-selected', 'true');
    expect(allTab).toHaveAttribute('tabindex', '0');
    expect(screen.queryByTestId('nosc-search-tab-APPLICATION')).not.toBeInTheDocument();

    // aria-labelledby resolves to a rendered tab.
    const panel = screen.getByRole('tabpanel');
    const labelId = panel.getAttribute('aria-labelledby') ?? '';
    expect(document.getElementById(labelId)).toBe(allTab);
  });

  it('hides the IQ/Guide source toggle in catalog mode (the IQ option cannot match)', async () => {
    mockParams = { q: 'log4j', source: 'catalog' };
    renderPage();
    await screen.findByTestId('nosc-search-results-list');
    expect(screen.queryByTestId('nosc-search-results-source-filter')).not.toBeInTheDocument();
  });

  it('keeps the pagination row mounted while a fetch is in flight (buttons disabled, no layout jump)', async () => {
    const user = userEvent.setup();
    mock
      .onGet(RESULTS_RE)
      .reply(200, response({
        results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}` })),
        nextSearchAfter: 'cursor-1',
      }));
    renderPage();
    const next = await screen.findByTestId('nosc-search-results-next');

    // Re-running the query puts a fetch in flight; the row must not unmount.
    await user.clear(screen.getByTestId('nosc-search-results-input'));
    await user.type(screen.getByTestId('nosc-search-results-input'), 'guava{Enter}');
    expect(screen.getByTestId('nosc-search-results-pagination')).toBeInTheDocument();
    expect(next).toBeInTheDocument();
  });

  it('arrow-key tab navigation replaces the history entry instead of pushing one per keypress', async () => {
    const user = userEvent.setup();
    renderPage();
    const allTab = await screen.findByTestId('nosc-search-tab-all');
    allTab.focus();
    await user.keyboard('{ArrowRight}');
    // Activation follows focus for arrow keys, so the transition replaces rather
    // than pushing — arrowing across the strip must not fill the back stack.
    expect(mockGo).toHaveBeenCalledWith('nexusOneSearch', expect.any(Object), { location: 'replace' });

    // A click is a deliberate navigation and still pushes.
    mockGo.mockClear();
    await user.click(screen.getByTestId('nosc-search-tab-COMPONENT'));
    expect(mockGo).toHaveBeenCalledWith('nexusOneSearch', expect.any(Object), undefined);
  });

  it('BUG A: Next threads the prior page nextSearchAfter cursor (shallow page 2), and page 2 shows the next rows', async () => {
    // Page 1 returns a full page with a cursor to page 2; page 2 fetch (with that
    // cursor) returns genuinely different rows. Cursor state lives in a useRef that
    // survives the in-place param change, mirroring the real router.
    const page1 = response({
      results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `p1-${i}`, title: `page1-row-${i}` })),
      nextSearchAfter: 'CURSOR_P2',
    });
    const page2 = response({
      page: 2,
      results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `p2-${i}`, title: `page2-row-${i}` })),
      nextSearchAfter: 'CURSOR_P3',
    });
    mock.reset();
    mock.onGet(RESULTS_RE).reply((cfg) => [200, (cfg.url ?? '').includes('searchAfter=CURSOR_P2') ? page2 : page1]);

    mockParams = { q: 'log4j' };
    const { rerender } = renderPage();
    expect(await screen.findByText('page1-row-0')).toBeInTheDocument();

    // Page 1 must NOT carry a cursor.
    expect(mock.history.get[0].url ?? '').not.toContain('searchAfter=');

    // Simulate the router advancing to page 2 in place (same mounted component).
    mockParams = { q: 'log4j', page: '2' };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );

    // The page-2 fetch threads page-1's nextSearchAfter cursor...
    await waitFor(() =>
      expect(mock.history.get.some((r) => (r.url ?? '').includes('searchAfter=CURSOR_P2'))).toBe(true)
    );
    // ...and page 2 renders the genuinely next rows (not page 1's).
    expect(await screen.findByText('page2-row-0')).toBeInTheDocument();
    expect(screen.queryByText('page1-row-0')).not.toBeInTheDocument();
  });

  it('BUG A: cursor cache resets on query change (a new query sends no stale cursor)', async () => {
    const withCursor = response({
      results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `q1-${i}` })),
      nextSearchAfter: 'CURSOR_STALE',
    });
    mock.reset();
    mock.onGet(RESULTS_RE).reply(200, withCursor);

    mockParams = { q: 'log4j' };
    const { rerender } = renderPage();
    await screen.findByTestId('nosc-search-results-list');

    // New query, page unset → the query-change effect clears the cursor map, so
    // the fetch for the new query carries no searchAfter.
    mockParams = { q: 'guava' };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );
    await waitFor(() => expect(mock.history.get.some((r) => (r.url ?? '').includes('q=guava'))).toBe(true));
    const guavaReq = mock.history.get.find((r) => (r.url ?? '').includes('q=guava'));
    expect(guavaReq?.url ?? '').not.toContain('searchAfter=');
  });

  it('clears the fallback tab-count badges when the data source changes', async () => {
    // Counts are a function of (query, source): the local index and the shared catalog
    // are different corpora. Switching source without editing the query must not leave
    // catalog-derived Component/Vulnerability counts rendering as local badges.
    mock.reset();
    mock.onGet(RESULTS_RE).reply((cfg) => {
      const url = cfg.url ?? '';
      if (url.includes('source=catalog')) {
        // The catalog answers only the shared-corpus tabs, and an older backend omits
        // tabCounts entirely, so the active tab's totalEstimate is all that is learned.
        return [200, response({ tabCounts: undefined, totalEstimate: 777 })];
      }
      return [200, response({ tabCounts: undefined, totalEstimate: 42 })];
    });

    mockParams = { q: 'log4j', tab: 'COMPONENT', source: 'catalog' };
    const { rerender } = renderPage();
    await screen.findByTestId('nosc-search-results-list');
    await waitFor(() => expect(screen.getByTestId('nosc-search-tab-COMPONENT')).toHaveTextContent('777'));

    // Same query, source flipped back to the local index. The catalog-derived Component
    // count must not survive into the local badges.
    mockParams = { q: 'log4j', tab: 'all', source: undefined };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );

    await waitFor(() => expect(screen.getByTestId('nosc-search-tab-all')).toHaveTextContent('42'));
    const componentTab = screen.getByTestId('nosc-search-tab-COMPONENT');
    expect(componentTab).toHaveTextContent('Components');
    expect(componentTab).not.toHaveTextContent('777');
  });

  it('inline filter bar: a complete leaf re-runs the search (pushes a new URL)', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByTestId('nosc-search-results-list');

    const toggle = screen.getByTestId('nosc-search-results-filter-toggle');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');

    // Open the Type category, choose Application (a complete leaf → commit).
    await user.click(screen.getByTestId('nosc-search-filter-category-type'));
    const leaf = await screen.findByTestId('nosc-search-filter-leaf-itemType:APPLICATION');
    await user.click(leaf);

    await waitFor(() =>
      expect(mockGo).toHaveBeenCalledWith(
        'nexusOneSearch',
        expect.objectContaining({ q: expect.stringContaining('itemType:APPLICATION'), page: undefined }),
        undefined
      )
    );
  });

  it('inline filter bar: an incomplete leaf defers (does not re-run the search)', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByTestId('nosc-search-results-list');

    await user.click(screen.getByTestId('nosc-search-results-filter-toggle'));
    // "Application" category → "Public ID" leaf ends with ':' (incomplete → defer).
    await user.click(screen.getByTestId('nosc-search-filter-category-application'));
    const leaf = await screen.findByTestId('nosc-search-filter-leaf-applicationPublicId:');
    await user.click(leaf);

    // The input got the syntax but no navigation (search not re-run).
    const input = screen.getByTestId('nosc-search-results-input') as HTMLInputElement;
    await waitFor(() => expect(input.value).toContain('applicationPublicId:'));
    expect(mockGo).not.toHaveBeenCalled();
  });

  it('hides the IQ-only tabs in catalog mode (source=catalog)', async () => {
    mockParams = { q: 'log4j', source: 'catalog' };
    renderPage();
    expect(await screen.findByTestId('nosc-search-tab-all')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-search-tab-COMPONENT')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-search-tab-VULNERABILITY')).toBeInTheDocument();
    expect(screen.queryByTestId('nosc-search-tab-APPLICATION')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nosc-search-tab-VIOLATION')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nosc-search-tab-WAIVER')).not.toBeInTheDocument();
    // And the request targets the catalog source.
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0));
    expect(mock.history.get[0].url ?? '').toContain('source=catalog');
  });

  it('preserves source=catalog across tab changes', async () => {
    const user = userEvent.setup();
    mockParams = { q: 'log4j', source: 'catalog' };
    renderPage();
    await user.click(await screen.findByTestId('nosc-search-tab-COMPONENT'));
    expect(mockGo).toHaveBeenCalledWith(
      'nexusOneSearch',
      expect.objectContaining({ source: 'catalog' }),
      undefined
    );
  });

  it('renders a WarningPill from response.warnings above the tabs', async () => {
    mock.onGet(RESULTS_RE).reply(200, response({ warnings: ['Unknown filter "foo" ignored'] }));
    renderPage();
    const pill = await screen.findByTestId('nosc-search-warning-pill');
    expect(pill).toHaveTextContent(/unknown filter/i);
    expect(within(pill).getByRole('status')).toHaveAttribute('aria-live', 'polite');
  });

  it('filters the All-tab rows by source via the IQ/Guide toggle', async () => {
    const user = userEvent.setup();
    mock.onGet(RESULTS_RE).reply(
      200,
      response({
        results: [
          resultRow({ id: 'local-1', title: 'iq-comp', source: 'local' }),
          resultRow({ id: 'cat-1', title: 'guide-comp', source: 'catalog' }),
        ],
      })
    );
    renderPage();
    await screen.findByTestId('nosc-search-results-list');
    expect(screen.getByText('iq-comp')).toBeInTheDocument();
    expect(screen.getByText('guide-comp')).toBeInTheDocument();

    // Toggle to Guide (catalog) — only the catalog row remains.
    await user.click(screen.getByTestId('nosc-search-source-catalog'));
    await waitFor(() => expect(screen.queryByText('iq-comp')).not.toBeInTheDocument());
    expect(screen.getByText('guide-comp')).toBeInTheDocument();
  });

  it('resets the IQ/Guide toggle on tab change so it cannot narrow rows invisibly', async () => {
    const user = userEvent.setup();
    const { rerender } = renderPage();
    await screen.findByTestId('nosc-search-results-list');
    await user.click(screen.getByTestId('nosc-search-source-catalog'));
    expect(screen.getByTestId('nosc-search-source-catalog')).toHaveAttribute('data-state', 'on');

    // Leave the All tab (the toggle is hidden there) and come back.
    mockParams = { q: 'log4j', tab: 'COMPONENT' };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );
    mockParams = { q: 'log4j' };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );

    await screen.findByTestId('nosc-search-results-source-filter');
    expect(screen.getByTestId('nosc-search-source-all')).toHaveAttribute('data-state', 'on');
  });

  it('resets the IQ/Guide toggle when the source changes so it cannot narrow rows invisibly', async () => {
    // `source` is a dynamic URL param, so switching corpus re-renders this page in
    // place. A toggle left on the previous corpus would keep filtering the new rows
    // while its control is hidden in catalog mode.
    const user = userEvent.setup();
    const { rerender } = renderPage();
    await screen.findByTestId('nosc-search-results-list');
    await user.click(screen.getByTestId('nosc-search-source-local'));
    expect(screen.getByTestId('nosc-search-source-local')).toHaveAttribute('data-state', 'on');

    mockParams = { q: 'log4j', source: 'catalog' };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );
    // Back to local: the toggle is visible again and must have reset to 'all'.
    mockParams = { q: 'log4j' };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );

    await screen.findByTestId('nosc-search-results-source-filter');
    expect(screen.getByTestId('nosc-search-source-all')).toHaveAttribute('data-state', 'on');
  });

  it('keeps Previous reachable on page 2 when a client filter empties the visible rows', async () => {
    const user = userEvent.setup();
    // Walk to page 2 through Next so page 2 holds a cursor, then confirm a client
    // filter emptying the rows does not strip the pagination row.
    mock
      .onGet(RESULTS_RE)
      .reply(200, response({
        results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}`, source: 'local' })),
        nextSearchAfter: 'cursor-1',
      }));
    const { rerender } = renderPage();
    await screen.findByTestId('nosc-search-results-list');
    await user.click(screen.getByTestId('nosc-search-results-next'));
    mockParams = { q: 'log4j', page: '2' };
    rerender(
      <Theme>
        <SearchResultsPage />
      </Theme>
    );
    await screen.findByTestId('nosc-search-results-list');
    expect(screen.getByTestId('nosc-search-results-prev')).not.toBeDisabled();

    // Filter to Guide (catalog) — every visible row is local, so the page empties,
    // but the pagination must remain so the user can page back off the empty page.
    await user.click(screen.getByTestId('nosc-search-source-catalog'));
    await waitFor(() => expect(screen.getByTestId('nosc-search-results-pagination')).toBeInTheDocument());
    expect(screen.getByTestId('nosc-search-results-prev')).not.toBeDisabled();
  });

  it('exposes a live region announcing the page number and result count for assistive tech', async () => {
    renderPage();
    const live = await screen.findByTestId('nosc-search-results-live');
    expect(live).toHaveAttribute('aria-live', 'polite');
    // The visible "Page N" text is aria-hidden, so the page number has to be in
    // the announcement or a screen-reader user never learns which page they are on.
    await waitFor(() => expect(live).toHaveTextContent(/page 1, 1 results on this page/i));
  });

  it('BUG B: wires the tabs to the results tabpanel (aria-controls resolves)', async () => {
    renderPage();
    const panel = await screen.findByRole('tabpanel');
    expect(panel).toHaveAttribute('id', 'nosc-search-results-panel');
    for (const tab of screen.getAllByRole('tab')) {
      expect(tab).toHaveAttribute('aria-controls', 'nosc-search-results-panel');
    }
  });

  describe('per-tab facet rail (CLM-42453)', () => {
    const VIOLATION_FACETS = {
      states: [
        { value: 'OPEN', displayName: 'Open', count: 8940 },
        { value: 'WAIVED', displayName: 'Waived', count: 583 },
      ],
      policyTypes: [{ value: 'SECURITY', displayName: 'Security', count: 6290 }],
    };

    it('ALL tab renders NO facet rail and does NOT request facets', async () => {
      mockParams = { q: 'log4j' };
      renderPage();
      await screen.findByTestId('nosc-search-results-list');
      expect(screen.queryByRole('button', { name: /reset filters/i })).not.toBeInTheDocument();
      const url = mock.history.get[0].url ?? '';
      expect(url).not.toContain('includeFacets');
    });

    it('an entity tab requests includeFacets=true and renders its facet sections', async () => {
      mockParams = { q: 'jackson', tab: 'VIOLATION' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'VIOLATION', facets: VIOLATION_FACETS }));
      renderPage();
      await screen.findByTestId('nosc-search-results-list');
      const url = mock.history.get[0].url ?? '';
      expect(url).toContain('includeFacets=true');
      expect(screen.getByRole('group', { name: 'Violation State' })).toBeInTheDocument();
      expect(screen.getByRole('group', { name: 'Policy Types' })).toBeInTheDocument();
      expect(screen.getByText('8,940')).toBeInTheDocument();
    });

    it('selecting a facet checkbox appends the filter to the query and re-navigates to page 1', async () => {
      const user = userEvent.setup();
      mockParams = { q: 'jackson', tab: 'VIOLATION' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'VIOLATION', facets: VIOLATION_FACETS }));
      renderPage();
      await screen.findByRole('checkbox', { name: 'Security' });
      await user.click(screen.getByRole('checkbox', { name: 'Security' }));
      expect(mockGo).toHaveBeenCalledWith(
        'nexusOneSearch',
        expect.objectContaining({ q: 'jackson policyViolationThreatCategory:SECURITY', page: undefined }),
        undefined
      );
    });

    it('the Threat Level slider commits the active tab\'s range predicate into the query', async () => {
      mockParams = { q: 'jackson', tab: 'VIOLATION' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'VIOLATION', facets: VIOLATION_FACETS }));
      renderPage();
      const group = await screen.findByRole('group', { name: 'Policy Threat Level' });
      // Radix Slider renders a <span role="slider"> thumb; drive it via keyboard.
      const thumbs = within(group).getAllByRole('slider');
      thumbs[0].focus();
      await userEvent.keyboard('{ArrowRight}');
      await waitFor(() =>
        expect(mockGo).toHaveBeenCalledWith(
          'nexusOneSearch',
          expect.objectContaining({ q: expect.stringContaining('policyViolationThreatLevel:[') }),
          undefined
        )
      );
    });

    it('Reset filters keeps free text and drops predicates whole (quoted and range)', async () => {
      const user = userEvent.setup();
      // Splitting the query on raw whitespace shatters these two predicates,
      // leaving `Organization"` and `TO 7]` behind as garbage free text.
      mockParams = {
        q: 'jackson organizationName:"Sandbox Organization" policyViolationThreatLevel:[3 TO 7]',
        tab: 'VIOLATION',
      };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'VIOLATION', facets: VIOLATION_FACETS }));
      renderPage();
      await user.click(await screen.findByRole('button', { name: /reset filters/i }));
      expect(mockGo).toHaveBeenCalledWith(
        'nexusOneSearch',
        expect.objectContaining({ q: 'jackson', page: undefined }),
        undefined
      );
    });

    it('disables Reset on page 1 with a free-text-only query (nothing would change)', async () => {
      mockParams = { q: 'jackson', tab: 'VIOLATION' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'VIOLATION', facets: VIOLATION_FACETS }));
      renderPage();
      expect(await screen.findByRole('button', { name: /reset filters/i })).toBeDisabled();
    });

    it('enables Reset for a quoted or bracketed predicate', async () => {
      mockParams = { q: 'jackson organizationName:"Sandbox Organization"', tab: 'VIOLATION' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'VIOLATION', facets: VIOLATION_FACETS }));
      renderPage();
      expect(await screen.findByRole('button', { name: /reset filters/i })).not.toBeDisabled();
    });

    it('keeps the facet rail on page 2, where the backend omits the unchanged facet map', async () => {
      const user = userEvent.setup();
      mock.reset();
      // Page 1 carries the facets; page 2 omits them because the map is identical on every page.
      mock
        .onGet(RESULTS_RE)
        .replyOnce(
          200,
          response({
            tab: 'VIOLATION',
            facets: VIOLATION_FACETS,
            results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}` })),
            nextSearchAfter: 'cursor-1',
          })
        )
        .onGet(RESULTS_RE)
        .reply(
          200,
          response({
            tab: 'VIOLATION',
            results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `p2-${i}` })),
            nextSearchAfter: null,
          })
        );
      mockParams = { q: 'jackson', tab: 'VIOLATION' };
      const { rerender } = renderPage();
      await screen.findByRole('group', { name: 'Policy Types' });

      await user.click(screen.getByTestId('nosc-search-results-next'));
      mockParams = { q: 'jackson', tab: 'VIOLATION', page: '2' };
      rerender(
        <Theme>
          <SearchResultsPage />
        </Theme>
      );

      // The rail and its buckets survive the page turn rather than emptying.
      expect(await screen.findByRole('group', { name: 'Policy Types' })).toBeInTheDocument();
      expect(screen.getByRole('group', { name: 'Violation State' })).toBeInTheDocument();
      expect(screen.getByRole('checkbox', { name: 'Security' })).toBeInTheDocument();
    });

    it('enables Reset past page 1 even with no predicates, since it returns to page 1', async () => {
      const user = userEvent.setup();
      mock.reset();
      mock.onGet(RESULTS_RE).reply(
        200,
        response({
          tab: 'VIOLATION',
          facets: VIOLATION_FACETS,
          results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}` })),
          nextSearchAfter: 'cursor-1',
        })
      );
      mockParams = { q: 'jackson', tab: 'VIOLATION' };
      const { rerender } = renderPage();
      await screen.findByTestId('nosc-search-results-list');
      // Walk to page 2 through Next so the cursor is cached (a bare ?page=2 redirects).
      await user.click(screen.getByTestId('nosc-search-results-next'));
      mockParams = { q: 'jackson', tab: 'VIOLATION', page: '2' };
      rerender(
        <Theme>
          <SearchResultsPage />
        </Theme>
      );
      expect(await screen.findByRole('button', { name: /reset filters/i })).not.toBeDisabled();
    });

    it('names the page in the hit summary rather than mixing page and total scopes', async () => {
      // Rows are server-paginated, so the page row count and the total estimate are
      // different scopes: "Showing 25 of 10,000+" reads as a subset of the total.
      mock.reset();
      mock.onGet(RESULTS_RE).reply(
        200,
        response({
          totalEstimate: 10000,
          results: Array.from({ length: 25 }, (_, i) => resultRow({ id: `c-${i}` })),
          nextSearchAfter: 'cursor-1',
        })
      );
      renderPage();
      const summary = await screen.findByTestId('nosc-search-results-hit-summary');
      expect(summary).toHaveTextContent('page 1');
      expect(summary).toHaveTextContent('25 shown');
      expect(summary.textContent).not.toMatch(/25 of/);
    });

    it('qualifies the count on a single page when a client filter hid rows', async () => {
      const user = userEvent.setup();
      mockParams = { q: 'log4j' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(
        200,
        response({
          totalEstimate: 2,
          results: [
            resultRow({ id: 'local-1', title: 'iq-comp', source: 'local' }),
            resultRow({ id: 'cat-1', title: 'guide-comp', source: 'catalog' }),
          ],
        })
      );
      renderPage();
      await screen.findByTestId('nosc-search-results-list');
      // Single page, so no pagination clause -- but the source filter hides a row, and
      // the total must not read as the number of rows on screen.
      await user.click(screen.getByTestId('nosc-search-source-catalog'));
      await waitFor(() =>
        expect(screen.getByTestId('nosc-search-results-hit-summary')).toHaveTextContent('2 matches (1 shown)')
      );
    });

    it('reports only the total when the whole result set fits one page', async () => {
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ totalEstimate: 3, results: [resultRow()] }));
      renderPage();
      const summary = await screen.findByTestId('nosc-search-results-hit-summary');
      expect(summary).toHaveTextContent('3 matches');
      expect(summary.textContent).not.toMatch(/page/i);
    });

    it('falls back to a full-width layout when no facet key is renderable', async () => {
      // A facet key with an empty bucket list must not reserve a rail column that
      // renders nothing but the Reset button.
      mockParams = { q: 'jackson', tab: 'VULNERABILITY' };
      mock.reset();
      mock.onGet(RESULTS_RE).reply(200, response({ tab: 'VULNERABILITY', facets: { organizations: [] } }));
      renderPage();
      await screen.findByTestId('nosc-search-results-list');
      expect(screen.queryByRole('button', { name: /reset filters/i })).not.toBeInTheDocument();
    });
  });
});
