/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAuthFetch } from 'GuideRoot/auth/authFetch';
import * as csrfTokenModule from 'GuideRoot/auth/csrfToken';
import { RequestQueue } from 'GuideRoot/auth/requestQueue';

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

  describe('CSRF token injection', () => {
    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('adds X-CSRF-TOKEN header on POST requests', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(200));
      jest.spyOn(csrfTokenModule, 'getCsrfToken').mockReturnValue('csrf-abc');
      const authFetch = createAuthFetch(jest.fn());

      await authFetch('/api/data', { method: 'POST' });

      const [, init] = (global.fetch as jest.Mock).mock.calls[0];
      expect(init.headers.get('X-CSRF-TOKEN')).toBe('csrf-abc');
    });

    it('adds X-CSRF-TOKEN header on PUT requests', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(200));
      jest.spyOn(csrfTokenModule, 'getCsrfToken').mockReturnValue('csrf-abc');
      const authFetch = createAuthFetch(jest.fn());

      await authFetch('/api/data', { method: 'PUT' });

      const [, init] = (global.fetch as jest.Mock).mock.calls[0];
      expect(init.headers.get('X-CSRF-TOKEN')).toBe('csrf-abc');
    });

    it('adds X-CSRF-TOKEN header on DELETE requests', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(200));
      jest.spyOn(csrfTokenModule, 'getCsrfToken').mockReturnValue('csrf-abc');
      const authFetch = createAuthFetch(jest.fn());

      await authFetch('/api/data', { method: 'DELETE' });

      const [, init] = (global.fetch as jest.Mock).mock.calls[0];
      expect(init.headers.get('X-CSRF-TOKEN')).toBe('csrf-abc');
    });

    it('does NOT add X-CSRF-TOKEN header on GET requests', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(200));
      jest.spyOn(csrfTokenModule, 'getCsrfToken').mockReturnValue('csrf-abc');
      const authFetch = createAuthFetch(jest.fn());

      await authFetch('/api/data');

      const [, init] = (global.fetch as jest.Mock).mock.calls[0];
      expect(init).toBeUndefined();
    });

    it('does NOT add X-CSRF-TOKEN header when cookie is absent', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(200));
      jest.spyOn(csrfTokenModule, 'getCsrfToken').mockReturnValue(undefined);
      const authFetch = createAuthFetch(jest.fn());

      await authFetch('/api/data', { method: 'POST' });

      const [, init] = (global.fetch as jest.Mock).mock.calls[0];
      // When CSRF token is absent, init should be passed through unchanged
      // (no headers property was passed, so it remains undefined)
      expect(init.headers).toBeUndefined();
    });

    it('preserves existing headers when adding CSRF token', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(200));
      jest.spyOn(csrfTokenModule, 'getCsrfToken').mockReturnValue('csrf-abc');
      const authFetch = createAuthFetch(jest.fn());

      await authFetch('/api/data', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });

      const [, init] = (global.fetch as jest.Mock).mock.calls[0];
      expect(init.headers.get('X-CSRF-TOKEN')).toBe('csrf-abc');
      expect(init.headers.get('Content-Type')).toBe('application/json');
    });
  });

  describe('request queue integration', () => {
    it('queues the request on 401 and returns the replayed response', async () => {
      const queue = new RequestQueue();
      global.fetch = jest.fn()
        .mockResolvedValueOnce(mockResponse(401))
        .mockResolvedValueOnce(mockResponse(200));
      const onUnauthorized = jest.fn();
      const authFetch = createAuthFetch(onUnauthorized, { queue });

      const resultPromise = authFetch('/api/data');

      await new Promise((r) => setTimeout(r, 0));
      expect(queue.size).toBe(1);
      expect(onUnauthorized).toHaveBeenCalledTimes(1);

      await queue.replayAll();

      const result = await resultPromise;
      expect(result.status).toBe(200);
    });

    it('does not call onUnauthorized on subsequent 401s while re-authenticating', async () => {
      const queue = new RequestQueue();
      global.fetch = jest.fn().mockResolvedValue(mockResponse(401));
      const onUnauthorized = jest.fn();
      const authFetch = createAuthFetch(onUnauthorized, { queue });

      // Suppress unhandled promise rejection warnings
      const promise1 = authFetch('/api/first').catch(() => {});
      await new Promise((r) => setTimeout(r, 0));

      const promise2 = authFetch('/api/second').catch(() => {});
      await new Promise((r) => setTimeout(r, 0));

      expect(onUnauthorized).toHaveBeenCalledTimes(1);
      expect(queue.size).toBe(2);

      queue.rejectAll();
      await Promise.all([promise1, promise2]);
    });

    it('without queue, returns 401 response directly (backward compat)', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(401));
      const onUnauthorized = jest.fn();
      const authFetch = createAuthFetch(onUnauthorized);

      const response = await authFetch('/api/data');

      expect(response.status).toBe(401);
      expect(onUnauthorized).toHaveBeenCalledTimes(1);
    });
  });

  describe('onResponse callback', () => {
    it('calls onResponse on successful responses', async () => {
      const resp = mockResponse(200);
      global.fetch = jest.fn().mockResolvedValue(resp);
      const onResponse = jest.fn();
      const authFetch = createAuthFetch(jest.fn(), { onResponse });

      await authFetch('/api/data');

      expect(onResponse).toHaveBeenCalledWith(resp);
    });

    it('does not call onResponse on 401', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(401));
      const onResponse = jest.fn();
      const authFetch = createAuthFetch(jest.fn(), { onResponse });

      await authFetch('/api/data');

      expect(onResponse).not.toHaveBeenCalled();
    });

    it('does not call onResponse on 4xx/5xx errors', async () => {
      global.fetch = jest.fn().mockResolvedValue(mockResponse(500));
      const onResponse = jest.fn();
      const authFetch = createAuthFetch(jest.fn(), { onResponse });

      await authFetch('/api/data');

      expect(onResponse).not.toHaveBeenCalled();
    });
  });
});
