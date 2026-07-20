/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

jest.mock('GuideRoot/auth/csrfToken', () => ({
  getCsrfToken: jest.fn(),
}));

import { apiFetch, ApiError, GuideLicenseRevokedError } from 'GuideRoot/api/apiFetch';
import { getCsrfToken } from 'GuideRoot/auth/csrfToken';
import {
  setLicenseRevocationHandler,
  GUIDE_LICENSE_HEADER,
  GUIDE_LICENSE_UNAVAILABLE,
} from 'GuideRoot/license/licenseRevocation';
import { setOwnerScope, _resetOwnerScopeForTests } from 'GuideRoot/api/ownerScope';

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

afterEach(() => _resetOwnerScopeForTests());

function getRequestUrl(): string {
  return mockFetch.mock.calls[0][0] as string;
}

describe('apiFetch ownerId injection', () => {
  it('appends ownerId to a Guide path that already has a query string', async () => {
    setOwnerScope('payments');
    await apiFetch('/api/v2/guide/components/search?limit=25&sortField=trending');
    expect(getRequestUrl()).toBe(
      '/api/v2/guide/components/search?limit=25&sortField=trending&ownerId=payments'
    );
  });

  it('appends ownerId with a ? to a Guide path that has no query string', async () => {
    setOwnerScope('payments');
    await apiFetch('/api/v2/guide/recommendations', { method: 'POST' });
    expect(getRequestUrl()).toBe('/api/v2/guide/recommendations?ownerId=payments');
  });

  it('url-encodes the ownerId', async () => {
    setOwnerScope('org/with space');
    await apiFetch('/api/v2/guide/components/detail');
    expect(getRequestUrl()).toBe('/api/v2/guide/components/detail?ownerId=org%2Fwith%20space');
  });

  it('does NOT append ownerId when the scope is null (root)', async () => {
    await apiFetch('/api/v2/guide/components/search?limit=25');
    expect(getRequestUrl()).toBe('/api/v2/guide/components/search?limit=25');
  });

  it('does NOT append ownerId to non-Guide paths (e.g. the picker owner endpoints)', async () => {
    setOwnerScope('payments');
    await apiFetch('/api/v2/policy-context/owners/payments');
    expect(getRequestUrl()).toBe('/api/v2/policy-context/owners/payments');
  });

  it('does not mangle an already-percent-encoded query string', async () => {
    setOwnerScope('payments');
    await apiFetch('/api/v2/guide/components/detail?purl=pkg%3Anpm%2Freact%40%40types');
    expect(getRequestUrl()).toBe(
      '/api/v2/guide/components/detail?purl=pkg%3Anpm%2Freact%40%40types&ownerId=payments'
    );
  });
});

function getRequestHeaders(): Headers | undefined {
  const init = mockFetch.mock.calls[0][1] as RequestInit | undefined;
  if (!init?.headers) return undefined;
  return init.headers instanceof Headers ? init.headers : new Headers(init.headers as HeadersInit);
}

describe('apiFetch UI marker header', () => {
  it('sends the X-Guide-Client: ui header on Guide API GET calls', async () => {
    await apiFetch('/api/v2/guide/components/detail?purl=pkg:npm/x@1');

    expect(getRequestHeaders()?.get('X-Guide-Client')).toBe('ui');
  });

  it('sends the X-Guide-Client: ui header on unsafe (POST) calls alongside the CSRF token', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-abc');

    await apiFetch('/api/v2/guide/recommendations', { method: 'POST' });

    const headers = getRequestHeaders();
    expect(headers?.get('X-Guide-Client')).toBe('ui');
    expect(headers?.get('X-CSRF-TOKEN')).toBe('csrf-abc');
  });
});

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

describe('apiFetch Guide license-revocation handling', () => {
  let revocationHandler: jest.Mock;

  beforeEach(() => {
    revocationHandler = jest.fn();
    setLicenseRevocationHandler(revocationHandler);
  });

  afterEach(() => {
    setLicenseRevocationHandler(null);
  });

  function errorResponse(
    status: number,
    statusText: string,
    { marker, body }: { marker?: boolean; body?: unknown } = {}
  ): Response {
    const headers = new Headers();
    if (marker) headers.set(GUIDE_LICENSE_HEADER, GUIDE_LICENSE_UNAVAILABLE);
    return {
      ok: false,
      status,
      statusText,
      headers,
      json: async () => body ?? { success: false, message: `${status} denied` },
    } as unknown as Response;
  }

  it('notifies the revocation handler and throws GuideLicenseRevokedError on a 403 carrying the marker', async () => {
    mockFetch.mockResolvedValue(errorResponse(403, 'Forbidden', { marker: true }));

    const error = await apiFetch('/api/v2/guide/components/detail').catch((e) => e);

    expect(error).toBeInstanceOf(GuideLicenseRevokedError);
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(403);
    expect(revocationHandler).toHaveBeenCalledTimes(1);
  });

  it('also handles the first-call 402 that carries the marker', async () => {
    mockFetch.mockResolvedValue(errorResponse(402, 'Payment Required', { marker: true }));

    const error = await apiFetch('/api/v2/guide/global/search?query=x').catch((e) => e);

    expect(error).toBeInstanceOf(GuideLicenseRevokedError);
    expect(error.status).toBe(402);
    expect(revocationHandler).toHaveBeenCalledTimes(1);
  });

  it('surfaces the backend error-envelope message on the revocation error', async () => {
    mockFetch.mockResolvedValue(
      errorResponse(403, 'Forbidden', {
        marker: true,
        body: { success: false, message: 'Guide API is not available with the current license.' },
      })
    );

    const error = await apiFetch('/api/v2/guide/x').catch((e) => e);

    expect(error.message).toBe('Guide API is not available with the current license.');
  });

  it('does NOT trigger a refresh on a 403 without the marker (e.g. a permission error)', async () => {
    mockFetch.mockResolvedValue(errorResponse(403, 'Forbidden', { marker: false }));

    const error = await apiFetch('/api/v2/guide/x').catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).not.toBeInstanceOf(GuideLicenseRevokedError);
    expect(revocationHandler).not.toHaveBeenCalled();
  });

  it('does not crash or trigger a refresh when an error response has no headers object', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 503,
      statusText: 'Service Unavailable',
      json: async () => {
        throw new Error('not json');
      },
    } as unknown as Response);

    const error = await apiFetch('/api/v2/guide/x').catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).not.toBeInstanceOf(GuideLicenseRevokedError);
    expect(revocationHandler).not.toHaveBeenCalled();
  });
});
