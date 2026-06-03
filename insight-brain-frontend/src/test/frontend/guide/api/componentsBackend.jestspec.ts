/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  searchComponents,
  fetchComponentBrowseAggregations,
  getComponentDetail,
  getComponentVulnerabilities,
  getComponentVersions,
  getComponentDependencies,
  _resetBrowseAggregationsCacheForTests,
} from 'GuideRoot/api/componentsBackend';
jest.mock('GuideRoot/api/apiFetch', () => ({
  ...jest.requireActual('GuideRoot/api/apiFetch'),
  apiFetch: jest.fn(),
}));

import { apiFetch } from 'GuideRoot/api/apiFetch';

const mockApiFetch = apiFetch as jest.MockedFunction<typeof apiFetch>;

beforeEach(() => {
  // Default: throw if no mockHandler present. All wired endpoints override per-test
  // with mockApiFetch.mockResolvedValue(...). This catches accidental mock handler usage.
  mockApiFetch.mockImplementation(async <T>(_path: string, init?: { mockHandler?: () => unknown }): Promise<T> => {
    if (init?.mockHandler) return init.mockHandler() as T;
    throw new Error('No mock handler — real API not available in tests');
  });
});

describe('componentsBackend', () => {
  describe('searchComponents (wired)', () => {
    it('calls the components search endpoint with searchParams.toString() appended', async () => {
      const fakeResponse = {
        hits: [],
        total: 0,
        offset: 0,
        limit: 25,
        aggregations: {},
      };
      mockApiFetch.mockResolvedValue(fakeResponse);

      const params = new URLSearchParams();
      params.set('query', 'lodash');
      params.set('limit', '25');
      params.append('formats', 'maven');
      params.append('formats', 'npm');

      const result = await searchComponents(params);

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toBe(`/api/v2/guide/components/search?${params.toString()}`);
      expect(init).toBeUndefined();
      expect(result).toBe(fakeResponse);
    });

    it('forwards repeated array params verbatim (preserves ordering)', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });

      const params = new URLSearchParams();
      params.append('severities', 'critical');
      params.append('severities', 'high');

      await searchComponents(params);

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/components/search?severities=critical&severities=high');
    });

    it('forwards an empty searchParams as a bare query string', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });

      await searchComponents(new URLSearchParams());

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/components/search?');
    });
  });

  describe('fetchComponentBrowseAggregations (memoized)', () => {
    const browseResponse = {
      hits: [],
      total: 0,
      offset: 0,
      limit: 1,
      aggregations: { byFormat: { maven: 1, npm: 1 } },
    };

    beforeEach(() => {
      _resetBrowseAggregationsCacheForTests();
    });

    it('issues exactly one fetch when called twice within the TTL', async () => {
      mockApiFetch.mockResolvedValue(browseResponse);

      const a = await fetchComponentBrowseAggregations();
      const b = await fetchComponentBrowseAggregations();

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      expect(a).toEqual({ byFormat: { maven: 1, npm: 1 } });
      expect(b).toEqual({ byFormat: { maven: 1, npm: 1 } });
    });

    it('hits the browse endpoint with limit=1 and no filters', async () => {
      mockApiFetch.mockResolvedValue(browseResponse);

      await fetchComponentBrowseAggregations();

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/components/search?limit=1');
    });

    it('returns the same in-flight promise to concurrent callers', () => {
      mockApiFetch.mockReturnValue(new Promise(() => {}));

      const first = fetchComponentBrowseAggregations();
      const second = fetchComponentBrowseAggregations();

      expect(first).toBe(second);
      expect(mockApiFetch).toHaveBeenCalledTimes(1);
    });

    it('refetches once the 10-minute TTL has expired', async () => {
      mockApiFetch.mockResolvedValue(browseResponse);
      const realNow = Date.now;
      let now = 1_000_000;
      Date.now = () => now;
      try {
        await fetchComponentBrowseAggregations();
        expect(mockApiFetch).toHaveBeenCalledTimes(1);

        now += 9 * 60 * 1000;
        await fetchComponentBrowseAggregations();
        expect(mockApiFetch).toHaveBeenCalledTimes(1);

        now += 2 * 60 * 1000;
        await fetchComponentBrowseAggregations();
        expect(mockApiFetch).toHaveBeenCalledTimes(2);
      } finally {
        Date.now = realNow;
      }
    });

    it('returns null on error and retries on the next call (does not poison the cache)', async () => {
      mockApiFetch.mockRejectedValueOnce(new Error('boom'));
      mockApiFetch.mockResolvedValueOnce(browseResponse);

      const first = await fetchComponentBrowseAggregations();
      expect(first).toBeNull();

      const second = await fetchComponentBrowseAggregations();
      expect(second).toEqual({ byFormat: { maven: 1, npm: 1 } });
      expect(mockApiFetch).toHaveBeenCalledTimes(2);
    });
  });

  describe('getComponentDetail (wired)', () => {
    it('calls the detail endpoint with a PURL query param for a namespaced package', async () => {
      const fakeDetail = { format: 'maven', name: 'spring-core', version: '5.3.0' };
      mockApiFetch.mockResolvedValue(fakeDetail);

      const result = await getComponentDetail('maven', 'org.springframework:spring-core', '5.3.0');

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toBe(
        '/api/v2/guide/components/detail?purl=pkg%3Amaven%2Forg.springframework%2Fspring-core%405.3.0'
      );
      expect(init).toBeUndefined();
      expect(result).toBe(fakeDetail);
    });

    it('calls the detail endpoint with a PURL for a non-namespaced package', async () => {
      mockApiFetch.mockResolvedValue({ format: 'npm', name: 'lodash', version: '4.17.21' });

      await getComponentDetail('npm', 'lodash', '4.17.21');

      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toBe(
        '/api/v2/guide/components/detail?purl=pkg%3Anpm%2Flodash%404.17.21'
      );
      expect(init).toBeUndefined();
    });

    it('lowercases the format in the PURL', async () => {
      mockApiFetch.mockResolvedValue({ format: 'maven', name: 'guava', version: '31.0' });

      await getComponentDetail('Maven', 'com.google.guava:guava', '31.0');

      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toContain('purl=pkg%3Amaven%2F');
      expect(init).toBeUndefined();
    });

    it('does not double-encode scoped npm packages in the PURL', async () => {
      mockApiFetch.mockResolvedValue({ format: 'npm', name: 'name', version: '1.0.0' });

      await getComponentDetail('npm', '@scope/name', '1.0.0');

      const [path] = mockApiFetch.mock.calls[0];
      // URLSearchParams encodes @ → %40 and / → %2F once; no double-encoding (%2540)
      expect(path).toContain('pkg%3Anpm%2F%40scope%2Fname%401.0.0');
      expect(path).not.toContain('%2540');
    });

    it('returns null when the backend returns 404', async () => {
      const { ApiError } = jest.requireActual<typeof import('GuideRoot/api/apiFetch')>('GuideRoot/api/apiFetch');
      mockApiFetch.mockRejectedValue(new ApiError('not found', 404, 'Not Found'));

      const result = await getComponentDetail('npm', 'no-such-package', '1.0.0');

      expect(result).toBeNull();
    });

    it('rethrows non-404 errors', async () => {
      const { ApiError } = jest.requireActual<typeof import('GuideRoot/api/apiFetch')>('GuideRoot/api/apiFetch');
      mockApiFetch.mockRejectedValue(new ApiError('server error', 500, 'Internal Server Error'));

      await expect(getComponentDetail('npm', 'lodash', '4.17.21')).rejects.toThrow('server error');
    });
  });

  describe('getComponentVulnerabilities (unwired — 404 until GUIDE-2606)', () => {
    it('calls apiFetch with the component vulnerabilities path and no mock handler', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });

      await getComponentVulnerabilities('npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 25 });

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toContain('/api/v2/guide/components/');
      expect(path).toContain('vulnerabilities');
      expect(init).toBeUndefined();
    });
  });

  describe('getComponentVersions (wired)', () => {
    const fakeVersionsResponse = {
      hits: [],
      total: 0,
      offset: 0,
      limit: 25,
      aggregations: {},
    };

    it('calls versions endpoint with PURL and default pagination', async () => {
      mockApiFetch.mockResolvedValue(fakeVersionsResponse);

      await getComponentVersions('npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 25 });

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toBe(
        '/api/v2/guide/components/versions?purl=pkg%3Anpm%2Flodash%404.17.21&offset=0&limit=25'
      );
      expect(init).toBeUndefined();
    });

    it('appends optional query text as versionQuery param', async () => {
      mockApiFetch.mockResolvedValue(fakeVersionsResponse);

      await getComponentVersions('npm', 'lodash', '4.17.21', '4.17', {}, { offset: 0, limit: 25 });

      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toContain('versionQuery=4.17');
      expect(init).toBeUndefined();
    });

    it('appends sort params when provided', async () => {
      mockApiFetch.mockResolvedValue(fakeVersionsResponse);

      await getComponentVersions('npm', 'lodash', '4.17.21', undefined, {}, {
        offset: 0,
        limit: 10,
        sortField: 'versionScore',
        sortOrder: 'desc',
      });

      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toContain('sortField=versionScore');
      expect(path).toContain('sortOrder=desc');
      expect(init).toBeUndefined();
    });

    it('appends filter params when provided', async () => {
      mockApiFetch.mockResolvedValue(fakeVersionsResponse);

      await getComponentVersions(
        'npm',
        'lodash',
        '4.17.21',
        undefined,
        {
          isStable: true,
          hasMalware: false,
          severities: ['critical', 'high'],
          minVersionScore: 10,
          maxVersionScore: 90,
          publishedWindow: '30d',
        },
        { offset: 0, limit: 25 }
      );

      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toContain('isStable=true');
      expect(path).toContain('hasMalware=false');
      expect(path).toContain('severities=critical');
      expect(path).toContain('severities=high');
      expect(path).toContain('minVersionScore=10');
      expect(path).toContain('maxVersionScore=90');
      expect(path).toContain('publishedWindow=30d');
      expect(init).toBeUndefined();
    });

    it('does not append undefined filter params', async () => {
      mockApiFetch.mockResolvedValue(fakeVersionsResponse);

      await getComponentVersions('npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 25 });

      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).not.toContain('isStable');
      expect(path).not.toContain('hasMalware');
      expect(path).not.toContain('sortField');
      expect(path).not.toContain('versionQuery');
      expect(init).toBeUndefined();
    });
  });

  describe('getComponentDependencies (unwired — 404 until GUIDE-2606)', () => {
    it('calls apiFetch with the component dependencies path and no mock handler', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });

      await getComponentDependencies('npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 25 });

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toContain('/api/v2/guide/components/');
      expect(path).toContain('dependencies');
      expect(init).toBeUndefined();
    });
  });
});
