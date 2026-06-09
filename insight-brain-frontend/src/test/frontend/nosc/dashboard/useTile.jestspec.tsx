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

    // Wait long enough for the deferred response to fire; React would warn if
    // we tried to setState on the unmounted hook. We just want no throw.
    await new Promise((r) => setTimeout(r, 100));
  });
});
