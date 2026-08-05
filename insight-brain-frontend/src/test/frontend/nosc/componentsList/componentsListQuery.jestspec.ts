/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildComponentsListRouteParams,
  parseComponentsListParams,
  parseComponentsThreatRange,
} from 'MainRoot/nosc/componentsList/componentsListQuery';
import {
  DEFAULT_COMPONENTS_THREAT_RANGE,
  EMPTY_COMPONENTS_LIST_FILTERS,
} from 'MainRoot/nosc/componentsList/componentsListFilters';

describe('componentsListQuery (CLM-42214 catalog)', () => {
  it('parseComponentsListParams reads source, search, page, org names, and ecosystems', () => {
    const parsed = parseComponentsListParams({
      source: 'catalog',
      q: 'guava pie',
      page: '3',
      org: 'Java Team',
      ecosystem: 'maven,npm',
    });

    expect(parsed.tab).toBe('catalog');
    expect(parsed.search).toBe('guava pie');
    expect(parsed.page).toBe(2);
    expect(Array.from(parsed.filters.organizations)).toEqual(['Java Team']);
    expect(Array.from(parsed.filters.ecosystems).sort()).toEqual(['maven', 'npm']);
  });

  it('defaults to My Scan Data when source is omitted', () => {
    expect(parseComponentsListParams({}).tab).toBe('myScanData');
  });

  it('buildComponentsListRouteParams omits local source, page 1, and empty filters', () => {
    expect(
      buildComponentsListRouteParams({
        tab: 'myScanData',
        search: '',
        page: 0,
        filters: EMPTY_COMPONENTS_LIST_FILTERS,
      }),
    ).toEqual({});
  });

  it('serializes catalog source and omits estate scope filters on the catalog tab', () => {
    expect(
      buildComponentsListRouteParams({
        tab: 'catalog',
        search: 'lodash',
        page: 1,
        filters: {
          ...EMPTY_COMPONENTS_LIST_FILTERS,
          organizations: new Set(['Java Team']),
          ecosystems: new Set(['npm']),
          applications: new Set(['app-1']),
          stages: new Set(['build']),
        },
      }),
    ).toEqual({
      source: 'catalog',
      q: 'lodash',
      page: '2',
      ecosystem: 'npm',
    });
  });

  it('round-trips My Scan Data filter state', () => {
    const filters = {
      ...EMPTY_COMPONENTS_LIST_FILTERS,
      organizations: new Set(['Java Team']),
      ecosystems: new Set(['maven']),
      applications: new Set(['app-1']),
      stages: new Set(['build', 'release']),
      threatRange: [3, 9] as const,
    };
    const params = buildComponentsListRouteParams({
      tab: 'myScanData',
      search: 'guava',
      page: 0,
      filters,
    });
    expect(params.threat).toBe('3-9');
    const parsed = parseComponentsListParams(params);
    expect(parsed.tab).toBe('myScanData');
    expect(parsed.search).toBe('guava');
    expect(Array.from(parsed.filters.organizations)).toEqual(['Java Team']);
    expect(Array.from(parsed.filters.ecosystems)).toEqual(['maven']);
    expect(Array.from(parsed.filters.applications)).toEqual(['app-1']);
    expect(Array.from(parsed.filters.stages).sort()).toEqual(['build', 'release']);
    expect(parsed.filters.threatRange).toEqual([3, 9]);
  });

  it('omits default threat range and catalog threat from the route', () => {
    expect(
      buildComponentsListRouteParams({
        tab: 'myScanData',
        search: '',
        page: 0,
        filters: EMPTY_COMPONENTS_LIST_FILTERS,
      }).threat,
    ).toBeUndefined();

    expect(
      buildComponentsListRouteParams({
        tab: 'catalog',
        search: '',
        page: 0,
        filters: {
          ...EMPTY_COMPONENTS_LIST_FILTERS,
          threatRange: [3, 9],
        },
      }).threat,
    ).toBeUndefined();
  });

  it('ignores threat= on Catalog URLs so a hidden slider cannot seed active filters', () => {
    const parsed = parseComponentsListParams({
      source: 'catalog',
      threat: '3-9',
    });
    expect(parsed.tab).toBe('catalog');
    expect(parsed.filters.threatRange).toEqual(DEFAULT_COMPONENTS_THREAT_RANGE);
  });

  describe('parseComponentsThreatRange', () => {
    it('returns the default for missing or malformed tokens', () => {
      expect(parseComponentsThreatRange(null)).toEqual(DEFAULT_COMPONENTS_THREAT_RANGE);
      expect(parseComponentsThreatRange('')).toEqual(DEFAULT_COMPONENTS_THREAT_RANGE);
      expect(parseComponentsThreatRange('abc')).toEqual(DEFAULT_COMPONENTS_THREAT_RANGE);
      expect(parseComponentsThreatRange('1-2-3')).toEqual(DEFAULT_COMPONENTS_THREAT_RANGE);
      expect(parseComponentsThreatRange('5-')).toEqual(DEFAULT_COMPONENTS_THREAT_RANGE);
      expect(parseComponentsThreatRange('-1-5')).toEqual(DEFAULT_COMPONENTS_THREAT_RANGE);
    });

    it('keeps a level-0-only filter (does not collapse to default)', () => {
      expect(parseComponentsThreatRange('0-0')).toEqual([0, 0]);
    });

    it('clamps and orders an inverted range', () => {
      expect(parseComponentsThreatRange('9-3')).toEqual([3, 9]);
      expect(parseComponentsThreatRange('0-99')).toEqual([0, 10]);
    });
  });

  it('round-trips organization names that contain commas', () => {
    const filters = {
      ...EMPTY_COMPONENTS_LIST_FILTERS,
      organizations: new Set(['Widgets, Inc.', 'Research & Development, EMEA']),
      ecosystems: new Set(['maven']),
    };
    const params = buildComponentsListRouteParams({
      tab: 'myScanData',
      search: '',
      page: 0,
      filters,
    });
    expect(params.org).toContain('%2C');
    const parsed = parseComponentsListParams(params);
    expect(Array.from(parsed.filters.organizations).sort()).toEqual([
      'Research & Development, EMEA',
      'Widgets, Inc.',
    ]);
    expect(Array.from(parsed.filters.ecosystems)).toEqual(['maven']);
  });
});
