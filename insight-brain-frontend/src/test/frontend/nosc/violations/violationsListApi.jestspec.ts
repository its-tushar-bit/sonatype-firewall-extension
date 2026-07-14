/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildViolationsListRequest,
  deriveViolationFacetLabels,
  stageLabel,
  threatCategoryLabel,
  violationStateLabel,
  VIOLATIONS_DEFAULT_ORDER_BY,
  VIOLATIONS_PAGE_SIZE,
} from 'MainRoot/nosc/violations/violationsListApi';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import { ViolationRow } from 'MainRoot/nosc/violations/violationListTypes';

describe('violationsListApi', () => {
  describe('buildViolationsListRequest', () => {
    it('defaults page size, facets, and threat-desc sort', () => {
      const request = buildViolationsListRequest({ page: 0 });
      expect(request).toEqual({
        page: 0,
        pageSize: VIOLATIONS_PAGE_SIZE,
        includeFacets: true,
        orderBy: VIOLATIONS_DEFAULT_ORDER_BY,
      });
    });

    it('omits blank/whitespace search terms and trims real ones', () => {
      expect(buildViolationsListRequest({ page: 1, search: '   ' }).search).toBeUndefined();
      expect(buildViolationsListRequest({ page: 1, search: '  log4j  ' }).search).toBe('log4j');
    });
  });

  describe('facet labels', () => {
    it('maps enum-keyed states and categories to friendly labels, falling back to the raw id', () => {
      expect(violationStateLabel('OPEN')).toBe('Open');
      expect(violationStateLabel('WAIVED')).toBe('Waived');
      expect(violationStateLabel('SOMETHING')).toBe('SOMETHING');
      expect(threatCategoryLabel('security')).toBe('Security');
      expect(threatCategoryLabel('license')).toBe('License');
      expect(threatCategoryLabel('mystery')).toBe('mystery');
    });

    it('maps id-keyed stage facets to display names, title-casing unknown/future ids', () => {
      // The stages facet is keyed by stage id (e.g. build, stage-release), not the row display name.
      expect(stageLabel('build')).toBe('Build');
      expect(stageLabel('stage-release')).toBe('Stage Release');
      expect(stageLabel('operate')).toBe('Operate');
      // Unknown id falls back to a Title-Cased id, never the raw slug.
      expect(stageLabel('some-new-stage')).toBe('Some New Stage');
    });
  });

  describe('deriveViolationFacetLabels', () => {
    it('builds id→name maps for org / app from the current rows (stages are labeled separately)', () => {
      const rows: ReadonlyArray<ViolationRow> = [
        {
          policyViolationId: 'pv-1',
          organizationId: 'org-1',
          organizationName: 'Java-team',
          applicationId: 'app-1',
          applicationName: 'Apple - Java',
          stage: 'Build',
        },
      ];
      const labels = deriveViolationFacetLabels(rows);
      expect(labels.organizations['org-1']).toBe('Java-team');
      expect(labels.applications['app-1']).toBe('Apple - Java');
    });
  });

  describe('violationDetailHref', () => {
    it('targets the embed state and URL-encodes the id', () => {
      expect(violationDetailHref('pv-1')).toBe('#/violations/pv-1');
      expect(violationDetailHref('a/b c')).toBe('#/violations/a%2Fb%20c');
    });
  });
});
