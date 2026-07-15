/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ViolationRow,
  ViolationsFilterState,
  ViolationsListRequest,
  ViolationThreatRange,
  VIOLATION_THREAT_MAX,
  VIOLATION_THREAT_MIN,
} from 'MainRoot/nosc/violations/violationListTypes';

/** Default page size for the Violations card list. */
export const VIOLATIONS_PAGE_SIZE = 25;

/** Default sort — highest threat first (backend default; see ViolationsListRequestDTO). */
export const VIOLATIONS_DEFAULT_ORDER_BY = '-policyThreatLevel';

/** Full [0, 10] policy-threat-level range — the "no narrowing" default. */
export const DEFAULT_VIOLATION_THREAT_RANGE: ViolationThreatRange = [
  VIOLATION_THREAT_MIN,
  VIOLATION_THREAT_MAX,
];

/** A fresh, empty filter selection (all groups cleared, threat range at the full [0, 10] domain). */
export function createDefaultViolationsFilterState(): ViolationsFilterState {
  return {
    states: new Set<string>(),
    threatCategories: new Set<string>(),
    stageIds: new Set<string>(),
    organizationIds: new Set<string>(),
    applicationIds: new Set<string>(),
    threatRange: DEFAULT_VIOLATION_THREAT_RANGE,
  };
}

/** True when the threat range covers the whole [0, 10] domain (so it needn't be sent to the API). */
export function isDefaultThreatRange(range: ViolationThreatRange): boolean {
  return range[0] <= VIOLATION_THREAT_MIN && range[1] >= VIOLATION_THREAT_MAX;
}

/** True when any filter group is narrowing the result set (drives the Reset control's enabled state). */
export function hasActiveViolationFilters(filters: ViolationsFilterState): boolean {
  return (
    filters.states.size > 0 ||
    filters.threatCategories.size > 0 ||
    filters.stageIds.size > 0 ||
    filters.organizationIds.size > 0 ||
    filters.applicationIds.size > 0 ||
    !isDefaultThreatRange(filters.threatRange)
  );
}

/**
 * Sorted array from a selection set, or undefined when empty (so the field is omitted from the body).
 * Exported so the list-request body and the CSV export payload share one serialization helper.
 */
export function toSortedArray(ids: ReadonlySet<string>): ReadonlyArray<string> | undefined {
  return ids.size > 0 ? Array.from(ids).sort() : undefined;
}

/**
 * Build the POST body for a Violations list request. Only validator-safe fields are sent; {@code page}
 * is 0-based. Filter selections are serialized to the backend wire formats: state names as an array,
 * categories and the threat range as comma-delimited strings, and stage/org/app ids as arrays.
 * A default [0, 10] threat range and empty groups are omitted so an unfiltered request stays minimal.
 */
export function buildViolationsListRequest(params: {
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly includeFacets?: boolean;
  readonly filters?: ViolationsFilterState;
}): ViolationsListRequest {
  const search = params.search?.trim();
  const filters = params.filters;

  const states = filters ? toSortedArray(filters.states) : undefined;
  const stageIds = filters ? toSortedArray(filters.stageIds) : undefined;
  const organizationIds = filters ? toSortedArray(filters.organizationIds) : undefined;
  const applicationIds = filters ? toSortedArray(filters.applicationIds) : undefined;
  const categories = filters ? toSortedArray(filters.threatCategories) : undefined;
  const threatRange =
    filters && !isDefaultThreatRange(filters.threatRange)
      ? `${filters.threatRange[0]},${filters.threatRange[1]}`
      : undefined;

  return {
    page: params.page,
    pageSize: params.pageSize ?? VIOLATIONS_PAGE_SIZE,
    includeFacets: params.includeFacets ?? true,
    orderBy: VIOLATIONS_DEFAULT_ORDER_BY,
    ...(search ? { search } : {}),
    ...(states ? { policyViolationStates: states } : {}),
    ...(categories ? { policyThreatCategories: categories.join(',') } : {}),
    ...(threatRange ? { policyThreatLevelRange: threatRange } : {}),
    ...(stageIds ? { stageIds } : {}),
    ...(organizationIds ? { organizationIds } : {}),
    ...(applicationIds ? { applicationIds } : {}),
  };
}

/**
 * Friendly labels for the enum-keyed violation-state facet (OPEN / WAIVED). Exported so the URL codec
 * can derive its accepted-token allow-list from these keys instead of duplicating the set.
 */
export const STATE_LABELS: Readonly<Record<string, string>> = {
  OPEN: 'Open',
  WAIVED: 'Waived',
};

/**
 * Friendly labels for the policy threat-category facet. Exported so the URL codec can derive its
 * accepted-token allow-list from these keys instead of duplicating the set.
 */
export const THREAT_CATEGORY_LABELS: Readonly<Record<string, string>> = {
  security: 'Security',
  license: 'License',
  quality: 'Quality',
  other: 'Other',
};

/**
 * Friendly labels for the licensed pipeline-stage facet, keyed by stage id. The API keys the
 * {@code stages} facet map by stage id (e.g. {@code build}, {@code stage-release}), while a row's
 * {@code stage} is the resolved display name — so an id→name map cannot be derived from the page rows
 * (unlike org/app rows, which carry both id and name). This authoritative map covers the IQ lifecycle
 * stages; any unknown/future id falls back to a Title-Cased id so the sidebar never renders a raw slug.
 */
const STAGE_LABELS: Readonly<Record<string, string>> = {
  proxy: 'Proxy',
  develop: 'Develop',
  source: 'Source',
  build: 'Build',
  'stage-release': 'Stage Release',
  release: 'Release',
  operate: 'Operate',
};

export function violationStateLabel(id: string): string {
  return STATE_LABELS[id] ?? id;
}

export function threatCategoryLabel(id: string): string {
  return THREAT_CATEGORY_LABELS[id] ?? id;
}

/** Title-case a hyphenated stage id (e.g. {@code stage-release} → {@code Stage Release}). */
function titleCaseStageId(id: string): string {
  return id
    .split('-')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

/**
 * Map a stage-facet id to a display name. Uses the authoritative {@link STAGE_LABELS} map and falls
 * back to a Title-Cased id (never the raw slug) for any id not yet in the map.
 */
export function stageLabel(id: string): string {
  return STAGE_LABELS[id] ?? titleCaseStageId(id);
}

/**
 * Derive id→display-name maps for the org / application facets from the current page of rows. Facet
 * maps are keyed by internal id only; org/app rows carry both the id and the human-readable name, so
 * we use them to label the sidebar for entities visible on the page. Ids with no matching row fall
 * back to the raw id when no row supplies a name. Stage facets are labeled by {@link stageLabel}
 * instead — a row's {@code stage} is a display name, not the id the facet is keyed by, so it cannot
 * seed an id→name map.
 *
 * Known V1 limitation (accepted tradeoff): labels only cover entities present on the current page of
 * rows (pageSize 25). Org/app facet entries for entities not on the page therefore render their raw
 * id and can't be matched by the sidebar's name-search. The durable fix is server-side — return an
 * id→name map in the facets payload (or a dedicated labels endpoint) so labels don't depend on which
 * rows happen to be visible — tracked in CLM-42443. Until then this row-derived map is the V1 behavior.
 */
export function deriveViolationFacetLabels(rows: ReadonlyArray<ViolationRow>): {
  readonly organizations: Readonly<Record<string, string>>;
  readonly applications: Readonly<Record<string, string>>;
} {
  const organizations: Record<string, string> = {};
  const applications: Record<string, string> = {};
  rows.forEach((row) => {
    if (row.organizationId && row.organizationName) organizations[row.organizationId] = row.organizationName;
    if (row.applicationId && row.applicationName) applications[row.applicationId] = row.applicationName;
  });
  return { organizations, applications };
}
