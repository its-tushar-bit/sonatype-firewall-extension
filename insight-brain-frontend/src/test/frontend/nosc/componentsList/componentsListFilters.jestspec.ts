/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  collapseFacetEntries,
  componentsListFiltersToCatalogFilters,
  type ComponentsListFilterState,
  EMPTY_COMPONENTS_LIST_FILTERS,
  FACET_COLLAPSE_LIMIT,
  hasActiveComponentsListFilters,
} from 'MainRoot/nosc/componentsList/componentsListFilters';

const manyOrgs = Array.from({ length: 12 }, (_, index) => ({
  id: `org-${index}`,
  label: `Org ${index}`,
  count: index + 1,
}));

describe('componentsListFilters', () => {
  it('collapseFacetEntries keeps the first 8 rows and reports the remainder', () => {
    const collapsed = collapseFacetEntries(manyOrgs, new Set(), FACET_COLLAPSE_LIMIT, false);
    expect(collapsed).toHaveLength(8);
    expect(collapsed[0].id).toBe('org-0');
    expect(collapsed[7].id).toBe('org-7');
  });

  it('collapseFacetEntries shows all rows when expanded', () => {
    expect(collapseFacetEntries(manyOrgs, new Set(), FACET_COLLAPSE_LIMIT, true)).toHaveLength(12);
  });

  it('collapseFacetEntries is a no-op when length is at or below the limit', () => {
    const short = manyOrgs.slice(0, 5);
    expect(collapseFacetEntries(short, new Set(), FACET_COLLAPSE_LIMIT, false)).toEqual(short);
  });

  it('detects an active selection in any filter group', () => {
    const active: ReadonlyArray<Partial<ComponentsListFilterState>> = [
      { organizations: new Set(['Java Team']) },
      { ecosystems: new Set(['npm']) },
      { applications: new Set(['app-1']) },
      { stages: new Set(['build']) },
      { threatRange: [3, 9] as const },
    ];
    active.forEach((selection) => {
      expect(
        hasActiveComponentsListFilters({ ...EMPTY_COMPONENTS_LIST_FILTERS, ...selection }),
      ).toBe(true);
    });
    expect(hasActiveComponentsListFilters(EMPTY_COMPONENTS_LIST_FILTERS)).toBe(false);
  });

  it('maps active filters into catalog filter fields and omits orgs when disabled', () => {
    const filters = {
      ...EMPTY_COMPONENTS_LIST_FILTERS,
      organizations: new Set(['Java Team']),
      ecosystems: new Set(['maven']),
    };
    expect(componentsListFiltersToCatalogFilters(filters, { includeOrganizations: true })).toEqual({
      organizations: ['Java Team'],
      ecosystems: ['maven'],
    });
    expect(componentsListFiltersToCatalogFilters(filters, { includeOrganizations: false })).toEqual({
      ecosystems: ['maven'],
    });
  });
});
