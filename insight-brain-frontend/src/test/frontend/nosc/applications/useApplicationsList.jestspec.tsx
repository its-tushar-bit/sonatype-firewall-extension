/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { act, renderHook, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import {
  APPLICATIONS_INDEX_NOT_READY_MESSAGE,
  useApplicationsList,
} from 'MainRoot/nosc/applications/useApplicationsList';
import { EMPTY_APPLICATIONS_LIST_FILTERS } from 'MainRoot/nosc/applications/applicationsListFilters';
import { getApplicationsListUrl } from 'MainRoot/util/CLMLocation';

const OK_BODY = {
  applications: [
    {
      applicationId: 'apple-java',
      applicationName: 'Apple - Java',
      organizationId: 'org-java',
      organizationName: 'Java-team',
      totalApplicationRisk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
      stageRisks: [],
    },
  ],
  facets: { totalApplications: 1 },
  total: 1,
  page: 0,
  pageSize: 50,
  hasNextPage: false,
  source: 'index',
};

describe('useApplicationsList', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('resolves ready with mapped applications and totals', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(200, OK_BODY);

    const { result } = renderHook(() => useApplicationsList());

    expect(result.current.loading).toBe(true);

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.applications).toHaveLength(1);
    expect(result.current.total).toBe(1);
    expect(result.current.error).toBeNull();
    expect(result.current.info).toBeNull();
  });

  it('maps HTTP 409 to an informational not-ready panel instead of loading+error', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(409, { message: 'index building' });

    const { result } = renderHook(() => useApplicationsList());

    await waitFor(() => expect(result.current.info).not.toBeNull());

    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.info?.message).toBe(APPLICATIONS_INDEX_NOT_READY_MESSAGE);
    expect(result.current.info?.testId).toBe('applications-list-not-ready');
  });

  it('maps HTTP 500 to an error state', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(500, { message: 'Backend unavailable' });

    const { result } = renderHook(() => useApplicationsList());

    await waitFor(() => expect(result.current.error).toBeTruthy());

    expect(result.current.loading).toBe(false);
    expect(result.current.info).toBeNull();
  });

  it('clamps the page index when the result set shrinks', async () => {
    axiosMock
      .onPost(getApplicationsListUrl())
      .reply(200, { ...OK_BODY, total: 1, page: 2, hasNextPage: false });

    const { result } = renderHook(() => useApplicationsList());

    act(() => result.current.setPage(2));

    await waitFor(() => expect(result.current.page).toBe(0));
  });

  it('retains the requested page when the API omits page in its response', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(200, {
      ...OK_BODY,
      page: undefined,
      total: 120,
      hasNextPage: true,
    });

    const { result } = renderHook(() => useApplicationsList());

    act(() => result.current.setPage(1));

    await waitFor(() => expect(result.current.applications).toHaveLength(1));
    expect(result.current.page).toBe(1);
    expect(result.current.hasNextPage).toBe(true);
  });

  it('keeps the requested page while a stale hasNextPage response is still cached', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      const page = typeof body.page === 'number' ? body.page : 0;
      return [
        200,
        {
          ...OK_BODY,
          page,
          total: 120,
          hasNextPage: page === 0,
        },
      ];
    });

    const { result } = renderHook(() => useApplicationsList());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.page).toBe(0);

    act(() => result.current.setPage(1));

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body.page).toBe(1);
    });
    expect(result.current.page).toBe(1);
  });

  it('posts search and orderBy in the list request body', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(200, OK_BODY);

    const { result } = renderHook(() => useApplicationsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.submitSearch('apple'));
    act(() => result.current.changeOrderBy('lastEvaluationTime'));

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body).toEqual(
        expect.objectContaining({
          search: 'apple',
          orderBy: 'lastEvaluationTime',
          page: 0,
        }),
      );
    });
  });

  it('posts filter fields when sidebar selections change', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(200, OK_BODY);

    const { result } = renderHook(() => useApplicationsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.toggleFilter('organizationIds', 'org-java'));

    await waitFor(() => {
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body).toEqual(
        expect.objectContaining({
          organizationIds: ['org-java'],
          page: 0,
        }),
      );
    });
  });

  it('ignores no-op threat range commits without resetting page', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(200, {
      ...OK_BODY,
      page: 1,
      hasNextPage: true,
    });

    const { result } = renderHook(() => useApplicationsList());

    act(() => result.current.setPage(1));
    await waitFor(() => expect(result.current.page).toBe(1));
    const postsBefore = axiosMock.history.post.length;

    act(() => result.current.setThreatRange([0, 10]));

    expect(result.current.page).toBe(1);
    expect(axiosMock.history.post.length).toBe(postsBefore);
  });

  it('seeds initial state from route without a default-scope fetch first', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(200, OK_BODY);

    const { result } = renderHook(() =>
      useApplicationsList({
        initialState: {
          search: 'apple',
          orderBy: 'lastEvaluationTime',
          page: 1,
          filters: {
            ...EMPTY_APPLICATIONS_LIST_FILTERS,
            stageIds: new Set(['build']),
          },
        },
      }),
    );

    await waitFor(() => expect(result.current.loading).toBe(false));

    const firstRequest = axiosMock.history.post[0];
    const body = JSON.parse(String(firstRequest?.data));
    expect(body).toEqual(
      expect.objectContaining({
        search: 'apple',
        orderBy: 'lastEvaluationTime',
        page: 1,
        stageIds: ['build'],
      }),
    );
  });

  it('syncQueryState hydrates search, sort, page, and filters from route params', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(200, OK_BODY);

    const { result } = renderHook(() => useApplicationsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() =>
      result.current.syncQueryState({
        search: 'banana',
        orderBy: 'lastEvaluationTime',
        page: 1,
        filters: {
          ...EMPTY_APPLICATIONS_LIST_FILTERS,
          stageIds: new Set(['build']),
          organizationIds: new Set(['org-java']),
          policyTypes: new Set(['security']),
          violationStates: new Set(['OPEN']),
        },
      }),
    );

    expect(result.current.search).toBe('banana');
    expect(result.current.orderBy).toBe('lastEvaluationTime');
    expect(result.current.filters.stageIds.has('build')).toBe(true);
    expect(result.current.filters.organizationIds.has('org-java')).toBe(true);
    expect(result.current.filters.policyTypes.has('security')).toBe(true);
    expect(result.current.filters.violationStates.has('OPEN')).toBe(true);
  });

  it('resetFilters clears active selections and resets page', async () => {
    axiosMock.onPost(getApplicationsListUrl()).reply(200, OK_BODY);

    const { result } = renderHook(() => useApplicationsList());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.toggleFilter('stageIds', 'build'));
    await waitFor(() => expect(result.current.hasActiveFilters).toBe(true));

    act(() => result.current.resetFilters());

    await waitFor(() => {
      expect(result.current.hasActiveFilters).toBe(false);
      const lastRequest = axiosMock.history.post.at(-1);
      const body = JSON.parse(String(lastRequest?.data));
      expect(body.stageIds).toBeUndefined();
      expect(body.page).toBe(0);
    });
  });
});
