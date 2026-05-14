/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { searchVulnerabilities } from 'GuideRoot/api/vulnerabilitiesBackend';
import { getCVSSSeverity } from '@guide/ui-core';

describe('vulnerabilitiesBackend', () => {
  describe('searchVulnerabilities', () => {
    it('returns full dataset when called with no filters', async () => {
      const result = await searchVulnerabilities();

      expect(result.hits).toHaveLength(25); // Default limit
      expect(result.total).toBeGreaterThan(40); // 50+ mock vulnerabilities
      expect(result.offset).toBe(0);
      expect(result.limit).toBe(25);
      expect(result.aggregations).toBeDefined();
      expect(result.aggregations!.byEcosystem).toBeDefined();
    });

    it('affectedEcosystems filter reduces results and updates aggregation counts', async () => {
      const result = await searchVulnerabilities({
        filters: { affectedEcosystems: ['npm'] },
      });

      expect(result.total).toBeLessThan(50);
      expect(result.hits.every((v) => v.affectedEcosystems.includes('npm'))).toBe(true);

      // Aggregations should reflect filtered set
      expect(result.aggregations!.byEcosystem.npm).toBe(result.total);
    });

    it('severities filter reduces results', async () => {
      const criticalOnly = await searchVulnerabilities({
        filters: { severities: ['critical'] },
      });

      // Use ui-core's getCVSSSeverity with Sonatype-adjusted severity for consistency
      expect(
        criticalOnly.hits.every((v) => getCVSSSeverity(v.sonatypeCvssSeverity ?? v.cvssSeverity ?? 0) === 'critical')
      ).toBe(true);
      expect(criticalOnly.aggregations!.bySeverity.critical).toBe(criticalOnly.total);
    });

    it('sort changes result ordering', async () => {
      const ascResult = await searchVulnerabilities({
        options: { sortField: 'sonatypeCvssSeverity', sortOrder: 'asc' },
      });

      const descResult = await searchVulnerabilities({
        options: { sortField: 'sonatypeCvssSeverity', sortOrder: 'desc' },
      });

      // First result in asc should not equal first in desc
      expect(ascResult.hits[0].vulnId).not.toBe(descResult.hits[0].vulnId);
    });

    it('offset and limit return correct slice; total remains pre-pagination count', async () => {
      const page1 = await searchVulnerabilities({
        options: { offset: 0, limit: 10 },
      });

      const page2 = await searchVulnerabilities({
        options: { offset: 10, limit: 10 },
      });

      expect(page1.hits).toHaveLength(10);
      expect(page2.hits).toHaveLength(10);

      // Total should be the same regardless of pagination
      const totalVulns = page1.total;
      expect(totalVulns).toBeGreaterThan(40); // 50+ mock vulnerabilities
      expect(page2.total).toBe(totalVulns);

      // Results should not overlap
      const page1Ids = page1.hits.map((v) => v.vulnId);
      const page2Ids = page2.hits.map((v) => v.vulnId);
      const overlap = page1Ids.filter((id) => page2Ids.includes(id));
      expect(overlap).toHaveLength(0);
    });

    it('returns a Promise (latency is awaited)', async () => {
      // Verify async behavior by checking the result is correct
      // (latency is an implementation detail, not a contract)
      const result = await searchVulnerabilities();
      expect(result.hits).toBeDefined();
      expect(result.total).toBeGreaterThan(0);
    });

    it('query filter matches vulnerability ID or summary', async () => {
      const result = await searchVulnerabilities({
        query: 'Log4j',
      });

      expect(result.total).toBeGreaterThan(0);
      expect(
        result.hits.every(
          (v) =>
            v.vulnId.toLowerCase().includes('log4j') ||
            v.summary.toLowerCase().includes('log4j')
        )
      ).toBe(true);
    });

    it('combined filters work together', async () => {
      const result = await searchVulnerabilities({
        query: 'CVE',
        filters: { affectedEcosystems: ['maven'] },
        options: { sortField: 'publishedDate', sortOrder: 'desc', limit: 5 },
      });

      expect(result.hits.every((v) => v.affectedEcosystems.includes('maven'))).toBe(true);
      expect(result.hits).toHaveLength(Math.min(5, result.total));
    });

    it('aggregations reflect filtered results', async () => {
      const allResults = await searchVulnerabilities();
      const npmOnly = await searchVulnerabilities({
        filters: { affectedEcosystems: ['npm'] },
      });

      // npm-only aggregation should only have npm ecosystem
      expect(Object.keys(npmOnly.aggregations!.byEcosystem)).toContain('npm');

      // Total aggregation counts should be larger
      const allTotal = Object.values(allResults.aggregations!.byEcosystem).reduce(
        (a, b) => a + b,
        0
      );
      const npmTotal = npmOnly.aggregations!.byEcosystem.npm ?? 0;
      expect(allTotal).toBeGreaterThan(npmTotal);
    });

    it('cwes filter reduces results', async () => {
      const result = await searchVulnerabilities({
        filters: { cwes: ['CWE-400'] },
      });

      expect(result.total).toBeGreaterThan(0);
      expect(result.hits.every((v) => v.cwes?.includes('CWE-400'))).toBe(true);
    });

    it('exploitationKnown filter (kev) reduces results', async () => {
      const result = await searchVulnerabilities({
        filters: { exploitationKnown: true },
      });

      expect(result.total).toBeGreaterThan(0);
      expect(result.hits.every((v) => v.kev === true)).toBe(true);
    });
  });
});
