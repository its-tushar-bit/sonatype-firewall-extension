/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ViolationRow, ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';
import { filterViolations, computeViolationFacets } from 'MainRoot/nosc/searchResults/violationSearchFiltering';
import { createDefaultViolationsFilterState } from 'MainRoot/nosc/violations/violationsListApi';

const mockRows: ViolationRow[] = [
  {
    policyViolationId: 'violation-001',
    threatLevel: 9,
    threatCategory: 'security',
    stage: 'Build',
    state: 'OPEN',
    organizationId: 'org-1',
    applicationId: 'app-1',
    policyName: 'Security Policy',
    componentName: 'component-a',
  },
  {
    policyViolationId: 'violation-002',
    threatLevel: 5,
    threatCategory: 'license',
    stage: 'Stage Release',
    state: 'WAIVED',
    organizationId: 'org-1',
    applicationId: 'app-2',
    waivedWithAutoWaiver: true,
    policyName: 'License Policy',
    componentName: 'component-b',
  },
  {
    policyViolationId: 'violation-003',
    threatLevel: 7,
    threatCategory: 'security',
    stage: 'Build',
    state: 'OPEN',
    organizationId: 'org-2',
    applicationId: 'app-3',
    policyName: 'Security Policy',
    componentName: 'component-c',
  },
  {
    policyViolationId: 'violation-004',
    threatLevel: 3,
    threatCategory: 'quality',
    stage: 'Release',
    state: 'WAIVED',
    organizationId: 'org-2',
    applicationId: 'app-1',
    waivedWithAutoWaiver: false,
    policyName: 'Quality Policy',
    componentName: 'component-d',
  },
  {
    policyViolationId: 'violation-005',
    threatLevel: 10,
    threatCategory: 'security',
    stage: 'Build',
    state: 'OPEN',
    organizationId: 'org-1',
    applicationId: 'app-2',
    policyName: 'Critical Security',
    componentName: 'component-e',
  },
];

describe('violationSearchFiltering', () => {
  describe('filterViolations', () => {
    it('returns all rows when no filters are active', () => {
      const filters = createDefaultViolationsFilterState();
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(mockRows.length);
    });

    it('filters by state', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        states: new Set(['OPEN']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(3);
      expect(result.every((r) => r.state === 'OPEN')).toBe(true);
    });

    it('filters by multiple states', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        states: new Set(['OPEN', 'WAIVED']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(mockRows.length);
    });

    it('filters by threat category', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        threatCategories: new Set(['security']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(3);
      expect(result.every((r) => r.threatCategory === 'security')).toBe(true);
    });

    it('filters by multiple threat categories', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        threatCategories: new Set(['security', 'license']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(4);
    });

    it('filters by threat range', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        threatRange: [7, 10],
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(3);
      expect(result.every((r) => r.threatLevel !== undefined && r.threatLevel >= 7)).toBe(true);
    });

    it('filters by threat range (lower bound)', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        threatRange: [5, 10],
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(4);
    });

    it('filters by threat range (specific value)', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        threatRange: [9, 9],
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(1);
      expect(result[0].policyViolationId).toBe('violation-001');
    });

    it('filters by stage id (lowercased and hyphenated)', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        stageIds: new Set(['build']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(3);
    });

    it('filters by stage id with multi-word stage name', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        stageIds: new Set(['stage-release']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(1);
      expect(result[0].policyViolationId).toBe('violation-002');
    });

    it('filters by organization id', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        organizationIds: new Set(['org-1']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(3);
    });

    it('filters by application id', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        applicationIds: new Set(['app-1']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(2);
    });

    it('filters by auto waiver type', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        waiverType: 'AUTO',
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(1);
      expect(result[0].waivedWithAutoWaiver).toBe(true);
    });

    it('filters by manual waiver type', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        waiverType: 'MANUAL',
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(1);
      expect(result[0].waivedWithAutoWaiver).toBe(false);
    });

    it('applies multiple filters together (AND logic)', () => {
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        states: new Set(['OPEN']),
        threatCategories: new Set(['security']),
        organizationIds: new Set(['org-1']),
      };
      const result = filterViolations(mockRows, filters);
      expect(result).toHaveLength(2);
      // violation-001 and violation-005
    });

    it('excludes rows without required field when filtering by that field', () => {
      const rowsWithMissing: ViolationRow[] = [
        { policyViolationId: 'no-state', threatLevel: 5 },
        ...mockRows,
      ];
      const filters: ViolationsFilterState = {
        ...createDefaultViolationsFilterState(),
        states: new Set(['OPEN']),
      };
      const result = filterViolations(rowsWithMissing, filters);
      // Should not include the row without state
      expect(result.find((r) => r.policyViolationId === 'no-state')).toBeUndefined();
    });
  });

  describe('computeViolationFacets', () => {
    it('computes facet counts for all dimensions', () => {
      const facets = computeViolationFacets(mockRows);
      expect(facets.totalViolations).toBe(mockRows.length);
      expect(facets.states).toBeDefined();
      expect(facets.threatCategories).toBeDefined();
      expect(facets.stages).toBeDefined();
      expect(facets.organizations).toBeDefined();
      expect(facets.applications).toBeDefined();
    });

    it('counts states correctly', () => {
      const facets = computeViolationFacets(mockRows);
      expect(facets.states?.OPEN).toBe(3);
      expect(facets.states?.WAIVED).toBe(2);
    });

    it('counts threat categories correctly', () => {
      const facets = computeViolationFacets(mockRows);
      expect(facets.threatCategories?.security).toBe(3);
      expect(facets.threatCategories?.license).toBe(1);
      expect(facets.threatCategories?.quality).toBe(1);
    });

    it('counts stage ids correctly (lowercased and hyphenated)', () => {
      const facets = computeViolationFacets(mockRows);
      expect(facets.stages?.build).toBe(3);
      expect(facets.stages?.['stage-release']).toBe(1);
      expect(facets.stages?.release).toBe(1);
    });

    it('counts organizations correctly', () => {
      const facets = computeViolationFacets(mockRows);
      expect(facets.organizations?.['org-1']).toBe(3);
      expect(facets.organizations?.['org-2']).toBe(2);
    });

    it('counts applications correctly', () => {
      const facets = computeViolationFacets(mockRows);
      expect(facets.applications?.['app-1']).toBe(2);
      expect(facets.applications?.['app-2']).toBe(2);
      expect(facets.applications?.['app-3']).toBe(1);
    });

    it('counts waiver types correctly (only for WAIVED state)', () => {
      const facets = computeViolationFacets(mockRows);
      // 2 WAIVED: 1 AUTO, 1 MANUAL
      expect(facets.waiverTypes?.AUTO).toBe(1);
      expect(facets.waiverTypes?.MANUAL).toBe(1);
    });

    it('does not count waiver type for OPEN violations', () => {
      const openOnlyRows = mockRows.filter((r) => r.state === 'OPEN');
      const facets = computeViolationFacets(openOnlyRows);
      // waiverTypes should be empty (or {} if initialized) since no WAIVED violations
      expect(Object.keys(facets.waiverTypes ?? {}).length).toBe(0);
    });

    it('excludes undefined/empty values from counts', () => {
      const rowsWithUndefined: ViolationRow[] = [
        { policyViolationId: 'undefined-fields' },
        ...mockRows,
      ];
      const facets = computeViolationFacets(rowsWithUndefined);
      // The undefined fields should not add to counts
      expect(facets.states?.undefined).toBeUndefined();
      expect(facets.threatCategories?.undefined).toBeUndefined();
    });

    it('handles empty rows array', () => {
      const facets = computeViolationFacets([]);
      expect(facets.totalViolations).toBe(0);
      expect(facets.states).toEqual({});
      expect(facets.threatCategories).toEqual({});
    });
  });
});
