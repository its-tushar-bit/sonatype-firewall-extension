/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  searchComponents,
  getComponentDetail,
  getComponentVulnerabilities,
  getComponentVersions,
  getComponentDependencies,
} from 'GuideRoot/api/componentsBackend';
jest.mock('GuideRoot/api/apiFetch', () => ({
  ...jest.requireActual('GuideRoot/api/apiFetch'),
  apiFetch: jest.fn(),
}));

import { apiFetch } from 'GuideRoot/api/apiFetch';

const mockApiFetch = apiFetch as jest.MockedFunction<typeof apiFetch>;

beforeEach(() => {
  // Default: delegate to mockHandler when present (preserves behavior for endpoints
  // that still use mock data — getComponentDetail, getComponentVulnerabilities, etc.).
  // Searchable endpoints in this file override this per-test.
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

  describe('getComponentDetail', () => {
    it('returns mock component details', async () => {
      const result = await getComponentDetail('npm', 'lodash', '4.17.21');
      expect(result).not.toBeNull();
      expect(result!.name).toBe('lodash');
      expect(result!.version).toBe('4.17.21');
      expect(result!.format).toBe('npm');
    });
  });

  describe('getComponentVulnerabilities', () => {
    it('returns vulnerability search response', async () => {
      const result = await getComponentVulnerabilities('npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 25 });
      expect(result.hits).toBeDefined();
      expect(typeof result.total).toBe('number');
      expect(result.offset).toBe(0);
    });

    it('respects limit:1 for count-only queries', async () => {
      const result = await getComponentVulnerabilities('npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 1 });
      expect(result.hits.length).toBeLessThanOrEqual(1);
      expect(result.total).toBeGreaterThan(0);
    });

    it('filters by query text', async () => {
      const result = await getComponentVulnerabilities('npm', 'lodash', '4.17.21', 'command injection', {}, { offset: 0, limit: 25 });
      expect(result.hits.length).toBe(1);
      expect(result.hits[0].vulnId).toBe('CVE-2021-23337');
    });
  });

  describe('getComponentVersions', () => {
    it('returns versions search response', async () => {
      const result = await getComponentVersions('npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 25 });
      expect(result.hits).toBeDefined();
      expect(result.hits.length).toBeGreaterThan(0);
      expect(result.hits[0].format).toBe('npm');
    });

    it('filters by query text (version string)', async () => {
      const result = await getComponentVersions('npm', 'lodash', '4.17.21', '4.17', {}, { offset: 0, limit: 25 });
      expect(result.hits.every((v) => v.version.includes('4.17'))).toBe(true);
      expect(result.hits.length).toBeGreaterThan(0);
    });
  });

  describe('getComponentDependencies', () => {
    it('returns dependencies search response', async () => {
      const result = await getComponentDependencies('npm', 'lodash', '4.17.21', undefined, {}, { offset: 0, limit: 25 });
      expect(result.hits).toBeDefined();
      expect(typeof result.total).toBe('number');
    });

    it('filters by query text', async () => {
      const result = await getComponentDependencies('npm', 'lodash', '4.17.21', 'underscore', {}, { offset: 0, limit: 25 });
      expect(result.hits.length).toBe(1);
      expect(result.hits[0].name).toBe('underscore');
    });
  });
});
