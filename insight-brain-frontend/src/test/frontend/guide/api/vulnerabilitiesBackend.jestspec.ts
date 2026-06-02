/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  searchVulnerabilities,
  getVulnerabilityDetails,
  getVulnerabilityAffectedComponents,
  filterVulnerabilities,
  computeVulnerabilityAggregations,
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
  // that still use mock data — getVulnerabilityDetails, getVulnerabilityAffectedComponents, etc.).
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
      expect(path).toBe(`/api/v2/guide/vulnerabilities?${params.toString()}`);
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
      expect(path).toBe('/api/v2/guide/vulnerabilities?affectedEcosystems=maven&affectedEcosystems=npm');
    });

    it('forwards an empty searchParams as a bare query string', async () => {
      mockApiFetch.mockResolvedValue({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} });

      await searchVulnerabilities(new URLSearchParams());

      const [path] = mockApiFetch.mock.calls[0];
      expect(path).toBe('/api/v2/guide/vulnerabilities?');
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

  describe('getVulnerabilityDetails', () => {
    it('returns null for an empty vulnId', async () => {
      const result = await getVulnerabilityDetails('');
      expect(result).toBeNull();
    });

    it('returns mock vulnerability detail for a known CVE', async () => {
      const result = await getVulnerabilityDetails('CVE-2021-44228');
      expect(result).not.toBeNull();
      expect(result!.vulnId).toBe('CVE-2021-44228');
    });
  });

  describe('getVulnerabilityAffectedComponents', () => {
    it('returns null for an empty vulnId', async () => {
      const result = await getVulnerabilityAffectedComponents('');
      expect(result).toBeNull();
    });

    it('returns paginated affected components for a known CVE', async () => {
      const result = await getVulnerabilityAffectedComponents('CVE-2021-44228');
      expect(result).not.toBeNull();
      expect(result!.hits).toBeDefined();
      expect(typeof result!.total).toBe('number');
    });
  });
});
