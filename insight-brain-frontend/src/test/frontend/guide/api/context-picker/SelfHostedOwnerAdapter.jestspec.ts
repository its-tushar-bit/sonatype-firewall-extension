/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { SelfHostedOwnerAdapter } from 'GuideRoot/api/context-picker/SelfHostedOwnerAdapter';

/**
 * Endpoint-wiring tests for {@link SelfHostedOwnerAdapter}: URL/param construction, the
 * `organization`/`application` → `org`/`app` type mapping, the two-array search fold, adapter-
 * owned AbortController cancellation, and 404 → null in resolveOwner. The Guide SPA uses the
 * native-fetch `apiFetch` (no axios), so the HTTP layer is exercised by mocking `global.fetch`.
 */

interface MockResponseInit {
  ok?: boolean;
  status?: number;
}

function jsonResponse(data: unknown, { ok = true, status = 200 }: MockResponseInit = {}) {
  return {
    ok,
    status,
    statusText: ok ? 'OK' : 'Error',
    headers: new Headers(),
    json: async () => data,
  } as Response;
}

describe('SelfHostedOwnerAdapter', () => {
  const originalFetch = global.fetch;
  let adapter: SelfHostedOwnerAdapter;

  beforeEach(() => {
    adapter = new SelfHostedOwnerAdapter();
  });

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  function mockFetchOnce(data: unknown, init?: MockResponseInit) {
    const fetchMock = jest.fn().mockResolvedValue(jsonResponse(data, init));
    global.fetch = fetchMock as unknown as typeof fetch;
    return fetchMock;
  }

  describe('getTopOrgs', () => {
    it('requests top-orgs with the limit and maps organization → org', async () => {
      const fetchMock = mockFetchOnce({
        orgs: [
          {
            id: 'payments',
            publicId: 'payments',
            name: 'Payments',
            type: 'organization',
            ancestorPath: [],
            appCount: 2,
          },
          {
            id: 'frontend',
            publicId: 'frontend',
            name: 'Frontend',
            type: 'organization',
            ancestorPath: [{ id: 'payments', name: 'Payments', type: 'organization' }],
            appCount: 3,
          },
        ],
        totalOrgCount: 22,
      });

      const result = await adapter.getTopOrgs(20);

      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(fetchMock.mock.calls[0][0]).toBe('/api/v2/policy-context/owners/top-orgs?limit=20');
      expect(result.totalOrgCount).toBe(22);
      expect(result.orgs).toHaveLength(2);
      expect(result.orgs[0]).toMatchObject({ id: 'payments', type: 'org', appCount: 2 });
      expect(result.orgs[1].ancestorPath).toEqual([{ id: 'payments', name: 'Payments', type: 'org' }]);
    });
  });

  describe('getAppsForOrg', () => {
    it('requests apps under the org, encodes the id, and maps application → app', async () => {
      const fetchMock = mockFetchOnce({
        apps: [
          {
            id: 'app-checkout',
            publicId: 'checkout-app',
            name: 'checkout-app',
            type: 'application',
            ancestorPath: [
              { id: 'payments', name: 'Payments', type: 'organization' },
              { id: 'frontend', name: 'Frontend', type: 'organization' },
            ],
          },
        ],
        truncated: true,
      });

      const result = await adapter.getAppsForOrg('front end', 500);

      expect(fetchMock.mock.calls[0][0]).toBe(
        '/api/v2/policy-context/owners/orgs/front%20end/apps?limit=500'
      );
      expect(result.truncated).toBe(true);
      expect(result.apps[0]).toMatchObject({ id: 'app-checkout', publicId: 'checkout-app', type: 'app' });
      expect(result.apps[0].ancestorPath).toEqual([
        { id: 'payments', name: 'Payments', type: 'org' },
        { id: 'frontend', name: 'Frontend', type: 'org' },
      ]);
    });
  });

  describe('searchOwners', () => {
    it('folds the orgs + apps arrays into a flat result and ORs the truncation flags', async () => {
      const fetchMock = mockFetchOnce({
        orgs: [
          { id: 'payments', publicId: 'payments', name: 'Payments', type: 'organization', ancestorPath: [], appCount: 2 },
        ],
        orgsTruncated: false,
        apps: [
          { id: 'app-pay', publicId: 'payment-service', name: 'payment-service', type: 'application', ancestorPath: [] },
        ],
        appsTruncated: true,
      });

      const result = await adapter.searchOwners('pay', 'all', 10);

      const url = fetchMock.mock.calls[0][0] as string;
      expect(url).toContain('/api/v2/policy-context/owners/search?');
      expect(url).toContain('query=pay');
      expect(url).toContain('type=all');
      expect(url).toContain('limit=10');
      expect(result.truncated).toBe(true);
      expect(result.results.map((o) => o.type)).toEqual(['org', 'app']);
    });

    it('aborts the in-flight request when a new search starts', async () => {
      const signals: (AbortSignal | undefined)[] = [];
      global.fetch = jest.fn((_url: string, init?: RequestInit) => {
        signals.push(init?.signal ?? undefined);
        // First call never settles so it stays in-flight until the second call aborts it.
        return signals.length === 1
          ? new Promise<Response>(() => {})
          : Promise.resolve(
              jsonResponse({ orgs: [], orgsTruncated: false, apps: [], appsTruncated: false })
            );
      }) as unknown as typeof fetch;

      const first = adapter.searchOwners('log', 'all', 10);
      first.catch(() => {}); // avoid unhandled-rejection noise if it later rejects
      await adapter.searchOwners('log4j', 'all', 10);

      expect(signals[0]?.aborted).toBe(true);
      expect(signals[1]?.aborted).toBe(false);
    });
  });

  describe('resolveOwner', () => {
    it('maps a resolved owner', async () => {
      mockFetchOnce({
        id: 'app-checkout',
        publicId: 'checkout-app',
        name: 'checkout-app',
        type: 'application',
        ancestorPath: [{ id: 'payments', name: 'Payments', type: 'organization' }],
      });

      const owner = await adapter.resolveOwner('checkout-app');

      expect(owner).toMatchObject({ id: 'app-checkout', publicId: 'checkout-app', type: 'app' });
      expect(owner?.ancestorPath).toEqual([{ id: 'payments', name: 'Payments', type: 'org' }]);
    });

    it('returns null on 404 (not found or lost permission)', async () => {
      mockFetchOnce({ message: 'Owner not found' }, { ok: false, status: 404 });

      await expect(adapter.resolveOwner('gone')).resolves.toBeNull();
    });

    it('rethrows non-404 errors', async () => {
      mockFetchOnce({ message: 'boom' }, { ok: false, status: 500 });

      await expect(adapter.resolveOwner('x')).rejects.toThrow();
    });

    it('warns and falls back to app for an unrecognised owner type', async () => {
      const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
      mockFetchOnce({
        id: 'weird',
        publicId: 'weird',
        name: 'Weird Owner',
        type: 'workspace', // not organization/application
        ancestorPath: [],
      });

      const owner = await adapter.resolveOwner('weird');

      expect(owner).toMatchObject({ id: 'weird', type: 'app' });
      expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('workspace'));
    });
  });
});
