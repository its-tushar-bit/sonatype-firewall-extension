/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { act, renderHook, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import {
  _clearWaiversCursorCacheForTesting,
  useAnaWaiversList,
  WAIVERS_INDEX_FORBIDDEN_MESSAGE,
  WAIVERS_INDEX_NOT_READY_MESSAGE,
  WAIVERS_INDEX_UNAVAILABLE_MESSAGE,
} from 'MainRoot/nosc/waivers/useAnaWaiversList';
import { getIndexQueryUrl } from 'MainRoot/util/CLMLocation';
import { MOCK_WAIVERS_INDEX_QUERY_RESPONSE } from 'TestRoot/nosc/waivers/mockWaiversAnaData';
import { EMPTY_WAIVERS_LIST_FILTERS } from 'MainRoot/nosc/waivers/waiversListFilters';

describe('useAnaWaiversList', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    _clearWaiversCursorCacheForTesting();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('resolves ready with mapped waivers and totals', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(200, MOCK_WAIVERS_INDEX_QUERY_RESPONSE);

    const { result } = renderHook(() => useAnaWaiversList());

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.waivers).toHaveLength(2);
    expect(result.current.waivers[0].policyName).toBe('Critical CVSS 9+');
    expect(result.current.total).toBe(2);
    expect(result.current.error).toBeNull();
    expect(result.current.info).toBeNull();
  });

  it('POSTs with entityType=WAIVER and default sort', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(200, MOCK_WAIVERS_INDEX_QUERY_RESPONSE);
    const { result } = renderHook(() => useAnaWaiversList());
    await waitFor(() => expect(result.current.loading).toBe(false));

    const body = JSON.parse(String(axiosMock.history.post.at(-1)?.data));
    expect(body).toEqual(
      expect.objectContaining({
        entityType: 'WAIVER',
        page: 1,
        includeFacets: true,
        sort: '-policyWaiverCreatedAt',
      }),
    );
  });

  it('maps HTTP 409 to a non-error informational panel', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(409, { message: 'index building' });
    const { result } = renderHook(() => useAnaWaiversList());

    await waitFor(() => expect(result.current.info).not.toBeNull());
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.info?.message).toBe(WAIVERS_INDEX_NOT_READY_MESSAGE);
    expect(result.current.info?.testId).toBe('waivers-list-not-ready');
  });

  it('maps HTTP 403 to a permissions informational panel', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(403, { message: 'forbidden' });
    const { result } = renderHook(() => useAnaWaiversList());

    await waitFor(() => expect(result.current.info).not.toBeNull());
    expect(result.current.error).toBeNull();
    expect(result.current.info?.message).toBe(WAIVERS_INDEX_FORBIDDEN_MESSAGE);
    expect(result.current.info?.testId).toBe('waivers-list-forbidden');
  });

  it('maps HTTP 404 to a Global Search unavailable informational panel', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(404, { message: 'not found' });
    const { result } = renderHook(() => useAnaWaiversList());

    await waitFor(() => expect(result.current.info).not.toBeNull());
    expect(result.current.error).toBeNull();
    expect(result.current.info?.message).toBe(WAIVERS_INDEX_UNAVAILABLE_MESSAGE);
    expect(result.current.info?.testId).toBe('waivers-list-unavailable');
  });

  it('maps HTTP 500 to an error state', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(500, { message: 'oops' });
    const { result } = renderHook(() => useAnaWaiversList());

    await waitFor(() => expect(result.current.error).toBeTruthy());
    expect(result.current.loading).toBe(false);
    expect(result.current.info).toBeNull();
  });

  it('submitSearch resets to page 1 and includes the search in the next request', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(200, MOCK_WAIVERS_INDEX_QUERY_RESPONSE);
    const { result } = renderHook(() => useAnaWaiversList());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.submitSearch('guava'));
    await waitFor(() => {
      const body = JSON.parse(String(axiosMock.history.post.at(-1)?.data));
      expect(body.filters?.query).toBe('guava');
      expect(body.page).toBe(1);
    });
    expect(result.current.page).toBe(1);
  });

  it('changeOrderBy switches sort token and resets page + cursors', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(200, {
      ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE,
      nextSearchAfter: 'cursor-2',
    });
    const { result } = renderHook(() => useAnaWaiversList());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.changeOrderBy('-policyWaiverThreatLevel'));
    await waitFor(() => {
      const body = JSON.parse(String(axiosMock.history.post.at(-1)?.data));
      expect(body.sort).toBe('-policyWaiverThreatLevel');
      expect(body.page).toBe(1);
      expect(body.searchAfter).toBeUndefined();
    });
  });

  it('paginates forward using the cursor returned by the prior response', async () => {
    // Two-stage handler: page 1 hands back a cursor; page 2 must carry that cursor.
    let cursorSeenOnPageTwo: string | undefined;
    axiosMock.onPost(getIndexQueryUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.page === 1) {
        return [200, { ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE, nextSearchAfter: 'cursor-p2' }];
      }
      if (body.page === 2) {
        cursorSeenOnPageTwo = body.searchAfter;
        return [200, { ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE, page: 2, nextSearchAfter: null }];
      }
      return [200, MOCK_WAIVERS_INDEX_QUERY_RESPONSE];
    });

    const { result } = renderHook(() => useAnaWaiversList());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.setPage(2));
    await waitFor(() => expect(cursorSeenOnPageTwo).toBe('cursor-p2'));
    expect(result.current.page).toBe(2);
  });

  it('clamps to page 1 when a deep link asks for page > 1 without a cached cursor', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(200, MOCK_WAIVERS_INDEX_QUERY_RESPONSE);
    const { result } = renderHook(() =>
      useAnaWaiversList({
        initialState: {
          search: '',
          orderBy: '-policyWaiverCreatedAt',
          page: 5,
          filters: {
            threatLevelIds: new Set(),
            expiryStatusIds: new Set(),
            autoStatusIds: new Set(),
            organizationIds: new Set(),
            applicationIds: new Set(),
            policyIds: new Set(),
          },
        },
      }),
    );
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.page).toBe(1);
    const body = JSON.parse(String(axiosMock.history.post.at(-1)?.data));
    expect(body.page).toBe(1);
    expect(body.searchAfter).toBeUndefined();
  });

  it('resetFilters clears active selections and resets page', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply(200, MOCK_WAIVERS_INDEX_QUERY_RESPONSE);
    const { result } = renderHook(() => useAnaWaiversList());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.toggleFilter('organizationIds', 'Java Team'));
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(true));
    act(() => result.current.resetFilters());
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(false));
    expect(result.current.page).toBe(1);
  });

  it('survives remount with cached cursor so page 2 still POSTs searchAfter', async () => {
    axiosMock.onPost(getIndexQueryUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.page === 1) {
        return [200, { ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE, nextSearchAfter: 'cursor-remount' }];
      }
      return [200, {
        ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE,
        page: 2,
        nextSearchAfter: null,
      }];
    });

    const initialState = {
      search: '',
      orderBy: '-policyWaiverCreatedAt' as const,
      page: 1,
      filters: EMPTY_WAIVERS_LIST_FILTERS,
    };
    const { result, unmount } = renderHook(() => useAnaWaiversList({ initialState }));
    await waitFor(() => expect(result.current.loading).toBe(false));
    act(() => result.current.setPage(2));
    await waitFor(() => expect(result.current.page).toBe(2));
    unmount();

    const remounted = renderHook(() =>
      useAnaWaiversList({
        initialState: { ...initialState, page: 2 },
      }),
    );
    await waitFor(() => expect(remounted.result.current.loading).toBe(false));
    const body = JSON.parse(String(axiosMock.history.post.at(-1)?.data));
    expect(body.page).toBe(2);
    expect(body.searchAfter).toBe('cursor-remount');
    expect(remounted.result.current.page).toBe(2);
  });
});
