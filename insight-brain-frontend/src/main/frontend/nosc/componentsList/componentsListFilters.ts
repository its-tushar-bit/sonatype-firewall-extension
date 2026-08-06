/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ComponentsFilterFacetEntry } from 'MainRoot/nosc/componentsList/componentListTypes';
import { normalizeRange } from 'MainRoot/nosc/util/normalizeRange';

/** Orgs/ecosystems show this many options before "See more" (Applications-pattern AC). */
export const FACET_COLLAPSE_LIMIT = 8;

/** Integer policy threat domain for the Components My Scan Data slider (Applications parity). */
export const COMPONENTS_THREAT_MIN = 0;
export const COMPONENTS_THREAT_MAX = 10;

export type ComponentsThreatRange = readonly [number, number];

export const DEFAULT_COMPONENTS_THREAT_RANGE: ComponentsThreatRange = [
  COMPONENTS_THREAT_MIN,
  COMPONENTS_THREAT_MAX,
];

/**
 * Sidebar filter selection for Components (Ana catalog schema + My Scan Data dashboard).
 * {@code organizations} values are friendly org names (local source only).
 * {@code applications} and {@code stages} are My Scan Data only (CLM-43211); the Catalog source
 * rejects them, so switching tabs clears them rather than sending a request that would 400.
 * {@code threatRange} is My Scan Data only (CLM-43960).
 */
export type ComponentsListFilterState = {
  readonly organizations: ReadonlySet<string>;
  readonly ecosystems: ReadonlySet<string>;
  readonly applications: ReadonlySet<string>;
  readonly stages: ReadonlySet<string>;
  readonly threatRange: ComponentsThreatRange;
};

export type ComponentsFilterSetGroup =
  | 'organizations'
  | 'ecosystems'
  | 'applications'
  | 'stages';

export const EMPTY_COMPONENTS_LIST_FILTERS: ComponentsListFilterState = {
  organizations: new Set(),
  ecosystems: new Set(),
  applications: new Set(),
  stages: new Set(),
  threatRange: DEFAULT_COMPONENTS_THREAT_RANGE,
};

/**
 * Full-domain span means "no threat filter" (Applications / Violations slider parity). Narrow
 * ranges such as {@code [0, 0]} still filter level-0-only; only {@code [0, 10]} is treated as unset.
 */
export function isDefaultComponentsThreatRange(range: ComponentsThreatRange): boolean {
  return range[0] <= COMPONENTS_THREAT_MIN && range[1] >= COMPONENTS_THREAT_MAX;
}

export function normalizeComponentsThreatRange(
  next: readonly number[],
): ComponentsThreatRange {
  return normalizeRange(next, COMPONENTS_THREAT_MIN, COMPONENTS_THREAT_MAX);
}

export function hasActiveComponentsListFilters(filters: ComponentsListFilterState): boolean {
  return (
    filters.organizations.size > 0
    || filters.ecosystems.size > 0
    || filters.applications.size > 0
    || filters.stages.size > 0
    || !isDefaultComponentsThreatRange(filters.threatRange)
  );
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
 * Threat range is My Scan Data dashboard–only and is not part of the catalog filter schema.
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
    && setEqual(left.applications, right.applications)
    && setEqual(left.stages, right.stages)
    && left.threatRange[0] === right.threatRange[0]
    && left.threatRange[1] === right.threatRange[1]
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
