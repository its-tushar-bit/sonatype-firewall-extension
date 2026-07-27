/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  adaptLegalFacetsForRail,
  adaptLegalFindingToViolationRow,
  buildLegalListRequest,
  createDefaultLegalFilterState,
  hasActiveLegalFilters,
  LEGAL_DEFAULT_ORDER_BY,
  legalFindingHref,
} from 'MainRoot/nosc/legal/legalListApi';

describe('legalListApi (CLM-43207 LEGAL_VIOLATION)', () => {
  describe('createDefaultLegalFilterState', () => {
    it('starts with no narrowing', () => {
      const filters = createDefaultLegalFilterState();
      expect(filters.threatCategories.size).toBe(0);
      expect(hasActiveLegalFilters(filters)).toBe(false);
    });
  });

  describe('buildLegalListRequest', () => {
    it('defaults to license-threat sort and omits empty filters', () => {
      const request = buildLegalListRequest({ page: 0 });
      expect(request.orderBy).toBe(LEGAL_DEFAULT_ORDER_BY);
      expect(request.licenseThreatGroupNames).toBeUndefined();
      expect(request.organizationIds).toBeUndefined();
    });

    it('maps threatCategories selection to licenseThreatGroupNames', () => {
      const request = buildLegalListRequest({
        page: 0,
        filters: {
          ...createDefaultLegalFilterState(),
          threatCategories: new Set(['Copyleft', 'Banned']),
          organizationIds: new Set(['org-1']),
        },
      });
      expect(request.licenseThreatGroupNames).toEqual(['Banned', 'Copyleft']);
      expect(request.organizationIds).toEqual(['org-1']);
    });

    it('sends licenseThreatLevelRange as min/max object when narrowed', () => {
      const request = buildLegalListRequest({
        page: 0,
        filters: {
          ...createDefaultLegalFilterState(),
          threatRange: [7, 10],
        },
      });
      expect(request.licenseThreatLevelRange).toEqual({
        minPolicyThreatLevel: 7,
        maxPolicyThreatLevel: 10,
      });
    });
  });

  describe('adaptLegalFindingToViolationRow', () => {
    it('prefers LTG name as the card title field', () => {
      const row = adaptLegalFindingToViolationRow({
        legalFindingId: 'a|h|lic|build',
        licenseThreatGroupName: 'Copyleft',
        licenseName: 'GPL-2.0',
        applicationPublicId: 'apple-java1',
        threatLevel: 8,
      });
      expect(row.policyViolationId).toBe('a|h|lic|build');
      expect(row.policyName).toBe('Copyleft');
      expect(row.threatCategory).toBe('license');
    });
  });

  describe('adaptLegalFacetsForRail', () => {
    it('maps licenseThreatGroups onto threatCategories for the shared rail', () => {
      const facets = adaptLegalFacetsForRail({
        totalFindings: 12,
        licenseThreatGroups: { Copyleft: 5 },
        organizations: { o1: 3 },
      });
      expect(facets?.totalViolations).toBe(12);
      expect(facets?.threatCategories).toEqual({ Copyleft: 5 });
      expect(facets?.organizations).toEqual({ o1: 3 });
    });
  });

  describe('legalFindingHref', () => {
    it('embeds Classic Legal component overview in NOUX when ALP is licensed', () => {
      expect(
        legalFindingHref(
          { componentHash: 'abc123', applicationPublicId: 'apple-java1' },
          { advancedLegalPack: true },
        ),
      ).toBe('#/legal/component/abc123');
    });

    it('embeds Classic report Component Legal tab using reportId as the scan id (index field name)', () => {
      // LEGAL_VIOLATION.reportId is PolicyEvaluation.getScanId() — the applicationReport state
      // consumes that value in the scan-id route segment (not a report-row primary key).
      expect(
        legalFindingHref(
          {
            componentHash: 'abc123',
            applicationPublicId: 'apple-java1',
            reportId: 'scan-1',
          },
          { advancedLegalPack: false },
        ),
      ).toBe('#/applicationReport/apple-java1/scan-1/componentDetails/abc123/legal');
    });

    it('falls back to Nexus One Application Detail when scan/hash is incomplete', () => {
      expect(
        legalFindingHref({ applicationPublicId: 'apple-java1' }, { advancedLegalPack: false }),
      ).toBe('#/applications/apple-java1');
    });
  });
});
