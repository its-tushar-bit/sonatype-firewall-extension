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
});
