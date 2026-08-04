/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildViolationsListRouteParams,
  MAX_DEEP_LINK_PAGE,
  parseViolationsListParams,
  violationsFiltersEqual,
} from 'MainRoot/nosc/violations/violationsListQuery';
import { createDefaultViolationsFilterState } from 'MainRoot/nosc/violations/violationsListApi';
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';

function filterState(overrides: Partial<ViolationsFilterState> = {}): ViolationsFilterState {
  return { ...createDefaultViolationsFilterState(), ...overrides };
}

describe('violationsListQuery (CLM-42260)', () => {
  describe('parseViolationsListParams', () => {
    it('returns empty search, page 0, and default filters for empty params', () => {
      const parsed = parseViolationsListParams({});
      expect(parsed.search).toBe('');
      expect(parsed.page).toBe(0);
      expect(violationsFiltersEqual(parsed.filters, createDefaultViolationsFilterState())).toBe(true);
    });

    it('parses search, 1-based page (to 0-based), and all filter groups', () => {
      const parsed = parseViolationsListParams({
        q: '  log4j  ',
        page: '3',
        state: 'OPEN,WAIVED',
        category: 'security,license',
        stage: 'build,release',
        org: 'org-java',
        app: 'app-apple,app-banana',
        appCategory: 'cat-internal,cat-distributed',
        threat: '4-9',
      });
      expect(parsed.search).toBe('log4j');
      expect(parsed.page).toBe(2);
      expect([...parsed.filters.states].sort()).toEqual(['OPEN', 'WAIVED']);
      expect([...parsed.filters.threatCategories].sort()).toEqual(['license', 'security']);
      expect([...parsed.filters.stageIds].sort()).toEqual(['build', 'release']);
      expect([...parsed.filters.organizationIds]).toEqual(['org-java']);
      expect([...parsed.filters.applicationIds].sort()).toEqual(['app-apple', 'app-banana']);
      expect([...parsed.filters.applicationCategoryIds].sort()).toEqual(['cat-distributed', 'cat-internal']);
      expect(parsed.filters.threatRange).toEqual([4, 9]);
    });

    it('drops unsupported state and category tokens', () => {
      const parsed = parseViolationsListParams({ state: 'OPEN,BOGUS', category: 'security,made-up' });
      expect([...parsed.filters.states]).toEqual(['OPEN']);
      expect([...parsed.filters.threatCategories]).toEqual(['security']);
    });

    it('accepts the LEGACY_VIOLATION state token', () => {
      const parsed = parseViolationsListParams({ state: 'OPEN,LEGACY_VIOLATION' });
      expect([...parsed.filters.states].sort()).toEqual(['LEGACY_VIOLATION', 'OPEN']);
    });

    it('parses the waiver-type token (case-insensitive) and defaults unknown tokens to ANY (CLM-42261)', () => {
      expect(parseViolationsListParams({ waiver: 'auto' }).filters.waiverType).toBe('AUTO');
      expect(parseViolationsListParams({ waiver: 'manual' }).filters.waiverType).toBe('MANUAL');
      expect(parseViolationsListParams({ waiver: 'AUTO' }).filters.waiverType).toBe('AUTO');
      expect(parseViolationsListParams({ waiver: 'bogus' }).filters.waiverType).toBe('ANY');
      expect(parseViolationsListParams({}).filters.waiverType).toBe('ANY');
    });

    it('clamps the threat range to [0, 10] and forces ascending order', () => {
      // '12-3' → clamp(12)=10, clamp(3)=3, then sort ascending → [3, 10].
      expect(parseViolationsListParams({ threat: '12-3' }).filters.threatRange).toEqual([3, 10]);
      expect(parseViolationsListParams({ threat: '0-15' }).filters.threatRange).toEqual([0, 10]);
    });

    it('falls back to the full range for a malformed threat param', () => {
      expect(parseViolationsListParams({ threat: 'abc' }).filters.threatRange).toEqual([0, 10]);
      expect(parseViolationsListParams({ threat: '5' }).filters.threatRange).toEqual([0, 10]);
      // Non-integer tokens are rejected wholesale (Number.parseInt would have accepted '4abc' → 4).
      expect(parseViolationsListParams({ threat: '4abc-10' }).filters.threatRange).toEqual([0, 10]);
      expect(parseViolationsListParams({ threat: '4.5-9' }).filters.threatRange).toEqual([0, 10]);
    });

    it('treats page values <= 1 or non-numeric as the first page', () => {
      expect(parseViolationsListParams({ page: '1' }).page).toBe(0);
      expect(parseViolationsListParams({ page: '0' }).page).toBe(0);
      expect(parseViolationsListParams({ page: 'x' }).page).toBe(0);
    });

    it('soft-clamps absurd deep-linked page values before the first POST', () => {
      expect(parseViolationsListParams({ page: String(MAX_DEEP_LINK_PAGE + 50) }).page).toBe(
        MAX_DEEP_LINK_PAGE - 1,
      );
      expect(parseViolationsListParams({ page: '999999' }).page).toBe(MAX_DEEP_LINK_PAGE - 1);
    });
  });

  describe('buildViolationsListRouteParams', () => {
    it('omits every param for the default (unfiltered, first-page) state', () => {
      const params = buildViolationsListRouteParams({
        search: '',
        page: 0,
        filters: createDefaultViolationsFilterState(),
      });
      expect(params).toEqual({
        q: undefined,
        page: undefined,
        state: undefined,
        category: undefined,
        stage: undefined,
        org: undefined,
        app: undefined,
        appCategory: undefined,
        threat: undefined,
        waiver: undefined,
      });
    });

    it('serializes search, 1-based page, sorted csv groups, and the threat range', () => {
      const params = buildViolationsListRouteParams({
        search: 'lodash',
        page: 2,
        filters: filterState({
          states: new Set(['WAIVED', 'OPEN']),
          threatCategories: new Set(['license', 'security']),
          stageIds: new Set(['release', 'build']),
          organizationIds: new Set(['org-java']),
          applicationIds: new Set(['app-banana', 'app-apple']),
          applicationCategoryIds: new Set(['cat-b', 'cat-a']),
          threatRange: [4, 9],
        }),
      });
      expect(params).toEqual({
        q: 'lodash',
        page: '3',
        state: 'OPEN,WAIVED',
        category: 'license,security',
        stage: 'build,release',
        org: 'org-java',
        app: 'app-apple,app-banana',
        appCategory: 'cat-a,cat-b',
        threat: '4-9',
      });
    });

    it('serializes the waiver-type radio to a lowercase token, omitting ANY (CLM-42261)', () => {
      const base = { search: '', page: 0 };
      expect(
        buildViolationsListRouteParams({ ...base, filters: filterState({ waiverType: 'ANY' }) }).waiver,
      ).toBeUndefined();
      expect(
        buildViolationsListRouteParams({ ...base, filters: filterState({ waiverType: 'AUTO' }) }).waiver,
      ).toBe('auto');
      expect(
        buildViolationsListRouteParams({ ...base, filters: filterState({ waiverType: 'MANUAL' }) }).waiver,
      ).toBe('manual');
    });

    it('omits the threat param when the range is the full [0, 10] default', () => {
      const params = buildViolationsListRouteParams({
        search: '',
        page: 0,
        filters: filterState({ threatRange: [0, 10] }),
      });
      expect(params.threat).toBeUndefined();
    });

    it('round-trips a populated state through build → parse', () => {
      const state = {
        search: 'log4j',
        page: 4,
        filters: filterState({
          states: new Set(['OPEN']),
          threatCategories: new Set(['security']),
          stageIds: new Set(['build']),
          organizationIds: new Set(['org-java']),
          applicationIds: new Set(['app-apple']),
          applicationCategoryIds: new Set(['cat-internal']),
          threatRange: [2, 8] as const,
          waiverType: 'AUTO' as const,
        }),
      };
      const reparsed = parseViolationsListParams(buildViolationsListRouteParams(state));
      expect(reparsed.search).toBe('log4j');
      expect(reparsed.page).toBe(4);
      expect(violationsFiltersEqual(reparsed.filters, state.filters)).toBe(true);
    });
  });

  describe('violationsFiltersEqual', () => {
    it('is true for two default selections', () => {
      expect(
        violationsFiltersEqual(createDefaultViolationsFilterState(), createDefaultViolationsFilterState()),
      ).toBe(true);
    });

    it('detects a differing set member', () => {
      expect(
        violationsFiltersEqual(filterState({ states: new Set(['OPEN']) }), filterState()),
      ).toBe(false);
    });

    it('detects differing application category ids (CLM-44129)', () => {
      expect(
        violationsFiltersEqual(
          filterState({ applicationCategoryIds: new Set(['cat-a']) }),
          filterState({ applicationCategoryIds: new Set(['cat-b']) }),
        ),
      ).toBe(false);
    });

    it('detects a differing threat range', () => {
      expect(
        violationsFiltersEqual(filterState({ threatRange: [1, 10] }), filterState({ threatRange: [0, 10] })),
      ).toBe(false);
    });

    it('detects a differing waiver type (CLM-42261)', () => {
      expect(
        violationsFiltersEqual(filterState({ waiverType: 'AUTO' }), filterState({ waiverType: 'MANUAL' })),
      ).toBe(false);
      expect(
        violationsFiltersEqual(filterState({ waiverType: 'AUTO' }), filterState({ waiverType: 'AUTO' })),
      ).toBe(true);
    });
  });
});
