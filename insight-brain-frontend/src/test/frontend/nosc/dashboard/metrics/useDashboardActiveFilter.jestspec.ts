/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { act, renderHook, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { useDashboardActiveFilter } from 'MainRoot/nosc/dashboard/metrics/useDashboardActiveFilter';
import { getDashboardFilters } from 'MainRoot/util/CLMLocation';

describe('useDashboardActiveFilter', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('requests only the active-filter document and maps persisted IDs to metric scope', async () => {
    axiosMock.onGet(getDashboardFilters()).reply(200, {
      needsAcknowledgement: false,
      filter: {
        organizationFilters: ['org-1'],
        applicationFilters: ['app-2'],
        stageTypeFilters: ['build'],
        tagFilters: ['tag-3'],
      },
    });

    const { result } = renderHook(() => useDashboardActiveFilter());

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.scope).toEqual({
      organizationIds: ['org-1'],
      applicationIds: ['app-2'],
      stageIds: ['build'],
      tagIds: ['tag-3'],
    });
    expect(axiosMock.history.get.map(({ url }) => url)).toEqual([getDashboardFilters()]);
  });

  it('preserves acknowledgement and maps missing filter arrays to an empty scope', async () => {
    axiosMock.onGet(getDashboardFilters()).reply(200, {
      needsAcknowledgement: true,
      filter: null,
    });

    const { result } = renderHook(() => useDashboardActiveFilter());

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.needsAcknowledgement).toBe(true);
    expect(result.current.scope).toEqual({});
  });

  it('retry recovers from an active-filter request failure', async () => {
    axiosMock.onGet(getDashboardFilters()).replyOnce(500).onGet(getDashboardFilters()).reply(200, {
      needsAcknowledgement: false,
      filter: null,
    });

    const { result } = renderHook(() => useDashboardActiveFilter());
    await waitFor(() => expect(result.current.error).not.toBeNull());

    act(() => result.current.retry());

    await waitFor(() => expect(result.current.error).toBeNull());
    expect(axiosMock.history.get).toHaveLength(2);
  });
});
