/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { searchAll } from 'GuideRoot/api/searchBackend';

describe('searchBackend', () => {
  describe('searchAll', () => {
    it('returns mixed component and vulnerability hits with default limit', async () => {
      const result = await searchAll();

      expect(result.hits.length).toBeGreaterThan(0);
      expect(result.hits.length).toBeLessThanOrEqual(25);
      expect(result.offset).toBe(0);
      expect(result.limit).toBe(25);
      expect(result.total).toBeGreaterThan(0);
      expect(result.aggregations).toBeDefined();
    });

    it('filters hits by case-insensitive query against name / vulnId / summary', async () => {
      const result = await searchAll({ query: 'left-pad' });

      expect(result.hits.length).toBeGreaterThan(0);
      expect(result.hits.every((h) => {
        if ('name' in h) return (h.name ?? '').toLowerCase().includes('left-pad');
        return (h.vulnId ?? '').toLowerCase().includes('left-pad') ||
               (h.summary ?? '').toLowerCase().includes('left-pad');
      })).toBe(true);
    });

    it('respects offset and limit', async () => {
      const page1 = await searchAll({ options: { offset: 0, limit: 5 } });
      const page2 = await searchAll({ options: { offset: 5, limit: 5 } });

      expect(page1.hits).toHaveLength(5);
      expect(page2.hits).toHaveLength(5);
      expect(page1.total).toBe(page2.total);
      const ident = (h: typeof page1.hits[number]) => 'name' in h ? `c:${h.originId}` : `v:${h.vulnId}`;
      const overlap = page1.hits.map(ident).filter((id) => page2.hits.map(ident).includes(id));
      expect(overlap).toHaveLength(0);
    });

    it('aggregates byType reflecting the filtered hits', async () => {
      // Use a large limit so hits == filtered set; aggregations are computed over the
      // entire filtered set (pre-pagination), so we need every match in the page to
      // assert byType counts directly.
      const result = await searchAll({ query: 'left-pad', options: { limit: 1000 } });

      const components = result.hits.filter((h) => 'name' in h).length;
      const vulns = result.hits.filter((h) => 'vulnId' in h && !('name' in h)).length;

      expect(result.aggregations?.byType?.component ?? 0).toBe(components);
      expect(result.aggregations?.byType?.vulnerability ?? 0).toBe(vulns);
    });
  });
});
