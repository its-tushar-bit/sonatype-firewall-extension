/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import { act, renderHook, waitFor } from '@testing-library/react';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import { useGlobalSearch } from 'MainRoot/nosc/search/useGlobalSearch';
import { SuggestResponse, ResultsResponse } from 'MainRoot/nosc/search/searchTypes';

/**
 * CLM-42453: behavioral tests for the single-endpoint global-search hook.
 *
 * Covers:
 *   - typeahead → one GET /rest/search/suggest (not a 7-way fan-out)
 *   - full → one GET /rest/search/results with tab + source plumbed
 *   - source param threaded into the URL
 *   - 200ms debounce
 *   - AbortController cancels a stale request when the query changes
 *   - SuggestResponse → best match + groups mapping
 *   - ResultsResponse → results + totalEstimate + isExactTotal mapping
 *   - catalog degrade (catalogAvailable:false) is NOT an error
 */

const SUGGEST_RE = /\/rest\/search\/suggest/;
const RESULTS_RE = /\/rest\/search\/results/;

const SUGGEST_OK: SuggestResponse = {
  bestMatch: {
    id: 'CVE-2021-44228',
    type: 'VULNERABILITY',
    source: 'local',
    title: 'CVE-2021-44228',
    subtitle: 'Log4Shell',
  },
  groups: [
    { type: 'VULNERABILITY', source: 'local', results: [] },
    {
      type: 'COMPONENT',
      source: 'local',
      results: [
        { id: 'c-1', type: 'COMPONENT', source: 'local', title: 'log4j-core', subtitle: 'maven' },
      ],
    },
    { type: 'APPLICATION', source: 'local', results: [] },
    { type: 'VIOLATION', source: 'local', results: [] },
    { type: 'WAIVER', source: 'local', results: [] },
  ],
};

const RESULTS_OK: ResultsResponse = {
  tab: 'ALL',
  page: 1,
  pageSize: 25,
  totalEstimate: 3,
  results: [
    { id: 'app-1', type: 'APPLICATION', source: 'local', title: 'Webgoat', subtitle: 'Engineering' },
    { id: 'c-1', type: 'COMPONENT', source: 'local', title: 'log4j-core', fields: { ecosystem: 'maven' } },
  ],
  nextSearchAfter: null,
  warnings: [],
  catalogAvailable: true,
};

describe('useGlobalSearch — typeahead (suggest)', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(axios);
  });
  afterEach(() => mock.restore());

  it('returns empty state and makes no request for a query shorter than 2 chars', () => {
    mock.onGet(SUGGEST_RE).reply(200, SUGGEST_OK);
    const { result } = renderHook(() => useGlobalSearch('a'));
    expect(result.current.results).toHaveLength(0);
    expect(result.current.loading).toBe(false);
    expect(mock.history.get).toHaveLength(0);
  });

  it('typeahead never produces warnings (the suggest response carries none)', async () => {
    // SuggestResponse has no `warnings` field, so a typeahead caller has nothing to
    // surface -- the omnibar therefore renders no warning pill.
    mock.onGet(SUGGEST_RE).reply(200, SUGGEST_OK);
    const { result } = renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0), { timeout: 2000 });
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.warnings).toEqual([]);
  });

  it('issues exactly ONE request to /rest/search/suggest (not a per-entity fan-out)', async () => {
    mock.onGet(SUGGEST_RE).reply(200, SUGGEST_OK);
    renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(mock.history.get).toHaveLength(1);
    const url = mock.history.get[0].url ?? '';
    expect(url).toContain('/rest/search/suggest');
    expect(url).toContain('q=log4j');
  });

  it('threads the source param into the suggest URL', async () => {
    mock.onGet(SUGGEST_RE).reply(200, SUGGEST_OK);
    renderHook(() => useGlobalSearch('log4j', { source: 'catalog' }));
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(mock.history.get[0].url ?? '').toContain('source=catalog');
  });

  it('maps SuggestResponse to bestMatch + grouped rows + flat results', async () => {
    mock.onGet(SUGGEST_RE).reply(200, SUGGEST_OK);
    const { result } = renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(result.current.bestMatch).not.toBeNull(), { timeout: 2000 });
    expect(result.current.bestMatch?.id).toBe('CVE-2021-44228');
    // Empty groups are preserved (backend always sends all five).
    expect(result.current.groups).toHaveLength(5);
    const component = result.current.groups.find((g) => g.type === 'COMPONENT');
    expect(component?.rows).toHaveLength(1);
    // Flat list = best match + all group rows.
    expect(result.current.results).toHaveLength(2);
  });

  it('treats catalogAvailable:false as graceful degrade, not an error', async () => {
    mock.onGet(SUGGEST_RE).reply(200, { ...SUGGEST_OK, catalogAvailable: false });
    const { result } = renderHook(() => useGlobalSearch('log4j', { source: 'catalog' }));
    await waitFor(() => expect(result.current.bestMatch).not.toBeNull(), { timeout: 2000 });
    expect(result.current.loadError).toBeNull();
    expect(result.current.catalogAvailable).toBe(false);
  });

  it('leaves catalogAvailable undefined when the field is absent (catalog not consulted)', async () => {
    // SUGGEST_OK has no catalogAvailable field; the tri-state stays undefined, not false.
    mock.onGet(SUGGEST_RE).reply(200, SUGGEST_OK);
    const { result } = renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(result.current.bestMatch).not.toBeNull(), { timeout: 2000 });
    expect(result.current.catalogAvailable).toBeUndefined();
  });

  it('surfaces a loadError on a 5xx', async () => {
    mock.onGet(SUGGEST_RE).reply(500, 'boom');
    const { result } = renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(result.current.loadError).not.toBeNull(), { timeout: 2000 });
    expect(result.current.results).toHaveLength(0);
  });

  it('maps a 5xx to user-facing copy and logs the raw Axios message', async () => {
    // Axios sets error.message to "Request failed with status code 500", which is HTTP
    // plumbing the user cannot act on; it belongs in the console, not on screen.
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    mock.onGet(SUGGEST_RE).reply(500, 'boom');
    const { result } = renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(result.current.loadError).not.toBeNull(), { timeout: 2000 });
    expect(result.current.loadError).toBe('Search is unavailable. Try again in a moment.');
    expect(result.current.loadError).not.toContain('status code');
    expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('status code 500'));
    consoleError.mockRestore();
  });

  it('maps a 403 to a permission message rather than the raw status text', async () => {
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    mock.onGet(SUGGEST_RE).reply(403, 'forbidden');
    const { result } = renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(result.current.loadError).not.toBeNull(), { timeout: 2000 });
    expect(result.current.loadError).toBe('You do not have permission to search these results.');
    expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('HTTP 403'));
    consoleError.mockRestore();
  });

  it('maps a network failure to the unavailable message', async () => {
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    mock.onGet(SUGGEST_RE).networkError();
    const { result } = renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(result.current.loadError).not.toBeNull(), { timeout: 2000 });
    expect(result.current.loadError).toBe('Search is unavailable. Try again in a moment.');
    consoleError.mockRestore();
  });

  it('debounces ~200ms before firing the request', async () => {
    jest.useFakeTimers();
    mock.onGet(SUGGEST_RE).reply(200, SUGGEST_OK);
    renderHook(() => useGlobalSearch('log4j'));
    // Just before the debounce window elapses, no request yet.
    act(() => {
      jest.advanceTimersByTime(150);
    });
    expect(mock.history.get).toHaveLength(0);
    act(() => {
      jest.advanceTimersByTime(60);
    });
    jest.useRealTimers();
    await waitFor(() => expect(mock.history.get.length).toBe(1), { timeout: 2000 });
  });

  it('aborts the stale request when the query changes (AbortController)', async () => {
    mock.onGet(SUGGEST_RE).reply(200, SUGGEST_OK);
    const { result, rerender } = renderHook(({ q }) => useGlobalSearch(q), {
      initialProps: { q: 'log' },
    });
    rerender({ q: 'log4j' });
    await waitFor(() => expect(result.current.bestMatch).not.toBeNull(), { timeout: 2000 });
    // Only the final query's request survives; the debounced earlier one was cleared/aborted.
    const urls = mock.history.get.map((r) => r.url ?? '');
    expect(urls.every((u) => u.includes('q=log4j'))).toBe(true);
  });

  it('drops an in-flight response after the request is aborted (controller lifecycle)', async () => {
    // First query's request has already fired and is in flight (delayed) when the
    // query changes; unmounting/rerendering aborts it so its later resolution is dropped.
    const STALE: SuggestResponse = {
      ...SUGGEST_OK,
      bestMatch: { ...SUGGEST_OK.bestMatch!, id: 'STALE', title: 'STALE' },
    };
    // The stale request stays pending until we release it by hand, so there is no
    // real wall-clock delay to wait out.
    let resolveStale: (() => void) | undefined;
    mock.onGet(/q=stalequery/).reply(
      () =>
        new Promise((resolve) => {
          resolveStale = () => resolve([200, STALE]);
        }),
    );
    mock.onGet(/q=freshquery/).reply(200, SUGGEST_OK);

    jest.useFakeTimers();
    const { result, rerender } = renderHook(({ q }) => useGlobalSearch(q), {
      initialProps: { q: 'stalequery' },
    });
    // Fire the first request past the debounce window; it stays in flight (unresolved).
    act(() => {
      jest.advanceTimersByTime(210);
    });
    expect(mock.history.get.length).toBeGreaterThan(0);
    // Switch queries to abort the in-flight stale request, then let the fresh one fire.
    rerender({ q: 'freshquery' });
    act(() => {
      jest.advanceTimersByTime(210);
    });
    jest.useRealTimers();
    await waitFor(() => expect(result.current.bestMatch?.id).toBe('CVE-2021-44228'), {
      timeout: 2000,
    });
    // Release the stale response now; because its request was aborted it must be dropped.
    await act(async () => {
      resolveStale?.();
      await Promise.resolve();
    });
    expect(result.current.bestMatch?.id).toBe('CVE-2021-44228');
  });
});

describe('useGlobalSearch — full (results)', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(axios);
  });
  afterEach(() => mock.restore());

  it('issues one GET /rest/search/results with tab + source plumbed', async () => {
    mock.onGet(RESULTS_RE).reply(200, RESULTS_OK);
    renderHook(() =>
      useGlobalSearch('log4j', { mode: 'full', tab: 'COMPONENT', source: 'catalog', pageSize: 25 }),
    );
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(mock.history.get).toHaveLength(1);
    const url = mock.history.get[0].url ?? '';
    expect(url).toContain('/rest/search/results');
    expect(url).toContain('tab=COMPONENT');
    expect(url).toContain('source=catalog');
    expect(url).toContain('pageSize=25');
  });

  it('maps ResultsResponse to flat results + totalEstimate + exact-total flag', async () => {
    mock.onGet(RESULTS_RE).reply(200, RESULTS_OK);
    const { result } = renderHook(() => useGlobalSearch('log4j', { mode: 'full' }));
    await waitFor(() => expect(result.current.results.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(result.current.results).toHaveLength(2);
    expect(result.current.totalEstimate).toBe(3);
    expect(result.current.isExactTotal).toBe(true);
    expect(result.current.results[1].fields.ecosystem).toBe('maven');
  });

  it('marks the total as non-exact when the estimate hits the 10000 cap', async () => {
    mock.onGet(RESULTS_RE).reply(200, { ...RESULTS_OK, totalEstimate: 10000 });
    const { result } = renderHook(() => useGlobalSearch('log4j', { mode: 'full' }));
    await waitFor(() => expect(result.current.totalEstimate).toBe(10000), { timeout: 2000 });
    expect(result.current.isExactTotal).toBe(false);
  });

  it('omits includeTabCounts by default so a caller pays for one search, not six', async () => {
    mock.onGet(RESULTS_RE).reply(200, RESULTS_OK);
    renderHook(() => useGlobalSearch('log4j', { mode: 'full', tab: 'COMPONENT' }));
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(mock.history.get[0].url ?? '').not.toContain('includeTabCounts');
  });

  it('sends includeTabCounts=true when the caller opts into the sibling count probe', async () => {
    mock.onGet(RESULTS_RE).reply(200, { ...RESULTS_OK, tabCounts: { ALL: 3, COMPONENT: 1 } });
    const { result } = renderHook(() =>
      useGlobalSearch('log4j', { mode: 'full', tab: 'COMPONENT', includeTabCounts: true }),
    );
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(mock.history.get[0].url ?? '').toContain('includeTabCounts=true');
    await waitFor(() => expect(result.current.tabCounts).toBeDefined(), { timeout: 2000 });
    expect(result.current.tabCounts?.COMPONENT).toBe(1);
  });

  it('re-fetches when includeTabCounts flips, so the flag is a real fetch dependency', async () => {
    mock.onGet(RESULTS_RE).reply(200, RESULTS_OK);
    const { rerender } = renderHook(
      ({ includeTabCounts }) => useGlobalSearch('log4j', { mode: 'full', tab: 'COMPONENT', includeTabCounts }),
      { initialProps: { includeTabCounts: false } },
    );
    await waitFor(() => expect(mock.history.get.length).toBe(1), { timeout: 2000 });
    expect(mock.history.get[0].url ?? '').not.toContain('includeTabCounts');

    rerender({ includeTabCounts: true });
    await waitFor(() => expect(mock.history.get.length).toBe(2), { timeout: 2000 });
    expect(mock.history.get[1].url ?? '').toContain('includeTabCounts=true');
  });

  it('tolerates an absent tabCounts and reads it when present (forward compat)', async () => {
    mock.onGet(RESULTS_RE).replyOnce(200, RESULTS_OK);
    const { result, rerender } = renderHook(({ q }) => useGlobalSearch(q, { mode: 'full' }), {
      initialProps: { q: 'log4j' },
    });
    await waitFor(() => expect(result.current.results.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(result.current.tabCounts).toBeUndefined();

    mock.onGet(RESULTS_RE).reply(200, { ...RESULTS_OK, tabCounts: { ALL: 3, COMPONENT: 1 } });
    rerender({ q: 'log4shell' });
    await waitFor(() => expect(result.current.tabCounts).toBeDefined(), { timeout: 2000 });
    expect(result.current.tabCounts?.ALL).toBe(3);
  });
});
