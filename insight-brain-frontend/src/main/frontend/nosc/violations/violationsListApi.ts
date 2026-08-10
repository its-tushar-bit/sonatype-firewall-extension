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
  ViolationWaiverType,
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
    applicationCategoryIds: new Set<string>(),
    threatRange: DEFAULT_VIOLATION_THREAT_RANGE,
    waiverType: 'ANY',
  };
}

/** True when the threat range covers the whole [0, 10] domain (so it needn't be sent to the API). */
export function isDefaultThreatRange(range: ViolationThreatRange): boolean {
  return range[0] <= VIOLATION_THREAT_MIN && range[1] >= VIOLATION_THREAT_MAX;
}

/**
 * True when a filter the Classic CSV export can honor is active. Excludes {@code waiverType}, which is
 * index-only ({@code RisksFilterDTO} has no auto-waiver field) — the toolbar treats it like free-text
 * search and warns that it is not applied to the export.
 */
export function hasExportableViolationFilters(filters: ViolationsFilterState): boolean {
  return (
    filters.states.size > 0 ||
    filters.threatCategories.size > 0 ||
    filters.stageIds.size > 0 ||
    filters.organizationIds.size > 0 ||
    filters.applicationIds.size > 0 ||
    filters.applicationCategoryIds.size > 0 ||
    !isDefaultThreatRange(filters.threatRange)
  );
}

/** True when any filter group is narrowing the result set (drives the Reset control's enabled state). */
export function hasActiveViolationFilters(filters: ViolationsFilterState): boolean {
  return hasExportableViolationFilters(filters) || filters.waiverType !== 'ANY';
}

/** Map the sidebar waiver-type selection to the backend {@code waivedWithAutoWaiver} boolean. */
export function waiverTypeToRequestFlag(waiverType: ViolationWaiverType): boolean | undefined {
  if (waiverType === 'AUTO') return true;
  if (waiverType === 'MANUAL') return false;
  return undefined;
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
  readonly orderBy?: string;
  readonly filters?: ViolationsFilterState;
  readonly organizationFacetSearch?: string;
  readonly applicationFacetSearch?: string;
  /** Exact component hash scope for estate Component Detail (CLM-43961). */
  readonly componentHash?: string;
}): ViolationsListRequest {
  const search = params.search?.trim();
  const organizationFacetSearch = params.organizationFacetSearch?.trim();
  const applicationFacetSearch = params.applicationFacetSearch?.trim();
  const componentHash = params.componentHash?.trim();
  const filters = params.filters;

  const states = filters ? toSortedArray(filters.states) : undefined;
  const stageIds = filters ? toSortedArray(filters.stageIds) : undefined;
  const organizationIds = filters ? toSortedArray(filters.organizationIds) : undefined;
  const applicationIds = filters ? toSortedArray(filters.applicationIds) : undefined;
  const applicationCategoryIds = filters ? toSortedArray(filters.applicationCategoryIds) : undefined;
  const categories = filters ? toSortedArray(filters.threatCategories) : undefined;
  const threatRange =
    filters && !isDefaultThreatRange(filters.threatRange)
      ? `${filters.threatRange[0]},${filters.threatRange[1]}`
      : undefined;
  const waivedWithAutoWaiver = filters ? waiverTypeToRequestFlag(filters.waiverType) : undefined;

  return {
    page: params.page,
    pageSize: params.pageSize ?? VIOLATIONS_PAGE_SIZE,
    includeFacets: params.includeFacets ?? true,
    orderBy: params.orderBy ?? VIOLATIONS_DEFAULT_ORDER_BY,
    ...(search ? { search } : {}),
    ...(states ? { policyViolationStates: states } : {}),
    ...(categories ? { policyThreatCategories: categories.join(',') } : {}),
    ...(threatRange ? { policyThreatLevelRange: threatRange } : {}),
    ...(stageIds ? { stageIds } : {}),
    ...(organizationIds ? { organizationIds } : {}),
    ...(applicationIds ? { applicationIds } : {}),
    ...(applicationCategoryIds ? { applicationCategoryIds } : {}),
    ...(waivedWithAutoWaiver !== undefined ? { waivedWithAutoWaiver } : {}),
    ...(organizationFacetSearch ? { organizationFacetSearch } : {}),
    ...(applicationFacetSearch ? { applicationFacetSearch } : {}),
    ...(componentHash ? { componentHash } : {}),
  };
}

/**
 * Friendly labels for the enum-keyed violation-state facet (OPEN / WAIVED / LEGACY_VIOLATION). Exported
 * so the URL codec can derive its accepted-token allow-list from these keys instead of duplicating the
 * set. Keys are the API enum names (LEGACY_VIOLATION), not the index-side status string ("Legacy").
 */
export const STATE_LABELS: Readonly<Record<string, string>> = {
  OPEN: 'Open',
  WAIVED: 'Waived',
  LEGACY_VIOLATION: 'Legacy',
};

/**
 * Facet keys and friendly labels for the waiver-type radio (CLM-42261). Keys mirror the backend
 * {@code ViolationsListFacetsBuilder} facet map ({@code AUTO} / {@code MANUAL}); {@code ANY} is the
 * radio's unfiltered option and carries no facet count.
 */
export const WAIVER_TYPE_AUTO = 'AUTO';
export const WAIVER_TYPE_MANUAL = 'MANUAL';

export const WAIVER_TYPE_LABELS: Readonly<Record<string, string>> = {
  [WAIVER_TYPE_AUTO]: 'Auto-waived',
  [WAIVER_TYPE_MANUAL]: 'Manually waived',
};

export function waiverTypeLabel(id: string): string {
  return WAIVER_TYPE_LABELS[id] ?? id;
}

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
 * Derive id→display-name maps for the org / application facets.
 * <p>
 * Page rows seed the maps first; non-empty server {@code organizationNames}/{@code applicationNames}
 * overlay and win for shared keys — same merge order as Applications {@code mergeLabelMap}. Off-page
 * facet keys (including server-side facet-search matches, CLM-42912) stay friendly from the facets
 * payload. Ids still without a name fall back to the raw id (and for applications,
 * {@code applicationPublicId} from a page row when present). Stage facets use {@link stageLabel}.
 */
export function deriveViolationFacetLabels(
  rows: ReadonlyArray<ViolationRow>,
  facetNames?: {
    readonly organizations?: Readonly<Record<string, string>>;
    readonly applications?: Readonly<Record<string, string>>;
  },
): {
  readonly organizations: Readonly<Record<string, string>>;
  readonly applications: Readonly<Record<string, string>>;
} {
  const organizations: Record<string, string> = {};
  const applications: Record<string, string> = {};
  rows.forEach((row) => {
    if (row.organizationId && row.organizationName) {
      organizations[row.organizationId] = row.organizationName;
    }
    if (row.applicationId && row.applicationName) {
      applications[row.applicationId] = row.applicationName;
    } else if (row.applicationId && row.applicationPublicId && !applications[row.applicationId]) {
      applications[row.applicationId] = row.applicationPublicId;
    }
  });
  overlayFacetNameMap(organizations, facetNames?.organizations);
  overlayFacetNameMap(applications, facetNames?.applications);
  return { organizations, applications };
}

function overlayFacetNameMap(
  target: Record<string, string>,
  source: Readonly<Record<string, string>> | undefined,
): void {
  if (!source) return;
  Object.entries(source).forEach(([id, label]) => {
    if (id.trim().length > 0 && label.trim().length > 0) {
      target[id] = label;
    }
  });
}
