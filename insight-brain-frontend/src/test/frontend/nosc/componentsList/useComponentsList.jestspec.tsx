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
import { getComponentsListUrl, getSearchCatalogUrl } from 'MainRoot/util/CLMLocation';

const MOCK_DASHBOARD_RESPONSE = {
  components: [
    {
      hash: 'abc123',
      derivedComponentName: 'guava',
      scoreCritical: 1,
      scoreSevere: 0,
      scoreModerate: 2,
      scoreLow: 0,
      affectedApplications: 3,
    },
    {
      hash: 'def456',
      derivedComponentName: 'commons-lang',
      scoreCritical: 0,
      scoreSevere: 1,
      scoreModerate: 0,
      scoreLow: 1,
      affectedApplications: 1,
    },
  ],
  total: 2,
  page: 0,
  pageSize: 50,
  hasNextPage: false,
  source: 'index',
  facets: {
    totalComponents: 2,
    organizations: { 'org-1': 2 },
    organizationNames: { 'org-1': 'Java Team' },
  },
};

describe('useComponentsList', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('resolves My Scan Data from the hybrid dashboard list with risk fields', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(200, MOCK_DASHBOARD_RESPONSE);

    const { result } = renderHook(() => useComponentsList());

    expect(result.current.loading).toBe(true);

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.components).toHaveLength(2);
    expect(result.current.components[0].name).toBe('guava');
    expect(result.current.components[0].scoreCritical).toBe(1);
    expect(result.current.components[0].affectedApplications).toBe(3);
    expect(result.current.total).toBe(2);
    expect(result.current.error).toBeNull();
    expect(result.current.info).toBeNull();
  });

  it('maps HTTP 409 to an informational not-ready panel', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(409, { message: 'index building' });

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.info).not.toBeNull());

    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.info?.message).toBe(COMPONENTS_INDEX_NOT_READY_MESSAGE);
    expect(result.current.info?.testId).toBe('components-list-not-ready');
  });

  it('maps HTTP 500 to an error state', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(500, { message: 'Backend unavailable' });

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.error).toBeTruthy());

    expect(result.current.loading).toBe(false);
    expect(result.current.info).toBeNull();
  });

  it('posts dashboard request with search and organization ids for My Scan Data', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(200, MOCK_DASHBOARD_RESPONSE);

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.submitSearch('guava'));
    act(() => result.current.toggleFilter('organizations', 'org-1'));

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      expect(lastRequest?.url).toBe(getComponentsListUrl());
      const body = JSON.parse(String(lastRequest?.data));
      expect(body).toEqual(
        expect.objectContaining({
          page: 0,
          pageSize: 50,
          search: 'guava',
          organizationIds: ['org-1'],
        }),
      );
    });
  });

  it('switches to catalog URL and omits organizations', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(200, MOCK_DASHBOARD_RESPONSE);
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
      expect(lastRequest?.url).toBe(getSearchCatalogUrl());
      const body = JSON.parse(String(lastRequest?.data));
      expect(body.source).toBe('catalog');
      expect(body.filters).toEqual({ ecosystems: ['npm'] });
    });
  });

  it('resetFilters clears active selections and resets page', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(200, MOCK_DASHBOARD_RESPONSE);

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.toggleFilter('organizations', 'org-1'));
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(true));

    act(() => result.current.resetFilters());
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(false));
    expect(result.current.page).toBe(0);
  });

  it('keeps local page 0 while a stale catalog response page is still in data after filter reset', async () => {
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
    expect(result.current.page).toBe(0);
  });

  it('clears organization filters when switching to the Catalog tab', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(200, MOCK_DASHBOARD_RESPONSE);
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, {
      ...MOCK_COMPONENTS_CATALOG_RESPONSE,
      source: 'catalog',
    });

    const { result } = renderHook(() => useComponentsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.toggleFilter('organizations', 'org-1'));
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(true));

    act(() => result.current.setTab('catalog'));
    expect(result.current.filters.organizations.size).toBe(0);
  });
});
