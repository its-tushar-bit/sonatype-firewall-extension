/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { searchComponents } from 'GuideRoot/api/componentsBackend';
import { getCVSSSeverity } from '@guide/ui-core';

jest.mock('GuideRoot/api/apiFetch', () => ({
  ...jest.requireActual('GuideRoot/api/apiFetch'),
  apiFetch: async <T>(_path: string, init?: { mockHandler?: () => unknown }): Promise<T> => {
    if (init?.mockHandler) return init.mockHandler() as T;
    throw new Error('No mock handler — real API not available in tests');
  },
}));

describe('componentsBackend', () => {
  describe('searchComponents', () => {
    it('returns full dataset when called with no filters', async () => {
      const result = await searchComponents();

      expect(result.hits).toHaveLength(25); // Default limit
      expect(result.total).toBeGreaterThan(40); // ~50 mock components
      expect(result.offset).toBe(0);
      expect(result.limit).toBe(25);
      expect(result.aggregations).toBeDefined();
      expect(result.aggregations!.byFormat).toBeDefined();
    });

    it('formats filter reduces results and updates aggregation counts', async () => {
      const result = await searchComponents({
        filters: { formats: ['npm'] },
      });

      expect(result.total).toBeLessThan(50);
      expect(result.hits.every((c) => c.format === 'npm')).toBe(true);

      // Aggregations should reflect filtered set
      expect(result.aggregations!.byFormat.npm).toBe(result.total);
    });

    it('severity filter reduces results', async () => {
      const criticalOnly = await searchComponents({
        filters: { severities: ['critical'] },
      });

      // Use ui-core's getCVSSSeverity for consistency
      expect(criticalOnly.hits.every((c) => getCVSSSeverity(c.maxCvss ?? 0) === 'critical')).toBe(true);
      expect(criticalOnly.aggregations!.bySeverity.critical).toBe(criticalOnly.total);
    });

    it('sort changes result ordering', async () => {
      const ascResult = await searchComponents({
        options: { sortField: 'name', sortOrder: 'asc' },
      });

      const descResult = await searchComponents({
        options: { sortField: 'name', sortOrder: 'desc' },
      });

      // First result in asc should not equal first in desc
      expect(ascResult.hits[0].name).not.toBe(descResult.hits[0].name);

      // Verify asc order
      const ascNames = ascResult.hits.map((c) => c.name.toLowerCase());
      const sortedAsc = [...ascNames].sort();
      expect(ascNames).toEqual(sortedAsc);
    });

    it('offset and limit return correct slice; total remains pre-pagination count', async () => {
      const page1 = await searchComponents({
        options: { offset: 0, limit: 10 },
      });

      const page2 = await searchComponents({
        options: { offset: 10, limit: 10 },
      });

      expect(page1.hits).toHaveLength(10);
      expect(page2.hits).toHaveLength(10);

      // Total should be the same regardless of pagination
      const totalComponents = page1.total;
      expect(totalComponents).toBeGreaterThan(40); // ~50 mock components
      expect(page2.total).toBe(totalComponents);

      // Results should not overlap
      const page1Names = page1.hits.map((c) => c.originId);
      const page2Names = page2.hits.map((c) => c.originId);
      const overlap = page1Names.filter((id) => page2Names.includes(id));
      expect(overlap).toHaveLength(0);
    });

    it('returns a Promise (latency is awaited)', async () => {
      // Verify async behavior by checking the result is correct
      // (latency is an implementation detail, not a contract)
      const result = await searchComponents();
      expect(result.hits).toBeDefined();
      expect(result.total).toBeGreaterThan(0);
    });

    it('query filter matches component name', async () => {
      const result = await searchComponents({
        query: 'lodash',
      });

      expect(result.total).toBeGreaterThan(0);
      expect(result.hits.every((c) => c.name.toLowerCase().includes('lodash'))).toBe(true);
    });

    it('combined filters work together', async () => {
      const result = await searchComponents({
        query: 'spring',
        filters: { formats: ['maven'] },
        options: { sortField: 'version', sortOrder: 'desc', limit: 5 },
      });

      expect(result.hits.every((c) => c.format === 'maven')).toBe(true);
      expect(result.hits.every((c) => c.name.toLowerCase().includes('spring'))).toBe(true);
      expect(result.hits).toHaveLength(Math.min(5, result.total));
    });

    it('aggregations reflect filtered results', async () => {
      const allResults = await searchComponents();
      const npmOnly = await searchComponents({
        filters: { formats: ['npm'] },
      });

      // npm-only aggregation should only have npm format
      expect(Object.keys(npmOnly.aggregations!.byFormat)).toEqual(['npm']);

      // Total aggregation counts should be larger
      const allTotal = Object.values(allResults.aggregations!.byFormat).reduce((a: number, b: number) => a + b, 0);
      const npmTotal = npmOnly.aggregations!.byFormat.npm;
      expect(allTotal).toBeGreaterThan(npmTotal);
    });
  });
});
