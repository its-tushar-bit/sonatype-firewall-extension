/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getExploreProducts,
  groupAndSortLicensedSolutions,
  PRODUCT_METADATA,
  SolutionId,
} from 'GuideRoot/layout/ProductSwitcher/productMetadata';
import type {
  LicensedProduct,
  LicensedSolution,
} from 'GuideRoot/layout/ProductSwitcher/productMetadata';

describe('productMetadata', () => {
  describe('PRODUCT_METADATA', () => {
    it('contains entries for all known solution ids', () => {
      const expectedIds: SolutionId[] = [
        'lifecycle',
        'nexusRepositoryManager',
        'firewall',
        'sbom',
        'guide',
      ];
      expectedIds.forEach((id) => {
        expect(PRODUCT_METADATA[id]).toBeDefined();
        expect(PRODUCT_METADATA[id].displayName).toEqual(expect.any(String));
        expect(PRODUCT_METADATA[id].iconLight).toEqual(expect.any(String));
        expect(PRODUCT_METADATA[id].iconDark).toEqual(expect.any(String));
        expect(PRODUCT_METADATA[id].marketingUrl).toMatch(/^https:\/\/www\.sonatype\.com\//);
      });
    });
  });

  describe('groupAndSortLicensedSolutions', () => {
    it('returns an empty array for an empty input', () => {
      expect(groupAndSortLicensedSolutions([])).toEqual([]);
    });

    it('drops entries whose id is not in PRODUCT_METADATA', () => {
      const input: LicensedSolution[] = [
        { id: 'unknown-product' as SolutionId, url: '/u' },
        { id: 'lifecycle', url: '/l' },
      ];
      const result = groupAndSortLicensedSolutions(input);
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe('lifecycle');
    });

    it('returns single-instance products with a url', () => {
      const input: LicensedSolution[] = [
        { id: 'lifecycle', url: '/lifecycle' },
      ];
      const result = groupAndSortLicensedSolutions(input);
      expect(result).toEqual([
        {
          id: 'lifecycle',
          displayName: PRODUCT_METADATA.lifecycle.displayName,
          url: '/lifecycle',
        },
      ]);
    });

    it('groups entries with the same id into a multi-instance product', () => {
      const input: LicensedSolution[] = [
        { id: 'nexusRepositoryManager', url: '/nxrm-east' },
        { id: 'nexusRepositoryManager', url: '/nxrm-west' },
      ];
      const result = groupAndSortLicensedSolutions(input);
      expect(result).toHaveLength(1);
      expect(result[0]).toEqual({
        id: 'nexusRepositoryManager',
        displayName: PRODUCT_METADATA.nexusRepositoryManager.displayName,
        instances: [{ url: '/nxrm-east' }, { url: '/nxrm-west' }],
      });
    });

    it('sorts the resulting products alphabetically by displayName', () => {
      const input: LicensedSolution[] = [
        { id: 'sbom', url: '/sbom' },
        { id: 'guide', url: '/guide' },
        { id: 'lifecycle', url: '/lifecycle' },
      ];
      const result = groupAndSortLicensedSolutions(input);
      const displayNames = result.map((p) => p.displayName);
      expect(displayNames).toEqual([...displayNames].sort((a, b) => a.localeCompare(b)));
    });
  });

  describe('getExploreProducts', () => {
    it('returns all known products when nothing is licensed', () => {
      const result = getExploreProducts([]);
      const ids = result.map((p) => p.id);
      expect(ids.sort()).toEqual(Object.keys(PRODUCT_METADATA).sort());
    });

    it('omits products that are already licensed', () => {
      const licensed: LicensedProduct[] = [
        { id: 'lifecycle', displayName: 'Lifecycle', url: '/l' },
        { id: 'sbom', displayName: 'SBOM Manager', url: '/s' },
      ];
      const result = getExploreProducts(licensed);
      const ids = result.map((p) => p.id);
      expect(ids).not.toContain('lifecycle');
      expect(ids).not.toContain('sbom');
      expect(ids).toContain('nexusRepositoryManager');
      expect(ids).toContain('firewall');
      expect(ids).toContain('guide');
    });

    it('uses the marketingUrl from PRODUCT_METADATA', () => {
      const result = getExploreProducts([]);
      const lifecycle = result.find((p) => p.id === 'lifecycle');
      expect(lifecycle?.url).toBe(PRODUCT_METADATA.lifecycle.marketingUrl);
    });

    it('sorts results alphabetically by displayName', () => {
      const result = getExploreProducts([]);
      const displayNames = result.map((p) => p.displayName);
      expect(displayNames).toEqual([...displayNames].sort((a, b) => a.localeCompare(b)));
    });
  });
});
