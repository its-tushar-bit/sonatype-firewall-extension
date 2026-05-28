/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/guide/test-utils';
import { useLicensedProducts } from 'GuideRoot/layout/ProductSwitcher/useLicensedProducts';

const LICENSED_URL = '/api/v2/solutions/licensed?allowRelativeUrls=true';
const originalFetch = global.fetch;

function HookProbe() {
  const { products, loading, error } = useLicensedProducts();
  return (
    <div>
      <div data-testid="loading">{String(loading)}</div>
      <div data-testid="error">{String(error)}</div>
      <ul data-testid="products">
        {products.map((p) => (
          <li key={p.id}>{p.displayName}</li>
        ))}
      </ul>
    </div>
  );
}

function mockFetchOnce(response: { ok: boolean; status?: number; body?: unknown }) {
  // Note: AuthProvider's session probe also uses fetch — we let the test-utils default
  // mock handle that and only intercept the licensed-solutions URL specifically.
  global.fetch = jest.fn((input: RequestInfo | URL) => {
    const url = typeof input === 'string' ? input : input.toString();
    if (url.includes('/api/v2/solutions/licensed')) {
      return Promise.resolve({
        ok: response.ok,
        status: response.status ?? (response.ok ? 200 : 500),
        headers: new Headers(),
        json: () => Promise.resolve(response.body),
      } as unknown as Response);
    }
    return Promise.resolve({ ok: true, status: 200, headers: new Headers() } as Response);
  }) as unknown as typeof fetch;
}

describe('useLicensedProducts', () => {
  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  it('starts in loading state and fetches the licensed-solutions endpoint once', async () => {
    mockFetchOnce({ ok: true, body: [] });
    render(<HookProbe />);

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    const calls = (global.fetch as jest.Mock).mock.calls.filter((c) => {
      const url = typeof c[0] === 'string' ? c[0] : String(c[0]);
      return url.includes('/api/v2/solutions/licensed');
    });
    expect(calls).toHaveLength(1);
    expect(calls[0][0]).toBe(LICENSED_URL);
  });

  it('parses, groups, and sorts the response on success', async () => {
    mockFetchOnce({
      ok: true,
      body: [
        { id: 'sbom', url: '/sbom' },
        { id: 'lifecycle', url: '/lifecycle' },
      ],
    });
    render(<HookProbe />);

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    const items = screen.getAllByRole('listitem').map((el) => el.textContent);
    expect(items).toEqual(['Lifecycle', 'SBOM Manager']);
    expect(screen.getByTestId('error')).toHaveTextContent('false');
  });

  it('groups multi-instance entries into one product', async () => {
    mockFetchOnce({
      ok: true,
      body: [
        { id: 'nexusRepositoryManager', url: '/east' },
        { id: 'nexusRepositoryManager', url: '/west' },
      ],
    });
    render(<HookProbe />);

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    const items = screen.getAllByRole('listitem').map((el) => el.textContent);
    expect(items).toEqual(['Nexus Repository']);
  });

  it('returns error=true and products=[] when the request fails', async () => {
    mockFetchOnce({ ok: false, status: 500, body: { message: 'oops' } });
    render(<HookProbe />);

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('error')).toHaveTextContent('true');
    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
  });

  it('returns error=true and products=[] when fetch rejects', async () => {
    global.fetch = jest.fn((input: RequestInfo | URL) => {
      const url = typeof input === 'string' ? input : input.toString();
      if (url.includes('/api/v2/solutions/licensed')) {
        return Promise.reject(new Error('network down'));
      }
      return Promise.resolve({ ok: true, status: 200, headers: new Headers() } as Response);
    }) as unknown as typeof fetch;

    render(<HookProbe />);

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('error')).toHaveTextContent('true');
  });
});
