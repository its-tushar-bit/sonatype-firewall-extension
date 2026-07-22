/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildComponentsCatalogRequest,
  mapCatalogComponentRow,
  mapComponentsCatalogResponse,
} from 'MainRoot/nosc/componentsList/componentsListApi';

describe('componentsListApi (catalog)', () => {
  it('builds a local My Scan Data catalog request with friendly org names', () => {
    expect(
      buildComponentsCatalogRequest({
        tab: 'myScanData',
        page: 0,
        search: 'guava',
        filters: {
          organizations: new Set(['Java Team']),
          ecosystems: new Set(['maven']),
        },
      }),
    ).toEqual({
      entityType: 'COMPONENT',
      source: 'local',
      page: 1,
      pageSize: 50,
      includeFacets: true,
      filters: {
        query: 'guava',
        organizations: ['Java Team'],
        ecosystems: ['maven'],
      },
    });
  });

  it('omits organizations on the Sonatype Catalog tab', () => {
    const request = buildComponentsCatalogRequest({
      tab: 'catalog',
      page: 2,
      filters: {
        organizations: new Set(['Java Team']),
        ecosystems: new Set(['npm']),
      },
    });
    expect(request.source).toBe('catalog');
    expect(request.page).toBe(3);
    expect(request.filters).toEqual({ ecosystems: ['npm'] });
  });

  it('maps catalog rows and friendly organization facet values', () => {
    const mapped = mapComponentsCatalogResponse({
      source: 'local',
      page: 1,
      pageSize: 50,
      totalEstimate: 2,
      rows: [
        {
          id: 'guava',
          title: 'guava',
          subtitle: '31.1',
          source: 'local',
          fields: { ecosystem: 'maven', organization: 'Java Team' },
        },
      ],
      facets: {
        organization: [{ value: 'Java Team', count: 5 }],
        ecosystem: [{ value: 'maven', count: 10 }],
      },
      nextSearchAfter: 'cursor-2',
    });

    expect(mapped.components).toEqual([
      {
        id: 'guava',
        name: 'guava',
        subtitle: '31.1',
        ecosystem: 'maven',
        organization: 'Java Team',
        source: 'local',
      },
    ]);
    expect(mapped.facets.organizations).toEqual([{ id: 'Java Team', label: 'Java Team', count: 5 }]);
    expect(mapped.facets.ecosystems).toEqual([{ id: 'maven', label: 'maven', count: 10 }]);
    expect(mapped.hasNextPage).toBe(true);
    expect(mapped.nextSearchAfter).toBe('cursor-2');
  });

  it('drops rows without an id', () => {
    expect(mapCatalogComponentRow({ title: 'orphan' })).toBeNull();
  });

  it('treats catalogAvailable=false as unknown total (0) with no next page', () => {
    const mapped = mapComponentsCatalogResponse({
      source: 'catalog',
      catalogAvailable: false,
      page: 1,
      pageSize: 50,
      totalEstimate: 0,
      rows: [],
    });
    expect(mapped.catalogAvailable).toBe(false);
    expect(mapped.total).toBe(0);
    expect(mapped.hasNextPage).toBe(false);
  });

  it('preserves exactTotalEstimate=false for capped totals', () => {
    const mapped = mapComponentsCatalogResponse({
      source: 'catalog',
      page: 1,
      pageSize: 50,
      totalEstimate: 10000,
      exactTotalEstimate: false,
      rows: [{ id: 'a', title: 'a', source: 'catalog' }],
    });
    expect(mapped.exactTotalEstimate).toBe(false);
    expect(mapped.total).toBe(10000);
  });
});
