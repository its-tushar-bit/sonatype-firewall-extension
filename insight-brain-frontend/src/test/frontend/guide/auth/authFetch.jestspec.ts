/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAuthFetch } from 'GuideRoot/auth/authFetch';

function mockResponse(status: number) {
  return { status, ok: status >= 200 && status < 300 };
}

describe('createAuthFetch', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('returns the response on success (200)', async () => {
    global.fetch = jest.fn().mockResolvedValue(mockResponse(200));
    const onUnauthorized = jest.fn();
    const authFetch = createAuthFetch(onUnauthorized);

    const response = await authFetch('/api/data');

    expect(response.status).toBe(200);
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('calls onUnauthorized and returns the response on 401', async () => {
    global.fetch = jest.fn().mockResolvedValue(mockResponse(401));
    const onUnauthorized = jest.fn();
    const authFetch = createAuthFetch(onUnauthorized);

    const response = await authFetch('/api/protected');

    expect(response.status).toBe(401);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('passes through non-401 error responses without calling onUnauthorized', async () => {
    global.fetch = jest.fn().mockResolvedValue(mockResponse(403));
    const onUnauthorized = jest.fn();
    const authFetch = createAuthFetch(onUnauthorized);

    const response = await authFetch('/api/forbidden');

    expect(response.status).toBe(403);
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('forwards all arguments to fetch', async () => {
    global.fetch = jest.fn().mockResolvedValue(mockResponse(200));
    const authFetch = createAuthFetch(jest.fn());

    await authFetch('/api/data', { method: 'POST', headers: { 'X-Custom': 'value' } });

    expect(global.fetch).toHaveBeenCalledWith('/api/data', {
      method: 'POST',
      headers: { 'X-Custom': 'value' },
    });
  });

  it('propagates network errors without calling onUnauthorized', async () => {
    global.fetch = jest.fn().mockRejectedValue(new TypeError('Network error'));
    const onUnauthorized = jest.fn();
    const authFetch = createAuthFetch(onUnauthorized);

    await expect(authFetch('/api/data')).rejects.toThrow('Network error');
    expect(onUnauthorized).not.toHaveBeenCalled();
  });
});
