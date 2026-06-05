/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  searchVulnerabilities,
  fetchVulnerabilityBrowseAggregations,
  getVulnerabilityDetails,
  getVulnerabilityAffectedComponents,
  filterVulnerabilities,
  computeVulnerabilityAggregations,
  _resetBrowseAggregationsCacheForTests,
} from 'GuideRoot/api/vulnerabilitiesBackend';
import { getCVSSSeverity } from '@guide/ui-core';

jest.mock('GuideRoot/api/apiFetch', () => ({
  ...jest.requireActual('GuideRoot/api/apiFetch'),
  apiFetch: jest.fn(),
}));

import { apiFetch } from 'GuideRoot/api/apiFetch';

const mockApiFetch = apiFetch as jest.MockedFunction<typeof apiFetch>;

beforeEach(() => {
  // Default: delegate to mockHandler when present (preserves behaviour for endpoints
  // in other backends that still use mock data — see componentsBackend, searchBackend).
  mockApiFetch.mockImplementation(async <T>(_path: string, init?: { mockHandler?: () => unknown }): Promise<T> => {
    if (init?.mockHandler) return init.mockHandler() as T;
    throw new Error('No mock handler — real API not available in tests');
  });
});

describe('vulnerabilitiesBackend', () => {
  describe('searchVulnerabilities (wired)', () => {
    it('calls the vulnerabilities search endpoint with searchParams.toString() appended', async () => {
      const fakeResponse = {
        hits: [],
        total: 0,
        offset: 0,
        limit: 25,
        aggregations: {},
      };
      mockApiFetch.mockResolvedValue(fakeResponse);

      const params = new URLSearchParams();
      params.set('query', 'log4j');
      params.set('limit', '25');
      params.append('severities', 'critical');
      params.append('severities', 'high');

      const result = await searchVulnerabilities(params);

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toBe(`/api/v2/guide/vulnerabilities/search?${params.toString()}`);
      expect(init).toBeUndefined();
      expect(result).toBe(fakeResponse);
    });

    it('forwards repeated array params verbatim (preserves ordering)', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });

      const params = new URLSearchParams();
      params.append('affectedEcosystems', 'maven');
      params.append('affectedEcosystems', 'npm');

      await searchVulnerabilities(params);

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/vulnerabilities/search?affectedEcosystems=maven&affectedEcosystems=npm');
    });

    it('forwards an empty searchParams as a bare query string', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });

      await searchVulnerabilities(new URLSearchParams());

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/vulnerabilities/search?');
    });
  });

  describe('fetchVulnerabilityBrowseAggregations (memoized)', () => {
    const browseResponse = {
      hits: [],
      total: 0,
      offset: 0,
      limit: 1,
      aggregations: { byEcosystem: { maven: 1, npm: 1 } },
    };

    beforeEach(() => {
      _resetBrowseAggregationsCacheForTests();
    });

    it('issues exactly one fetch when called twice within the TTL', async () => {
      mockApiFetch.mockResolvedValue(browseResponse);

      const a = await fetchVulnerabilityBrowseAggregations();
      const b = await fetchVulnerabilityBrowseAggregations();

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      expect(a).toEqual({ byEcosystem: { maven: 1, npm: 1 } });
      expect(b).toEqual({ byEcosystem: { maven: 1, npm: 1 } });
    });

    it('hits the browse endpoint with limit=1 and no filters', async () => {
      mockApiFetch.mockResolvedValue(browseResponse);

      await fetchVulnerabilityBrowseAggregations();

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/vulnerabilities/search?limit=1');
    });

    it('returns the same in-flight promise to concurrent callers', () => {
      mockApiFetch.mockReturnValue(new Promise(() => {})); // never resolves

      const first = fetchVulnerabilityBrowseAggregations();
      const second = fetchVulnerabilityBrowseAggregations();

      expect(first).toBe(second);
      expect(mockApiFetch).toHaveBeenCalledTimes(1);
    });

    it('refetches once the 10-minute TTL has expired', async () => {
      mockApiFetch.mockResolvedValue(browseResponse);
      const realNow = Date.now;
      let now = 1_000_000;
      Date.now = () => now;
      try {
        await fetchVulnerabilityBrowseAggregations();
        expect(mockApiFetch).toHaveBeenCalledTimes(1);

        // Within TTL — still served from cache.
        now += 9 * 60 * 1000;
        await fetchVulnerabilityBrowseAggregations();
        expect(mockApiFetch).toHaveBeenCalledTimes(1);

        // Past TTL — refetch.
        now += 2 * 60 * 1000;
        await fetchVulnerabilityBrowseAggregations();
        expect(mockApiFetch).toHaveBeenCalledTimes(2);
      } finally {
        Date.now = realNow;
      }
    });

    it('returns null on error and retries on the next call (does not poison the cache)', async () => {
      mockApiFetch.mockRejectedValueOnce(new Error('boom'));
      mockApiFetch.mockResolvedValueOnce(browseResponse);

      const first = await fetchVulnerabilityBrowseAggregations();
      expect(first).toBeNull();

      const second = await fetchVulnerabilityBrowseAggregations();
      expect(second).toEqual({ byEcosystem: { maven: 1, npm: 1 } });
      expect(mockApiFetch).toHaveBeenCalledTimes(2);
    });
  });

  describe('filterVulnerabilities', () => {
    it('returns all vulnerabilities when called with no filters', () => {
      const mockVulns = [
        { vulnId: 'CVE-1', affectedEcosystems: ['maven'], cvssSeverity: 9.8, sonatypeCvssSeverity: 9.8, isMalware: false, kev: false },
        { vulnId: 'CVE-2', affectedEcosystems: ['npm'], cvssSeverity: 5.0, sonatypeCvssSeverity: 5.0, isMalware: false, kev: false },
      ] as Parameters<typeof filterVulnerabilities>[0];

      const result = filterVulnerabilities(mockVulns);
      expect(result).toHaveLength(2);
    });

    it('filters by affectedEcosystems', () => {
      const mockVulns = [
        { vulnId: 'CVE-1', affectedEcosystems: ['maven'], cvssSeverity: 9.8, sonatypeCvssSeverity: 9.8, isMalware: false, kev: false },
        { vulnId: 'CVE-2', affectedEcosystems: ['npm'], cvssSeverity: 5.0, sonatypeCvssSeverity: 5.0, isMalware: false, kev: false },
      ] as Parameters<typeof filterVulnerabilities>[0];

      const result = filterVulnerabilities(mockVulns, undefined, { affectedEcosystems: ['maven'] });
      expect(result).toHaveLength(1);
      expect(result[0].vulnId).toBe('CVE-1');
    });

    it('filters by severities using sonatypeCvssSeverity', () => {
      const mockVulns = [
        { vulnId: 'CVE-1', affectedEcosystems: [], cvssSeverity: 9.8, sonatypeCvssSeverity: 9.8, isMalware: false, kev: false },
        { vulnId: 'CVE-2', affectedEcosystems: [], cvssSeverity: 5.0, sonatypeCvssSeverity: 5.0, isMalware: false, kev: false },
      ] as Parameters<typeof filterVulnerabilities>[0];

      const result = filterVulnerabilities(mockVulns, undefined, { severities: ['critical'] });
      expect(result.every((v) => getCVSSSeverity(v.sonatypeCvssSeverity ?? v.cvssSeverity ?? 0) === 'critical')).toBe(true);
    });
  });

  describe('computeVulnerabilityAggregations', () => {
    it('returns aggregation buckets for an empty list', () => {
      const result = computeVulnerabilityAggregations([]);
      expect(result.byEcosystem).toBeDefined();
      expect(result.bySeverity).toBeDefined();
      expect(result.byKev).toBeDefined();
      expect(result.byMalware).toBeDefined();
    });
  });

  describe('getVulnerabilityDetails (wired)', () => {
    it('returns null without calling apiFetch when vulnId is empty', async () => {
      const result = await getVulnerabilityDetails('');
      expect(mockApiFetch).not.toHaveBeenCalled();
      expect(result).toBeNull();
    });

    it('calls the correct endpoint and returns the response', async () => {
      const fakeVuln = { vulnId: 'CVE-2021-44228', cvssSeverity: 10 };
      mockApiFetch.mockResolvedValue(fakeVuln);

      const result = await getVulnerabilityDetails('CVE-2021-44228');

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/vulnerabilities/CVE-2021-44228');
      expect(init).toBeUndefined();
      expect(result).toBe(fakeVuln);
    });

    it('URL-encodes special characters in the vulnId', async () => {
      mockApiFetch.mockResolvedValue(null);

      await getVulnerabilityDetails('CVE 2021 44228');

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/vulnerabilities/CVE%202021%2044228');
    });
  });

  describe('getVulnerabilityAffectedComponents (wired)', () => {
    it('returns null without calling apiFetch when vulnId is empty', async () => {
      const result = await getVulnerabilityAffectedComponents('');
      expect(mockApiFetch).not.toHaveBeenCalled();
      expect(result).toBeNull();
    });

    it('calls the endpoint with no query string when params are omitted', async () => {
      const fakeResponse = { hits: [], total: 0, offset: 0, limit: 50 };
      mockApiFetch.mockResolvedValue(fakeResponse);

      const result = await getVulnerabilityAffectedComponents('CVE-2021-44228');

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
      const [path, init] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/vulnerabilities/CVE-2021-44228/components');
      expect(init).toBeUndefined();
      expect(result).toBe(fakeResponse);
    });

    it('serializes all params into the query string', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 25, limit: 50 });

      await getVulnerabilityAffectedComponents('CVE-2021-44228', {
        query: 'log4j',
        offset: 25,
        limit: 50,
        sortField: 'packageName',
        sortOrder: 'asc',
      });

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe(
        '/api/v2/guide/vulnerabilities/CVE-2021-44228/components?query=log4j&offset=25&limit=50&sortField=packageName&sortOrder=asc'
      );
    });

    it('omits undefined params from the query string', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25 });

      await getVulnerabilityAffectedComponents('CVE-2021-44228', { limit: 25 });

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe(
        '/api/v2/guide/vulnerabilities/CVE-2021-44228/components?limit=25'
      );
    });
  });
});
