/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

jest.mock('GuideRoot/auth/csrfToken', () => ({
  getCsrfToken: jest.fn(),
}));

import { apiFetch } from 'GuideRoot/api/apiFetch';
import { getCsrfToken } from 'GuideRoot/auth/csrfToken';

const mockGetCsrfToken = getCsrfToken as jest.MockedFunction<typeof getCsrfToken>;

const realFetch = global.fetch;
const mockFetch = jest.fn();

beforeAll(() => {
  global.fetch = mockFetch as unknown as typeof global.fetch;
});

afterAll(() => {
  global.fetch = realFetch;
});

beforeEach(() => {
  mockFetch.mockReset();
  mockGetCsrfToken.mockReset();
  mockFetch.mockResolvedValue({
    ok: true,
    status: 200,
    statusText: 'OK',
    json: async () => ({}),
  });
});

function getRequestHeaders(): Headers | undefined {
  const init = mockFetch.mock.calls[0][1] as RequestInit | undefined;
  if (!init?.headers) return undefined;
  return init.headers instanceof Headers ? init.headers : new Headers(init.headers as HeadersInit);
}

describe('apiFetch CSRF handling', () => {
  it('injects X-CSRF-TOKEN on POST when a token is present', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-abc-123');

    await apiFetch('/api/v2/guide/recommendations', { method: 'POST' });

    const headers = getRequestHeaders();
    expect(headers?.get('X-CSRF-TOKEN')).toBe('csrf-abc-123');
  });

  it('does not inject X-CSRF-TOKEN on POST when no token is available', async () => {
    mockGetCsrfToken.mockReturnValue(undefined);

    await apiFetch('/api/v2/guide/recommendations', { method: 'POST' });

    const headers = getRequestHeaders();
    expect(headers?.get('X-CSRF-TOKEN') ?? null).toBeNull();
  });

  it('does not inject X-CSRF-TOKEN on GET even when a token is present', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-abc-123');

    await apiFetch('/api/v2/guide/components/detail');

    const headers = getRequestHeaders();
    expect(headers?.get('X-CSRF-TOKEN') ?? null).toBeNull();
  });

  it('does not inject X-CSRF-TOKEN on HEAD or OPTIONS', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-abc-123');

    await apiFetch('/api/v2/guide/x', { method: 'HEAD' });
    expect(getRequestHeaders()?.get('X-CSRF-TOKEN') ?? null).toBeNull();

    mockFetch.mockClear();
    await apiFetch('/api/v2/guide/x', { method: 'OPTIONS' });
    expect(getRequestHeaders()?.get('X-CSRF-TOKEN') ?? null).toBeNull();
  });

  it('injects X-CSRF-TOKEN on PUT, PATCH, and DELETE', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-xyz');

    for (const method of ['PUT', 'PATCH', 'DELETE'] as const) {
      mockFetch.mockClear();
      await apiFetch('/api/v2/guide/x', { method });
      expect(getRequestHeaders()?.get('X-CSRF-TOKEN')).toBe('csrf-xyz');
    }
  });

  it('treats lowercase methods the same as uppercase (so post still injects the header)', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-abc');

    await apiFetch('/api/v2/guide/x', { method: 'post' });

    expect(getRequestHeaders()?.get('X-CSRF-TOKEN')).toBe('csrf-abc');
  });

  it('preserves caller-supplied headers when injecting the CSRF header', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-abc');

    await apiFetch('/api/v2/guide/x', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Custom': 'keep-me' },
    });

    const headers = getRequestHeaders();
    expect(headers?.get('Content-Type')).toBe('application/json');
    expect(headers?.get('X-Custom')).toBe('keep-me');
    expect(headers?.get('X-CSRF-TOKEN')).toBe('csrf-abc');
  });
});
