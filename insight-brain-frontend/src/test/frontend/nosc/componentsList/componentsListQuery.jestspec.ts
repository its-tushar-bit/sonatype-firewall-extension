/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildComponentsListRouteParams,
  parseComponentsListParams,
} from 'MainRoot/nosc/componentsList/componentsListQuery';

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
        filters: {
          organizations: new Set(),
          ecosystems: new Set(),
        },
      }),
    ).toEqual({});
  });

  it('serializes catalog source and omits org filters on the catalog tab', () => {
    expect(
      buildComponentsListRouteParams({
        tab: 'catalog',
        search: 'lodash',
        page: 1,
        filters: {
          organizations: new Set(['Java Team']),
          ecosystems: new Set(['npm']),
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
      organizations: new Set(['Java Team']),
      ecosystems: new Set(['maven']),
    };
    const params = buildComponentsListRouteParams({
      tab: 'myScanData',
      search: 'guava',
      page: 0,
      filters,
    });
    const parsed = parseComponentsListParams(params);
    expect(parsed.tab).toBe('myScanData');
    expect(parsed.search).toBe('guava');
    expect(Array.from(parsed.filters.organizations)).toEqual(['Java Team']);
    expect(Array.from(parsed.filters.ecosystems)).toEqual(['maven']);
  });

  it('round-trips organization names that contain commas', () => {
    const filters = {
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
