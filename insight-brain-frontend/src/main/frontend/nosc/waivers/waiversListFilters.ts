/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { THREAT_GROUPS } from 'MainRoot/nosc/applications/applicationDetailUtils';

/** Policy threat-level bucket ids — aligned with the Applications rail so filter labels match. */
export type WaiversThreatLevelId = (typeof THREAT_GROUPS)[number]['group'];

/** {@code Active|Expired|Never} maps 1:1 to the backend {@code policyWaiverExpiryStatus} keyword. */
export type WaiversExpiryStatusId = 'Active' | 'Expired' | 'Never';

/** {@code Auto|Manual} — collapsed to a single {@code includeAutoWaivers} boolean at the wire. */
export type WaiversAutoStatusId = 'Auto' | 'Manual';

function parseThreatGroupRange(range: string): readonly [number, number] {
  if (range === '0') {
    return [0, 0];
  }
  if (range.includes('-')) {
    const [min, max] = range.split('-').map((part) => Number(part));
    return [min, max];
  }
  const value = Number(range);
  return [value, value];
}

const THREAT_GROUP_RANGES = Object.fromEntries(
  THREAT_GROUPS.map(({ group, range }) => [group, parseThreatGroupRange(range)]),
) as Record<WaiversThreatLevelId, readonly [number, number]>;

const SELECTABLE_THREAT_LEVEL_IDS = new Set<WaiversThreatLevelId>([
  'Critical',
  'Severe',
  'Moderate',
  'Low',
]);

const SELECTABLE_EXPIRY_STATUS_IDS = new Set<WaiversExpiryStatusId>(['Active', 'Expired', 'Never']);
const SELECTABLE_AUTO_STATUS_IDS = new Set<WaiversAutoStatusId>(['Auto', 'Manual']);

export function isSelectableThreatLevelId(value: string): value is WaiversThreatLevelId {
  return SELECTABLE_THREAT_LEVEL_IDS.has(value as WaiversThreatLevelId);
}

export function isSelectableExpiryStatusId(value: string): value is WaiversExpiryStatusId {
  return SELECTABLE_EXPIRY_STATUS_IDS.has(value as WaiversExpiryStatusId);
}

export function isSelectableAutoStatusId(value: string): value is WaiversAutoStatusId {
  return SELECTABLE_AUTO_STATUS_IDS.has(value as WaiversAutoStatusId);
}

export type WaiversListFilterState = {
  readonly threatLevelIds: ReadonlySet<WaiversThreatLevelId>;
  readonly expiryStatusIds: ReadonlySet<WaiversExpiryStatusId>;
  readonly autoStatusIds: ReadonlySet<WaiversAutoStatusId>;
  readonly organizationIds: ReadonlySet<string>;
  readonly applicationIds: ReadonlySet<string>;
  readonly policyIds: ReadonlySet<string>;
};

export type WaiversFilterSetGroup = keyof WaiversListFilterState;

export const EMPTY_WAIVERS_LIST_FILTERS: WaiversListFilterState = {
  threatLevelIds: new Set(),
  expiryStatusIds: new Set(),
  autoStatusIds: new Set(),
  organizationIds: new Set(),
  applicationIds: new Set(),
  policyIds: new Set(),
};

export function hasActiveWaiversListFilters(filters: WaiversListFilterState): boolean {
  return (
    filters.threatLevelIds.size > 0
    || filters.expiryStatusIds.size > 0
    || filters.autoStatusIds.size > 0
    || filters.organizationIds.size > 0
    || filters.applicationIds.size > 0
    || filters.policyIds.size > 0
  );
}

/**
 * Toggle a single id in one of the sidebar sets. Silently no-ops when the id is not a
 * selectable value for the group (e.g. threat=None, expiry=Bogus) so URL/state stays
 * clean and callers do not need to guard.
 */
export function toggleWaiversListFilterId(
  filters: WaiversListFilterState,
  group: WaiversFilterSetGroup,
  id: string,
): WaiversListFilterState {
  if (group === 'threatLevelIds' && !isSelectableThreatLevelId(id)) return filters;
  if (group === 'expiryStatusIds' && !isSelectableExpiryStatusId(id)) return filters;
  if (group === 'autoStatusIds' && !isSelectableAutoStatusId(id)) return filters;
  if (!id.trim()) return filters;
  const current = filters[group] as ReadonlySet<string>;
  const next = new Set(current);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  return { ...filters, [group]: next };
}

export function filtersEqual(
  left: WaiversListFilterState,
  right: WaiversListFilterState,
): boolean {
  const fields: WaiversFilterSetGroup[] = [
    'threatLevelIds',
    'expiryStatusIds',
    'autoStatusIds',
    'organizationIds',
    'applicationIds',
    'policyIds',
  ];
  return fields.every((field) => {
    const leftIds = left[field] as ReadonlySet<string>;
    const rightIds = right[field] as ReadonlySet<string>;
    if (leftIds.size !== rightIds.size) return false;
    return Array.from(leftIds).every((id) => rightIds.has(id));
  });
}

/**
 * Envelope of the selected threat buckets — the {@code /rest/search/index-query} WAIVER filter
 * only accepts a single {@code policyThreatLevel: [min, max]} range. Multiple UI buckets
 * therefore collapse to their min/max envelope (widening the range compared to per-bucket
 * OR-terms), same trade-off the Applications CSV export makes when the wire only supports one range.
 */
function buildThreatLevelRange(
  threatLevelIds: ReadonlySet<WaiversThreatLevelId>,
): readonly [number, number] | undefined {
  if (threatLevelIds.size === 0) return undefined;
  let min = Number.POSITIVE_INFINITY;
  let max = Number.NEGATIVE_INFINITY;
  threatLevelIds.forEach((id) => {
    const range = THREAT_GROUP_RANGES[id];
    if (!range) return;
    min = Math.min(min, range[0]);
    max = Math.max(max, range[1]);
  });
  if (!Number.isFinite(min) || !Number.isFinite(max)) return undefined;
  return [min, max];
}

export interface WaiversIndexQueryFilterFields {
  readonly organizations?: ReadonlyArray<string>;
  readonly applications?: ReadonlyArray<string>;
  readonly policies?: ReadonlyArray<string>;
  readonly policyThreatLevel?: readonly [number, number];
  readonly expiryStatus?: ReadonlyArray<WaiversExpiryStatusId>;
  /** Classic: {@code false}=manual only; omitted otherwise (true is not sent). */
  readonly includeAutoWaivers?: boolean;
  /** Ana Auto-only chip → {@code isAuto:["true"]} (do not overload includeAutoWaivers). */
  readonly isAuto?: ReadonlyArray<'true' | 'false'>;
}

/**
 * Maps sidebar filter state into the {@code filters} bag understood by
 * {@code POST /rest/search/index-query} with {@code entityType: WAIVER}.
 *
 * Auto/Manual wire shape (matches Classic naming + Ana Auto-only):
 * - Manual only → {@code includeAutoWaivers:false}
 * - Auto only → {@code isAuto:["true"]}
 * - both / neither → omit both keys
 */
export function waiversListFiltersToRequest(
  filters: WaiversListFilterState,
): WaiversIndexQueryFilterFields {
  const out: {
    organizations?: ReadonlyArray<string>;
    applications?: ReadonlyArray<string>;
    policies?: ReadonlyArray<string>;
    policyThreatLevel?: readonly [number, number];
    expiryStatus?: ReadonlyArray<WaiversExpiryStatusId>;
    includeAutoWaivers?: boolean;
    isAuto?: ReadonlyArray<'true' | 'false'>;
  } = {};

  if (filters.organizationIds.size > 0) {
    out.organizations = Array.from(filters.organizationIds);
  }
  if (filters.applicationIds.size > 0) {
    out.applications = Array.from(filters.applicationIds);
  }
  if (filters.policyIds.size > 0) {
    out.policies = Array.from(filters.policyIds);
  }
  const range = buildThreatLevelRange(filters.threatLevelIds);
  if (range) {
    out.policyThreatLevel = range;
  }
  if (filters.expiryStatusIds.size > 0) {
    out.expiryStatus = Array.from(filters.expiryStatusIds);
  }
  const wantsAuto = filters.autoStatusIds.has('Auto');
  const wantsManual = filters.autoStatusIds.has('Manual');
  if (wantsManual && !wantsAuto) {
    out.includeAutoWaivers = false;
  } else if (wantsAuto && !wantsManual) {
    out.isAuto = ['true'];
  }
  return out;
}

/** Static threat facet rows for the filter rail (same shape as Applications). */
const STATIC_THREAT_LEVEL_FACETS = THREAT_GROUPS.filter(({ group }) => group !== 'None').map(({ group, range }) => ({
  id: group,
  label: `${range} ${group}`,
  count: 0,
}));

export function staticThreatLevelFacets(): ReadonlyArray<{
  readonly id: string;
  readonly label: string;
  readonly count: number;
}> {
  return STATIC_THREAT_LEVEL_FACETS;
}

/** Static expiry facet rows: the three canonical {@code policyWaiverExpiryStatus} keyword values. */
const STATIC_EXPIRY_STATUS_FACETS = [
  { id: 'Active', label: 'Active', count: 0 },
  { id: 'Expired', label: 'Expired', count: 0 },
  { id: 'Never', label: 'Never expires', count: 0 },
];

export function staticExpiryStatusFacets(): ReadonlyArray<{
  readonly id: string;
  readonly label: string;
  readonly count: number;
}> {
  return STATIC_EXPIRY_STATUS_FACETS;
}

/** Static Auto vs Manual facet rows. */
const STATIC_AUTO_STATUS_FACETS = [
  { id: 'Auto', label: 'Auto-generated', count: 0 },
  { id: 'Manual', label: 'Manual', count: 0 },
];

export function staticAutoStatusFacets(): ReadonlyArray<{
  readonly id: string;
  readonly label: string;
  readonly count: number;
}> {
  return STATIC_AUTO_STATUS_FACETS;
}
