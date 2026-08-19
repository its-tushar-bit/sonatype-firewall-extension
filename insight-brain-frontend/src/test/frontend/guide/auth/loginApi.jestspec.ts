/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { fetchSession } from 'GuideRoot/auth/loginApi';

class MockHeaders {
  private map: Record<string, string>;

  constructor(init?: Record<string, string>) {
    this.map = {};
    if (init) {
      for (const [key, value] of Object.entries(init)) {
        this.map[key.toLowerCase()] = value;
      }
    }
  }

  get(name: string): string | null {
    return this.map[name.toLowerCase()] ?? null;
  }
}

function mockResponse(body: string | null, init: { status: number; headers?: Record<string, string> }) {
  return {
    status: init.status,
    ok: init.status >= 200 && init.status < 300,
    headers: new MockHeaders(init.headers),
    json: () => Promise.resolve(body ? JSON.parse(body) : null),
    text: () => Promise.resolve(body ?? ''),
  };
}

describe('loginApi', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
  });

  describe('fetchSession', () => {
    it('returns authenticated session data on 200', async () => {
      global.fetch = jest.fn().mockResolvedValue(
        mockResponse(
          JSON.stringify({
            authenticated: true,
            username: 'admin',
            displayName: 'Administrator',
            internalUser: true,
            groups: ['Administrators'],
            sessionTimeoutMilliseconds: 1800000,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        )
      );

      const result = await fetchSession();

      expect(result).toEqual({
        authenticated: true,
        user: { username: 'admin', displayName: 'Administrator', groups: ['Administrators'] },
        sessionTimeoutMs: 1800000,
      });
      expect(global.fetch).toHaveBeenCalledWith('/rest/user/session', { credentials: 'same-origin' });
    });

    it('returns unauthenticated on 401', async () => {
      global.fetch = jest.fn().mockResolvedValue(
        mockResponse('Unauthorized', {
          status: 401,
          headers: { 'Content-Type': 'text/plain' },
        })
      );

      const result = await fetchSession();

      expect(result).toEqual({
        authenticated: false,
        user: null,
        sessionTimeoutMs: null,
      });
    });

    it('returns unauthenticated on 200 with authenticated=false', async () => {
      global.fetch = jest.fn().mockResolvedValue(
        mockResponse(
          JSON.stringify({ authenticated: false, username: null }),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        )
      );

      const result = await fetchSession();

      expect(result).toEqual({
        authenticated: false,
        user: null,
        sessionTimeoutMs: null,
      });
    });

    it('throws on server error (500)', async () => {
      global.fetch = jest.fn().mockResolvedValue(
        mockResponse(null, { status: 500 })
      );

      await expect(fetchSession()).rejects.toThrow('Session check failed (500)');
    });

    it('throws on network error', async () => {
      global.fetch = jest.fn().mockRejectedValue(new TypeError('Failed to fetch'));

      await expect(fetchSession()).rejects.toThrow('Failed to fetch');
    });
  });
});
