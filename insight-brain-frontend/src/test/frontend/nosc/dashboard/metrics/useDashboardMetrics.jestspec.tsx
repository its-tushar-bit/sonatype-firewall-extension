/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { act, renderHook, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { useDashboardMetrics } from 'MainRoot/nosc/dashboard/metrics/useDashboardMetrics';
import { getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';

const OK_BODY = {
  applications: { total: 12, breakdown: null, source: 'index' },
  violations: {
    total: 7,
    breakdown: { critical: 1, severe: 2, moderate: 3, low: 1 },
    source: 'index',
  },
  waivers: { total: 4, breakdown: { existing: 3, requested: 1 }, source: 'sql' },
  lastUpdatedAt: 1_700_000_000_000,
};

describe('useDashboardMetrics (CLM-40905 AT-F16: single POST + state machine)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('issues exactly ONE POST on mount with the scope in the body, then resolves ready', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, OK_BODY);

    const scope = { organizationIds: ['org-1'], applicationIds: ['app-2'] };
    const { result } = renderHook(() => useDashboardMetrics(scope));

    expect(result.current.status).toBe('loading');

    await waitFor(() => expect(result.current.status).toBe('ready'));

    expect(axiosMock.history.post).toHaveLength(1);
    expect(axiosMock.history.post[0].url).toBe(getDashboardMetricsUrl());
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(scope);
    expect(result.current.data).toEqual(OK_BODY);
  });

  it('does not re-POST across re-renders when the scope is unchanged (one POST on mount)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, OK_BODY);

    const { result, rerender } = renderHook(({ s }) => useDashboardMetrics(s), {
      initialProps: { s: { organizationIds: ['org-1'] } as Record<string, unknown> },
    });
    await waitFor(() => expect(result.current.status).toBe('ready'));

    // New object, same contents → must NOT trigger another request.
    rerender({ s: { organizationIds: ['org-1'] } });
    rerender({ s: { organizationIds: ['org-1'] } });

    expect(axiosMock.history.post).toHaveLength(1);
  });

  it('maps a 409 (index not ready) to status "not-ready" rather than a generic error', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(409, { message: 'index building' });

    const { result } = renderHook(() => useDashboardMetrics({}));

    await waitFor(() => expect(result.current.status).toBe('not-ready'));
    expect(result.current.data).toBeNull();
  });

  it('maps a 500 to status "error"', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(500, {});

    const { result } = renderHook(() => useDashboardMetrics({}));

    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.error).toBeTruthy();
  });

  it('retry() re-issues the POST and can recover from error → ready', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).replyOnce(500, {});
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, OK_BODY);

    const { result } = renderHook(() => useDashboardMetrics({}));
    await waitFor(() => expect(result.current.status).toBe('error'));

    act(() => result.current.retry());

    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(axiosMock.history.post.length).toBeGreaterThanOrEqual(2);
  });

  it('retains the last successful payload when a refresh fails', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).replyOnce(200, OK_BODY);
    axiosMock.onPost(getDashboardMetricsUrl()).replyOnce(500, {});

    const { result, rerender } = renderHook(({ s }) => useDashboardMetrics(s), {
      initialProps: { s: { organizationIds: ['org-1'] } as Record<string, unknown> },
    });
    await waitFor(() => expect(result.current.status).toBe('ready'));

    rerender({ s: { organizationIds: ['org-2'] } });
    await waitFor(() => expect(result.current.status).toBe('error'));

    expect(result.current.data).toEqual(OK_BODY);
  });
});
