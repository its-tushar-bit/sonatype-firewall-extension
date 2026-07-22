/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ComponentsFilterFacetEntry } from 'MainRoot/nosc/componentsList/componentListTypes';

/** Orgs/ecosystems show this many options before "See more" (Applications-pattern AC). */
export const FACET_COLLAPSE_LIMIT = 8;

/**
 * Sidebar filter selection for Components (Ana catalog schema).
 * {@code organizations} values are friendly org names (local source only).
 */
export type ComponentsListFilterState = {
  readonly organizations: ReadonlySet<string>;
  readonly ecosystems: ReadonlySet<string>;
};

export type ComponentsFilterSetGroup = 'organizations' | 'ecosystems';

export const EMPTY_COMPONENTS_LIST_FILTERS: ComponentsListFilterState = {
  organizations: new Set(),
  ecosystems: new Set(),
};

export function hasActiveComponentsListFilters(filters: ComponentsListFilterState): boolean {
  return filters.organizations.size > 0 || filters.ecosystems.size > 0;
}

export function toggleComponentsListFilterId(
  filters: ComponentsListFilterState,
  field: ComponentsFilterSetGroup,
  id: string,
): ComponentsListFilterState {
  const next = new Set(filters[field]);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  return { ...filters, [field]: next };
}

function toSortedArray(values: ReadonlySet<string>): ReadonlyArray<string> | undefined {
  if (values.size === 0) return undefined;
  return Array.from(values).sort();
}

/**
 * Maps sidebar selection into catalog {@code filters} (organizations / ecosystems TERMS).
 * Organization filter is omitted when {@code includeOrganizations} is false (Sonatype Catalog tab).
 */
export function componentsListFiltersToCatalogFilters(
  filters: ComponentsListFilterState,
  options: { readonly includeOrganizations: boolean },
): Record<string, unknown> {
  const organizations =
    options.includeOrganizations ? toSortedArray(filters.organizations) : undefined;
  const ecosystems = toSortedArray(filters.ecosystems);
  return {
    ...(organizations ? { organizations } : {}),
    ...(ecosystems ? { ecosystems } : {}),
  };
}

export function filtersEqual(
  left: ComponentsListFilterState,
  right: ComponentsListFilterState,
): boolean {
  return (
    setEqual(left.organizations, right.organizations)
    && setEqual(left.ecosystems, right.ecosystems)
  );
}

function setEqual(left: ReadonlySet<string>, right: ReadonlySet<string>): boolean {
  if (left.size !== right.size) return false;
  for (const value of left) {
    if (!right.has(value)) return false;
  }
  return true;
}

/**
 * Collapse facet entries for See more / See less. Selected ids outside the collapsed window
 * stay visible so the user can clear them.
 */
export function collapseFacetEntries(
  entries: ReadonlyArray<ComponentsFilterFacetEntry>,
  selected: ReadonlySet<string>,
  limit: number = FACET_COLLAPSE_LIMIT,
  expanded: boolean = false,
): ReadonlyArray<ComponentsFilterFacetEntry> {
  if (expanded || entries.length <= limit) return entries;
  const head = entries.slice(0, limit);
  const headIds = new Set(head.map((entry) => entry.id));
  const extras = entries.filter((entry) => selected.has(entry.id) && !headIds.has(entry.id));
  return extras.length === 0 ? head : [...head, ...extras];
}
