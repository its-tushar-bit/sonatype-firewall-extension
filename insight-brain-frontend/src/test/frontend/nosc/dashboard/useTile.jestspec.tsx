/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { act, renderHook, waitFor } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';

describe('useTile', () => {
  let axiosMock: any;
  const URL = '/rest/example/tile';

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('starts in loading state, transitions to ready with data on success', async () => {
    axiosMock.onGet(URL).reply(200, { value: 42 });

    const { result } = renderHook(() => useTile<{ value: number }>(URL));

    expect(result.current.status).toBe('loading');
    expect(result.current.data).toBeNull();

    await waitFor(() => {
      expect(result.current.status).toBe('ready');
    });

    expect(result.current.data).toEqual({ value: 42 });
    expect(result.current.error).toBeNull();
  });

  it('transitions to error state on failed fetch', async () => {
    axiosMock.onGet(URL).reply(500, { message: 'boom' });

    const { result } = renderHook(() => useTile<unknown>(URL));

    await waitFor(() => {
      expect(result.current.status).toBe('error');
    });

    expect(result.current.data).toBeNull();
    expect(result.current.error).toBeTruthy();
  });

  it('retry refetches and transitions loading → ready', async () => {
    axiosMock.onGet(URL).replyOnce(500, { message: 'first fail' });
    axiosMock.onGet(URL).reply(200, { value: 7 });

    const { result } = renderHook(() => useTile<{ value: number }>(URL));

    await waitFor(() => {
      expect(result.current.status).toBe('error');
    });

    act(() => {
      result.current.retry();
    });

    await waitFor(() => {
      expect(result.current.status).toBe('ready');
    });
    expect(result.current.data).toEqual({ value: 7 });
  });

  it('does not call setState after unmount (avoids React 18 warning)', async () => {
    axiosMock.onGet(URL).reply(() => new Promise((resolve) => setTimeout(() => resolve([200, { value: 1 }]), 50)));

    const { unmount } = renderHook(() => useTile<{ value: number }>(URL));
    unmount();

    await new Promise((r) => setTimeout(r, 100));
  });

  it('ignores stale responses when the request key changes before an earlier request settles', async () => {
    let resolveSlow: (value: [number, { total: number }]) => void = () => {};
    const slowPromise = new Promise<[number, { total: number }]>((resolve) => {
      resolveSlow = resolve;
    });

    axiosMock.onPost('/rest/example/metrics').replyOnce(() => slowPromise);
    axiosMock.onPost('/rest/example/metrics').reply(200, { total: 2 });

    const { result, rerender } = renderHook(
      ({ body }: { body: Record<string, string> }) =>
        useTile<{ total: number }>('/rest/example/metrics', undefined, {
          method: 'post',
          body,
        }),
      { initialProps: { body: { scope: 'a' } } },
    );

    rerender({ body: { scope: 'b' } });
    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(result.current.data).toEqual({ total: 2 });

    resolveSlow([200, { total: 1 }]);
    await new Promise((r) => setTimeout(r, 50));
    expect(result.current.data).toEqual({ total: 2 });
  });

  it('does not issue a request while enabled is false', async () => {
    const { result, rerender } = renderHook(
      ({ enabled }: { enabled: boolean }) =>
        useTile<{ value: number }>(URL, undefined, { enabled }),
      { initialProps: { enabled: false } },
    );

    expect(result.current.status).toBe('loading');
    expect(axiosMock.history.get).toHaveLength(0);

    axiosMock.onGet(URL).reply(200, { value: 9 });
    rerender({ enabled: true });
    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(result.current.data).toEqual({ value: 9 });
  });

  it('supports POST with a body and custom error status mapping', async () => {
    axiosMock.onPost('/rest/example/metrics').reply(409, { message: 'building' });

    const { result } = renderHook(() =>
      useTile<{ total: number }>('/rest/example/metrics', undefined, {
        method: 'post',
        body: { organizationIds: ['org-1'] },
        mapErrorStatus: (code) => (code === 409 ? 'not-ready' : 'error'),
      }),
    );

    await waitFor(() => expect(result.current.status).toBe('not-ready'));
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({ organizationIds: ['org-1'] });
  });
});
