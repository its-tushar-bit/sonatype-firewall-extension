/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { act, renderHook, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import {
  COMPONENTS_INDEX_NOT_READY_MESSAGE,
  useComponentsList,
} from 'MainRoot/nosc/componentsList/useComponentsList';
import { MOCK_COMPONENTS_CATALOG_RESPONSE } from 'TestRoot/nosc/componentsList/mockComponentsListData';
import { getSearchCatalogUrl } from 'MainRoot/util/CLMLocation';

describe('useComponentsList', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('resolves ready with mapped components and totals', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, MOCK_COMPONENTS_CATALOG_RESPONSE);

    const { result } = renderHook(() => useComponentsList());

    expect(result.current.loading).toBe(true);

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.components).toHaveLength(2);
    expect(result.current.components[0].name).toBe('guava');
    expect(result.current.total).toBe(2);
    expect(result.current.error).toBeNull();
    expect(result.current.info).toBeNull();
  });

  it('maps HTTP 409 to an informational not-ready panel', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply(409, { message: 'index building' });

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.info).not.toBeNull());

    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.info?.message).toBe(COMPONENTS_INDEX_NOT_READY_MESSAGE);
    expect(result.current.info?.testId).toBe('components-list-not-ready');
  });

  it('maps HTTP 500 to an error state', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply(500, { message: 'Backend unavailable' });

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.error).toBeTruthy());

    expect(result.current.loading).toBe(false);
    expect(result.current.info).toBeNull();
  });

  it('posts catalog request with query and local org names', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, MOCK_COMPONENTS_CATALOG_RESPONSE);

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.submitSearch('guava'));
    act(() => result.current.toggleFilter('organizations', 'Java Team'));

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body).toEqual(
        expect.objectContaining({
          entityType: 'COMPONENT',
          source: 'local',
          page: 1,
          filters: expect.objectContaining({
            query: 'guava',
            organizations: ['Java Team'],
          }),
        }),
      );
    });
  });

  it('switches source to catalog and omits organizations', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, {
      ...MOCK_COMPONENTS_CATALOG_RESPONSE,
      source: 'catalog',
    });

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.setTab('catalog'));
    act(() => result.current.toggleFilter('organizations', 'Java Team'));
    act(() => result.current.toggleFilter('ecosystems', 'npm'));

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body.source).toBe('catalog');
      expect(body.filters).toEqual({ ecosystems: ['npm'] });
    });
  });

  it('resetFilters clears active selections and resets page', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, MOCK_COMPONENTS_CATALOG_RESPONSE);

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.toggleFilter('ecosystems', 'maven'));
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(true));

    act(() => result.current.resetFilters());
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(false));
    expect(result.current.page).toBe(0);
  });

  it('keeps local page 0 while a stale response page is still in data after filter reset', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.page === 2) {
        return [
          200,
          {
            ...MOCK_COMPONENTS_CATALOG_RESPONSE,
            source: 'catalog',
            page: 2,
            pageSize: 50,
            totalEstimate: 100,
            rows: [{ id: 'page-2', title: 'page-2', source: 'catalog' }],
          },
        ];
      }
      return [
        200,
        {
          ...MOCK_COMPONENTS_CATALOG_RESPONSE,
          source: 'catalog',
          page: 1,
          pageSize: 50,
          totalEstimate: 100,
        },
      ];
    });

    const { result } = renderHook(() => useComponentsList({ initialState: {
      tab: 'catalog',
      search: '',
      page: 0,
      filters: { organizations: new Set(), ecosystems: new Set() },
    } }));

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.setPage(1));
    await waitFor(() => expect(result.current.page).toBe(1));

    act(() => result.current.toggleFilter('ecosystems', 'npm'));
    // Prefer local page immediately — do not snap back to mapped.page from the stale page-2 payload.
    expect(result.current.page).toBe(0);
  });

  it('clears organization filters when switching to the Catalog tab', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, MOCK_COMPONENTS_CATALOG_RESPONSE);

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.toggleFilter('organizations', 'Java Team'));
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(true));

    act(() => result.current.setTab('catalog'));
    expect(result.current.filters.organizations.size).toBe(0);
  });
});
