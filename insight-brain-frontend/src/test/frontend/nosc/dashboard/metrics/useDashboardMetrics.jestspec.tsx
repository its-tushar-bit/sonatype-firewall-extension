/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { act, renderHook, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { useDashboardMetrics } from 'MainRoot/nosc/dashboard/metrics/useDashboardMetrics';
import { getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';

const FAST_BODY = {
  applications: { total: 12, breakdown: null, source: 'index' },
  organizations: { total: 3, breakdown: null, source: 'index' },
  policies: { total: 2, breakdown: null, source: 'index' },
  waivers: { total: 4, breakdown: { existing: 3, requested: 1, expiring: 1 }, source: 'sql' },
  lastUpdatedAt: 1_700_000_000_000,
};

const HEAVY_ONLY_BODY = {
  violations: {
    total: 7,
    breakdown: { critical: 1, severe: 2, moderate: 3, low: 1 },
    source: 'index',
  },
  components: { total: 100, breakdown: null, source: 'index' },
  vulnerabilities: {
    total: 9,
    breakdown: { critical: 1, high: 2, medium: 3, low: 3 },
    source: 'index',
  },
  legal: {
    total: 5,
    breakdown: { applications: 2, components: 4 },
    source: 'index',
  },
  lastUpdatedAt: null,
};

function deferredResponse<T>() {
  let resolve!: (value: [number, T]) => void;
  const promise = new Promise<[number, T]>((promiseResolve) => {
    resolve = promiseResolve;
  });
  return { promise, resolve };
}

describe('useDashboardMetrics (two-phase fast + heavy)', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
    jest.restoreAllMocks();
  });

  it('POSTs fast tier first, becomes ready, then merges heavy tiles', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      return [200, body.includeHeavyMetrics === false ? FAST_BODY : HEAVY_ONLY_BODY];
    });

    const scope = { organizationIds: ['org-1'], applicationIds: ['app-2'] };
    const { result } = renderHook(() => useDashboardMetrics(scope));

    expect(result.current.status).toBe('loading');

    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(result.current.data?.applications?.total).toBe(12);
    expect(JSON.parse(axiosMock.history.post[0].data).includeHeavyMetrics).toBe(false);

    await waitFor(() => expect(result.current.heavyLoading).toBe(false));
    expect(result.current.data?.components?.total).toBe(100);
    expect(result.current.data?.vulnerabilities?.total).toBe(9);

    expect(axiosMock.history.post.length).toBe(2);
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
      ...scope,
      includeHeavyMetrics: false,
    });
    expect(JSON.parse(axiosMock.history.post[1].data)).toEqual({
      ...scope,
      includeHeavyMetrics: true,
    });
  });

  it('does not re-POST across re-renders when the scope is unchanged', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FAST_BODY);

    const { result, rerender } = renderHook(({ s }) => useDashboardMetrics(s), {
      initialProps: { s: { organizationIds: ['org-1'] } as Record<string, unknown> },
    });
    await waitFor(() => expect(result.current.status).toBe('ready'));
    await waitFor(() => expect(result.current.heavyLoading).toBe(false));

    const postsAfterMount = axiosMock.history.post.length;
    rerender({ s: { organizationIds: ['org-1'] } });
    rerender({ s: { organizationIds: ['org-1'] } });

    expect(axiosMock.history.post).toHaveLength(postsAfterMount);
  });

  it('does not re-POST when equivalent scope keys and ids arrive in a different order', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      return [200, body.includeHeavyMetrics === false ? FAST_BODY : HEAVY_ONLY_BODY];
    });

    const { result, rerender } = renderHook(({ s }) => useDashboardMetrics(s), {
      initialProps: {
        s: {
          organizationIds: ['org-2', 'org-1'],
          applicationIds: ['app-2', 'app-1'],
        } as Record<string, readonly string[]>,
      },
    });
    await waitFor(() => expect(result.current.data?.components?.total).toBe(100));

    const postsAfterMount = axiosMock.history.post.length;
    rerender({
      s: {
        applicationIds: ['app-1', 'app-2'],
        organizationIds: ['org-1', 'org-2'],
      },
    });

    expect(axiosMock.history.post).toHaveLength(postsAfterMount);
  });

  it('uses separate abort signals for the summary and heavy requests', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      return [200, body.includeHeavyMetrics === false ? FAST_BODY : HEAVY_ONLY_BODY];
    });

    const { result } = renderHook(() => useDashboardMetrics({}));
    await waitFor(() => expect(result.current.data?.components?.total).toBe(100));

    expect(axiosMock.history.post).toHaveLength(2);
    expect(axiosMock.history.post[0].signal).toBeDefined();
    expect(axiosMock.history.post[1].signal).toBeDefined();
    expect(axiosMock.history.post[0].signal === axiosMock.history.post[1].signal).toBe(false);
  });

  it('maps a 409 (index not ready) to status "not-ready" rather than a generic error', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(409, { message: 'index building' });

    const { result } = renderHook(() => useDashboardMetrics({}));

    await waitFor(() => expect(result.current.status).toBe('not-ready'));
    expect(result.current.data).toBeNull();
  });

  it('maps a 500 on the fast tier to status "error"', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(500, {});

    const { result } = renderHook(() => useDashboardMetrics({}));

    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.error).toBeTruthy();
  });

  it('retry() re-issues the POST and can recover from error → ready', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).replyOnce(500, {});
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FAST_BODY);

    const { result } = renderHook(() => useDashboardMetrics({}));
    await waitFor(() => expect(result.current.status).toBe('error'));

    act(() => result.current.retry());

    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(axiosMock.history.post.length).toBeGreaterThanOrEqual(2);
  });

  it('keeps fast KPIs when the heavy tier fails', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.includeHeavyMetrics === false) {
        return [200, FAST_BODY];
      }
      return [500, {}];
    });

    const { result } = renderHook(() => useDashboardMetrics({ organizationIds: ['org-1'] }));
    await waitFor(() => expect(result.current.status).toBe('ready'));
    await waitFor(() => expect(result.current.heavyLoading).toBe(false));

    expect(result.current.data?.applications?.total).toBe(12);
    expect(result.current.status).toBe('ready');
  });

  it('keeps summary fields when the heavy response omits them', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      return [200, body.includeHeavyMetrics === false ? FAST_BODY : HEAVY_ONLY_BODY];
    });

    const { result } = renderHook(() => useDashboardMetrics({}));
    await waitFor(() => expect(result.current.data?.components?.total).toBe(100));

    expect(result.current.data?.applications?.total).toBe(12);
    expect(result.current.heavyLoading).toBe(false);
    expect(result.current.data?.lastUpdatedAt).toBe(FAST_BODY.lastUpdatedAt);
  });

  it('retries only heavy metrics after a heavy failure', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.includeHeavyMetrics === false) return [200, FAST_BODY];
      return axiosMock.history.post.filter(({ data }) => JSON.parse(String(data)).includeHeavyMetrics === true)
        .length === 1
        ? [500, {}]
        : [200, HEAVY_ONLY_BODY];
    });

    const { result } = renderHook(() => useDashboardMetrics({}));
    await waitFor(() => expect(result.current.heavyError).not.toBeNull());

    act(() => result.current.retryHeavy());
    await waitFor(() => expect(result.current.data?.components?.total).toBe(100));

    const summaryPosts = axiosMock.history.post.filter(
      ({ data }) => JSON.parse(String(data)).includeHeavyMetrics === false
    );
    expect(summaryPosts).toHaveLength(1);
  });

  it('ignores stale scope work and starts heavy metrics only from the matching summary', async () => {
    const oldSummary = deferredResponse<typeof FAST_BODY>();
    const oldHeavy = deferredResponse<typeof HEAVY_ONLY_BODY>();
    const newSummary = deferredResponse<typeof FAST_BODY>();
    const newHeavy = deferredResponse<typeof HEAVY_ONLY_BODY>();
    const newFastBody = {
      ...FAST_BODY,
      applications: { ...FAST_BODY.applications, total: 24 },
      lastUpdatedAt: FAST_BODY.lastUpdatedAt + 1,
    };
    const newHeavyBody = {
      ...HEAVY_ONLY_BODY,
      components: { ...HEAVY_ONLY_BODY.components, total: 200 },
    };

    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.organizationIds[0] === 'org-old') {
        return body.includeHeavyMetrics === false ? oldSummary.promise : oldHeavy.promise;
      }
      return body.includeHeavyMetrics === false ? newSummary.promise : newHeavy.promise;
    });

    const { result, rerender } = renderHook(
      ({ organizationId }) => useDashboardMetrics({ organizationIds: [organizationId] }),
      {
        initialProps: { organizationId: 'org-old' },
      }
    );

    oldSummary.resolve([200, FAST_BODY]);
    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(({ data }) => {
          const body = JSON.parse(String(data));
          return body.organizationIds[0] === 'org-old' && body.includeHeavyMetrics === true;
        })
      ).toHaveLength(1)
    );

    rerender({ organizationId: 'org-new' });
    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(({ data }) => {
          const body = JSON.parse(String(data));
          return body.organizationIds[0] === 'org-new' && body.includeHeavyMetrics === false;
        })
      ).toHaveLength(1)
    );

    expect(
      axiosMock.history.post.filter(({ data }) => {
        const body = JSON.parse(String(data));
        return body.organizationIds[0] === 'org-new' && body.includeHeavyMetrics === true;
      })
    ).toHaveLength(0);

    newSummary.resolve([200, newFastBody]);
    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(({ data }) => {
          const body = JSON.parse(String(data));
          return body.organizationIds[0] === 'org-new' && body.includeHeavyMetrics === true;
        })
      ).toHaveLength(1)
    );
    newHeavy.resolve([200, newHeavyBody]);
    await waitFor(() => expect(result.current.data?.components?.total).toBe(200));

    await act(async () => {
      oldHeavy.resolve([200, HEAVY_ONLY_BODY]);
      await oldHeavy.promise;
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(result.current.data?.applications?.total).toBe(24);
    expect(result.current.data?.components?.total).toBe(200);
    expect(result.current.data?.lastUpdatedAt).toBe(newFastBody.lastUpdatedAt);
  });

  it('aborts an in-flight heavy request when the scope changes', async () => {
    const oldHeavy = deferredResponse<typeof HEAVY_ONLY_BODY>();
    const newSummary = deferredResponse<typeof FAST_BODY>();

    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.organizationIds[0] === 'org-old') {
        return body.includeHeavyMetrics === false ? [200, FAST_BODY] : oldHeavy.promise;
      }
      return newSummary.promise;
    });

    const { rerender } = renderHook(
      ({ organizationId }) => useDashboardMetrics({ organizationIds: [organizationId] }),
      {
        initialProps: { organizationId: 'org-old' },
      }
    );

    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(({ data }) => {
          const body = JSON.parse(String(data));
          return body.organizationIds[0] === 'org-old' && body.includeHeavyMetrics === true;
        })
      ).toHaveLength(1)
    );
    const oldHeavyRequest = axiosMock.history.post.find(({ data }) => {
      const body = JSON.parse(String(data));
      return body.organizationIds[0] === 'org-old' && body.includeHeavyMetrics === true;
    });
    expect(oldHeavyRequest?.signal?.aborted).toBe(false);

    rerender({ organizationId: 'org-new' });

    expect(oldHeavyRequest?.signal?.aborted).toBe(true);
  });

  it('retry() refetches both tiers after summary and heavy data are established', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      return [200, body.includeHeavyMetrics === false ? FAST_BODY : HEAVY_ONLY_BODY];
    });

    const { result } = renderHook(() => useDashboardMetrics({}));
    await waitFor(() => expect(result.current.data?.components?.total).toBe(100));

    act(() => result.current.retry());

    await waitFor(() => {
      const posts = axiosMock.history.post.map(({ data }) => JSON.parse(String(data)));
      expect(posts.filter(({ includeHeavyMetrics }) => includeHeavyMetrics === false)).toHaveLength(2);
      expect(posts.filter(({ includeHeavyMetrics }) => includeHeavyMetrics === true)).toHaveLength(2);
    });
  });

  it('requires a new summary before starting heavy metrics after re-enable', async () => {
    const restartedSummary = deferredResponse<typeof FAST_BODY>();
    let summaryRequests = 0;

    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      const body = JSON.parse(String(config.data));
      if (body.includeHeavyMetrics === true) return [200, HEAVY_ONLY_BODY];
      summaryRequests += 1;
      return summaryRequests === 1 ? [200, FAST_BODY] : restartedSummary.promise;
    });

    const { result, rerender } = renderHook(({ enabled }) => useDashboardMetrics({}, enabled), {
      initialProps: { enabled: true },
    });
    await waitFor(() => expect(result.current.data?.components?.total).toBe(100));

    rerender({ enabled: false });
    rerender({ enabled: true });
    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(({ data }) => JSON.parse(String(data)).includeHeavyMetrics === false)
      ).toHaveLength(2)
    );

    expect(
      axiosMock.history.post.filter(({ data }) => JSON.parse(String(data)).includeHeavyMetrics === true)
    ).toHaveLength(1);

    restartedSummary.resolve([
      200,
      {
        ...FAST_BODY,
        applications: { ...FAST_BODY.applications, total: 36 },
      },
    ]);
    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(({ data }) => JSON.parse(String(data)).includeHeavyMetrics === true)
      ).toHaveLength(2)
    );
    expect(result.current.data?.applications?.total).toBe(36);
  });

  it('ignores a late summary response after unmount', async () => {
    const summary = deferredResponse<typeof FAST_BODY>();
    let requestSignal: { readonly aborted: boolean } | undefined;
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    axiosMock.onPost(getDashboardMetricsUrl()).reply((config) => {
      requestSignal = config.signal;
      return summary.promise;
    });

    const { result, unmount } = renderHook(() => useDashboardMetrics({}));
    const stateBeforeUnmount = result.current;
    expect(requestSignal).toBeDefined();
    unmount();
    expect(requestSignal?.aborted).toBe(true);

    await act(async () => {
      summary.resolve([200, FAST_BODY]);
      await summary.promise;
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(result.current).toBe(stateBeforeUnmount);
    expect(axiosMock.history.post).toHaveLength(1);
    expect(
      consoleError.mock.calls.some((args) => args.some((arg) => /state update.*unmounted/i.test(String(arg))))
    ).toBe(false);
  });
});
