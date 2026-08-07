/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildComponentsCatalogRequest,
  buildComponentsDashboardRequest,
  COMPONENTS_LIST_PAGE_SIZE,
  mapCatalogComponentRow,
  mapComponentsCatalogResponse,
  mapComponentsDashboardResponse,
} from 'MainRoot/nosc/componentsList/componentsListApi';
import { EMPTY_COMPONENTS_LIST_FILTERS } from 'MainRoot/nosc/componentsList/componentsListFilters';

describe('componentsListApi (catalog)', () => {
  it('builds a local My Scan Data catalog request with friendly org names', () => {
    expect(
      buildComponentsCatalogRequest({
        tab: 'myScanData',
        page: 0,
        search: 'guava',
        filters: {
          ...EMPTY_COMPONENTS_LIST_FILTERS,
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
        ...EMPTY_COMPONENTS_LIST_FILTERS,
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

  it('maps Guide catalog byFormat aggregations into Ecosystems facets (CLM-44657)', () => {
    const mapped = mapComponentsCatalogResponse({
      source: 'catalog',
      catalogAvailable: true,
      page: 1,
      pageSize: 50,
      totalEstimate: 100,
      rows: [
        {
          id: 'pkg:npm/left-pad@1.3.0',
          title: 'left-pad',
          subtitle: '1.3.0',
          source: 'catalog',
          fields: { ecosystem: 'npm' },
        },
      ],
      facets: {
        byFormat: [
          { value: 'maven', count: 819831 },
          { value: 'npm', count: 1200 },
        ],
        byLicense: [{ value: 'MIT', count: 50 }],
      },
    });

    expect(mapped.facets.ecosystems).toEqual([
      { id: 'maven', label: 'maven', count: 819831 },
      { id: 'npm', label: 'npm', count: 1200 },
    ]);
    // Federated Catalog never surfaces org facets (My Scan owns estate filters).
    expect(mapped.facets.organizations).toEqual([]);
  });

  it('prefers byFormat over ecosystem when both are present', () => {
    const mapped = mapComponentsCatalogResponse({
      source: 'catalog',
      catalogAvailable: true,
      page: 1,
      pageSize: 50,
      totalEstimate: 5,
      rows: [],
      facets: {
        byFormat: [{ value: 'maven', count: 10 }],
        ecosystem: [{ value: 'npm', count: 5 }],
      },
    });
    expect(mapped.facets.ecosystems).toEqual([{ id: 'maven', label: 'maven', count: 10 }]);
  });

  it('ignores byOrganization on federated Catalog even when Guide returns it', () => {
    const mapped = mapComponentsCatalogResponse({
      source: 'catalog',
      catalogAvailable: true,
      page: 1,
      pageSize: 50,
      totalEstimate: 5,
      rows: [],
      facets: {
        byFormat: [{ value: 'npm', count: 3 }],
        byOrganization: [{ value: 'some-guide-org', count: 9 }],
      },
    });
    expect(mapped.facets.ecosystems).toEqual([{ id: 'npm', label: 'npm', count: 3 }]);
    expect(mapped.facets.organizations).toEqual([]);
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

describe('componentsListApi (My Scan Data dashboard)', () => {
  it('serializes organization, application, and stage selections (CLM-43211)', () => {
    expect(
      buildComponentsDashboardRequest({
        page: 0,
        filters: {
          ...EMPTY_COMPONENTS_LIST_FILTERS,
          organizations: new Set(['org-1']),
          applications: new Set(['app-2', 'app-1']),
          stages: new Set(['release', 'build']),
        },
      }),
    ).toEqual({
      page: 0,
      pageSize: COMPONENTS_LIST_PAGE_SIZE,
      includeFacets: true,
      organizationIds: ['org-1'],
      applicationIds: ['app-1', 'app-2'],
      stageIds: ['build', 'release'],
    });
  });

  it('omits scope keys the user has not selected', () => {
    const request = buildComponentsDashboardRequest({ page: 0 });
    expect(request).not.toHaveProperty('applicationIds');
    expect(request).not.toHaveProperty('stageIds');
    expect(request).not.toHaveProperty('policyThreatLevelRanges');
  });

  it('serializes a non-default threat range as policyThreatLevelRanges', () => {
    expect(
      buildComponentsDashboardRequest({
        page: 0,
        filters: {
          ...EMPTY_COMPONENTS_LIST_FILTERS,
          threatRange: [3, 9],
        },
      }),
    ).toMatchObject({
      policyThreatLevelRanges: [{ minPolicyThreatLevel: 3, maxPolicyThreatLevel: 9 }],
    });
  });

  it('maps application and stage facets, labelling by name where the backend resolved one', () => {
    const mapped = mapComponentsDashboardResponse({
      total: 9,
      components: [],
      facets: {
        totalComponents: 9,
        applications: { 'app-1': 7, 'app-2': 2 },
        stages: { build: 9, release: 4 },
        applicationNames: { 'app-1': 'Checkout' },
        stageNames: { build: 'Build', release: 'Release' },
      },
    });

    expect(mapped.facets.applications).toEqual([
      { id: 'app-2', label: 'app-2', count: 2 },
      { id: 'app-1', label: 'Checkout', count: 7 },
    ]);
    expect(mapped.facets.stages).toEqual([
      { id: 'build', label: 'Build', count: 9 },
      { id: 'release', label: 'Release', count: 4 },
    ]);
  });

  it('leaves application and stage facets empty when the backend omitted them', () => {
    const mapped = mapComponentsDashboardResponse({ total: 0, components: [] });
    expect(mapped.facets.applications).toEqual([]);
    expect(mapped.facets.stages).toEqual([]);
  });
});
