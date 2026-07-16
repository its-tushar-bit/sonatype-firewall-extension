/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildViolationsListRequest,
  createDefaultViolationsFilterState,
  deriveViolationFacetLabels,
  hasActiveViolationFilters,
  hasExportableViolationFilters,
  isDefaultThreatRange,
  stageLabel,
  threatCategoryLabel,
  violationStateLabel,
  waiverTypeLabel,
  waiverTypeToRequestFlag,
  WAIVER_TYPE_AUTO,
  WAIVER_TYPE_MANUAL,
  VIOLATIONS_DEFAULT_ORDER_BY,
  VIOLATIONS_PAGE_SIZE,
} from 'MainRoot/nosc/violations/violationsListApi';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import { ViolationRow, ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';

function filterState(overrides: Partial<ViolationsFilterState> = {}): ViolationsFilterState {
  return { ...createDefaultViolationsFilterState(), ...overrides };
}

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

    it('omits all filter fields when the selection is empty (default range, no groups)', () => {
      const request = buildViolationsListRequest({ page: 0, filters: filterState() });
      expect(request.policyViolationStates).toBeUndefined();
      expect(request.policyThreatCategories).toBeUndefined();
      expect(request.policyThreatLevelRange).toBeUndefined();
      expect(request.stageIds).toBeUndefined();
      expect(request.organizationIds).toBeUndefined();
      expect(request.applicationIds).toBeUndefined();
    });

    it('serializes each filter group to the backend wire format', () => {
      const request = buildViolationsListRequest({
        page: 0,
        filters: filterState({
          states: new Set(['WAIVED', 'OPEN']),
          threatCategories: new Set(['license', 'security']),
          stageIds: new Set(['release', 'build']),
          organizationIds: new Set(['org-b', 'org-a']),
          applicationIds: new Set(['app-2', 'app-1']),
          threatRange: [4, 9],
        }),
      });
      // States: array of enum names (PolicyViolationStateFilter @JsonCreator Set constructor).
      expect(request.policyViolationStates).toEqual(['OPEN', 'WAIVED']);
      // Categories + range: comma-delimited strings (their String constructors).
      expect(request.policyThreatCategories).toBe('license,security');
      expect(request.policyThreatLevelRange).toBe('4,9');
      // Id sets: arrays (sorted for deterministic output).
      expect(request.stageIds).toEqual(['build', 'release']);
      expect(request.organizationIds).toEqual(['org-a', 'org-b']);
      expect(request.applicationIds).toEqual(['app-1', 'app-2']);
    });

    it('omits a full-domain [0,10] threat range but sends a narrowed one', () => {
      expect(
        buildViolationsListRequest({ page: 0, filters: filterState({ threatRange: [0, 10] }) })
          .policyThreatLevelRange,
      ).toBeUndefined();
      expect(
        buildViolationsListRequest({ page: 0, filters: filterState({ threatRange: [7, 10] }) })
          .policyThreatLevelRange,
      ).toBe('7,10');
    });

    it('maps the waiver-type radio to the backend waivedWithAutoWaiver boolean (CLM-42261)', () => {
      // ANY: omitted; AUTO: true (auto-waived only); MANUAL: false (manually-waived only).
      expect(
        buildViolationsListRequest({ page: 0, filters: filterState({ waiverType: 'ANY' }) }),
      ).not.toHaveProperty('waivedWithAutoWaiver');
      expect(
        buildViolationsListRequest({ page: 0, filters: filterState({ waiverType: 'AUTO' }) })
          .waivedWithAutoWaiver,
      ).toBe(true);
      expect(
        buildViolationsListRequest({ page: 0, filters: filterState({ waiverType: 'MANUAL' }) })
          .waivedWithAutoWaiver,
      ).toBe(false);
    });
  });

  describe('waiverTypeToRequestFlag', () => {
    it('maps ANY→undefined, AUTO→true, MANUAL→false', () => {
      expect(waiverTypeToRequestFlag('ANY')).toBeUndefined();
      expect(waiverTypeToRequestFlag('AUTO')).toBe(true);
      expect(waiverTypeToRequestFlag('MANUAL')).toBe(false);
    });
  });

  describe('filter-state helpers', () => {
    it('createDefaultViolationsFilterState is empty with a full [0,10] range and ANY waiver type', () => {
      const state = createDefaultViolationsFilterState();
      expect(state.states.size).toBe(0);
      expect(state.threatCategories.size).toBe(0);
      expect(state.stageIds.size).toBe(0);
      expect(state.organizationIds.size).toBe(0);
      expect(state.applicationIds.size).toBe(0);
      expect(state.threatRange).toEqual([0, 10]);
      expect(state.waiverType).toBe('ANY');
    });

    it('isDefaultThreatRange is true only for the full domain', () => {
      expect(isDefaultThreatRange([0, 10])).toBe(true);
      expect(isDefaultThreatRange([1, 10])).toBe(false);
      expect(isDefaultThreatRange([0, 9])).toBe(false);
    });

    it('hasActiveViolationFilters detects any narrowing group, a narrowed range, or a waiver type', () => {
      expect(hasActiveViolationFilters(filterState())).toBe(false);
      expect(hasActiveViolationFilters(filterState({ states: new Set(['OPEN']) }))).toBe(true);
      expect(hasActiveViolationFilters(filterState({ threatRange: [2, 10] }))).toBe(true);
      expect(hasActiveViolationFilters(filterState({ waiverType: 'AUTO' }))).toBe(true);
    });

    it('hasExportableViolationFilters ignores the index-only waiver-type filter (CLM-42261)', () => {
      // waiverType has no RisksFilterDTO equivalent, so a waiver-only selection is not "exportable".
      expect(hasExportableViolationFilters(filterState({ waiverType: 'AUTO' }))).toBe(false);
      // Any export-honored group still counts.
      expect(hasExportableViolationFilters(filterState({ states: new Set(['WAIVED']) }))).toBe(true);
      expect(hasExportableViolationFilters(filterState({ threatRange: [2, 10] }))).toBe(true);
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

    it('maps the AUTO / MANUAL waiver-type facet keys to friendly labels (CLM-42261)', () => {
      expect(waiverTypeLabel('AUTO')).toBe('Auto-waived');
      expect(waiverTypeLabel('MANUAL')).toBe('Manually waived');
      expect(waiverTypeLabel('OTHER')).toBe('OTHER');
    });

    it('pins the waiver-type facet wire keys to the literal AUTO / MANUAL strings (CLM-42261)', () => {
      // The backend (ViolationsListFacetsBuilder.WAIVER_TYPE_AUTO/MANUAL) keys facets.waiverTypes by
      // these exact strings. Assert the literals (not the imported constant against itself) so a rename
      // on either side fails loudly instead of silently blanking the sidebar counts.
      expect(WAIVER_TYPE_AUTO).toBe('AUTO');
      expect(WAIVER_TYPE_MANUAL).toBe('MANUAL');
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
