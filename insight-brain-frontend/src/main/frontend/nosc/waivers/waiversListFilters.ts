/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { THREAT_GROUPS } from 'MainRoot/nosc/applications/applicationDetailUtils';

/** Policy threat-level bucket ids — aligned with the Applications rail so filter labels match. */
export type WaiversThreatLevelId = (typeof THREAT_GROUPS)[number]['group'];

/** WAIVER lifecycle-status facet ids returned by Ana's fixed {@code status} facet. */
export type WaiversLifecycleStatusId = 'active' | 'expiring' | 'expired' | 'auto-waived';

/** {@code Auto|Manual} — collapsed to a single {@code includeAutoWaivers} boolean at the wire. */
export type WaiversAutoStatusId = 'Auto' | 'Manual';

/**
 * Ana {@code waiverStates} multi-select. Never includes {@code excluded} — that token today matches
 * auto-waivers, not a true excluded set, and must not be offered as a UI chip.
 */
export type WaiversStateId = 'existing' | 'requested' | 'rejected';

/** Ana {@code scope} TERMS over {@code policyWaiverScope}. */
export type WaiversScopeId = 'application' | 'organization' | 'component';

/** Ana {@code policyTypes} TERMS over denormalized {@code policyWaiverPolicyType} (lowercase names). */
export type WaiversPolicyTypeId = 'security' | 'license' | 'quality' | 'other';

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

const SELECTABLE_LIFECYCLE_STATUS_IDS = new Set<WaiversLifecycleStatusId>([
  'active',
  'expiring',
  'expired',
  'auto-waived',
]);
const SELECTABLE_AUTO_STATUS_IDS = new Set<WaiversAutoStatusId>(['Auto', 'Manual']);
const SELECTABLE_STATE_IDS = new Set<WaiversStateId>(['existing', 'requested', 'rejected']);
const SELECTABLE_SCOPE_IDS = new Set<WaiversScopeId>(['application', 'organization', 'component']);
const SELECTABLE_POLICY_TYPE_IDS = new Set<WaiversPolicyTypeId>([
  'security',
  'license',
  'quality',
  'other',
]);

export function isSelectableThreatLevelId(value: string): value is WaiversThreatLevelId {
  return SELECTABLE_THREAT_LEVEL_IDS.has(value as WaiversThreatLevelId);
}

export function isSelectableLifecycleStatusId(value: string): value is WaiversLifecycleStatusId {
  return SELECTABLE_LIFECYCLE_STATUS_IDS.has(value as WaiversLifecycleStatusId);
}

export function isSelectableAutoStatusId(value: string): value is WaiversAutoStatusId {
  return SELECTABLE_AUTO_STATUS_IDS.has(value as WaiversAutoStatusId);
}

export function isSelectableStateId(value: string): value is WaiversStateId {
  return SELECTABLE_STATE_IDS.has(value as WaiversStateId);
}

export function isSelectableScopeId(value: string): value is WaiversScopeId {
  return SELECTABLE_SCOPE_IDS.has(value as WaiversScopeId);
}

export function isSelectablePolicyTypeId(value: string): value is WaiversPolicyTypeId {
  return SELECTABLE_POLICY_TYPE_IDS.has(value as WaiversPolicyTypeId);
}

export type WaiversListFilterState = {
  readonly threatLevelIds: ReadonlySet<WaiversThreatLevelId>;
  readonly lifecycleStatusIds: ReadonlySet<WaiversLifecycleStatusId>;
  readonly autoStatusIds: ReadonlySet<WaiversAutoStatusId>;
  readonly waiverStateIds: ReadonlySet<WaiversStateId>;
  readonly scopeIds: ReadonlySet<WaiversScopeId>;
  readonly policyTypeIds: ReadonlySet<WaiversPolicyTypeId>;
  readonly organizationIds: ReadonlySet<string>;
  readonly applicationIds: ReadonlySet<string>;
  /** Selected policy display names (wire {@code policy} TERMS on policyWaiverPolicyName). */
  readonly policyIds: ReadonlySet<string>;
};

export type WaiversFilterSetGroup = keyof WaiversListFilterState;

/**
 * The "no user-selected filters" state — every set empty. Kept for test seeds and toggle-diff
 * comparisons where a known-empty baseline is useful. Not used for the initial page render; the
 * page opens with {@link INITIAL_WAIVERS_LIST_FILTERS} so the count matches the dashboard's
 * non-expired waiver total by default.
 */
export const EMPTY_WAIVERS_LIST_FILTERS: WaiversListFilterState = {
  threatLevelIds: new Set(),
  lifecycleStatusIds: new Set(),
  autoStatusIds: new Set(),
  waiverStateIds: new Set(),
  scopeIds: new Set(),
  policyTypeIds: new Set(),
  organizationIds: new Set(),
  applicationIds: new Set(),
  policyIds: new Set(),
};

/**
 * Initial filter state for the Waivers list page — pre-selects every lifecycle bucket EXCEPT
 * {@code expired} so the default view hides expired waivers, matching the dashboard's Waivers
 * tile total (which SQL-filters out expired waivers via {@code EXPIRY_TIME > now()}). Reset also
 * returns to this state; users who want to see expired waivers explicitly toggle the
 * {@code Expired} chip in the rail's "Status" section. Any user chip change surfaces via
 * {@link hasActiveWaiversListFilters}. Aligned with CLM-44905's decision to wire the "Status"
 * rail to {@code lifecycleStatus} rather than {@code expiryStatus}.
 */
export const INITIAL_WAIVERS_LIST_FILTERS: WaiversListFilterState = {
  ...EMPTY_WAIVERS_LIST_FILTERS,
  lifecycleStatusIds: new Set(['active', 'expiring', 'auto-waived']),
};

/**
 * True when the user has changed any chip away from the page's initial default
 * ({@link INITIAL_WAIVERS_LIST_FILTERS}). Drives the "Reset to default view" affordance in the
 * rail — so it does NOT fire on a fresh load whose {@code lifecycleStatusIds} is
 * {@code active + expiring + auto-waived} (that's the default, not a user selection), and it
 * does fire the moment the user toggles any chip in or out of any group.
 */
export function hasActiveWaiversListFilters(filters: WaiversListFilterState): boolean {
  return !filtersEqual(filters, INITIAL_WAIVERS_LIST_FILTERS);
}

/**
 * Toggle a single id in one of the sidebar sets. Silently no-ops when the id is not a
 * selectable value for the group (e.g. threat=None, lifecycle=Bogus) so URL/state stays
 * clean and callers do not need to guard.
 */
export function toggleWaiversListFilterId(
  filters: WaiversListFilterState,
  group: WaiversFilterSetGroup,
  id: string,
): WaiversListFilterState {
  if (group === 'threatLevelIds' && !isSelectableThreatLevelId(id)) return filters;
  if (group === 'lifecycleStatusIds' && !isSelectableLifecycleStatusId(id)) return filters;
  if (group === 'autoStatusIds' && !isSelectableAutoStatusId(id)) return filters;
  if (group === 'waiverStateIds' && !isSelectableStateId(id)) return filters;
  if (group === 'scopeIds' && !isSelectableScopeId(id)) return filters;
  if (group === 'policyTypeIds' && !isSelectablePolicyTypeId(id)) return filters;
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
    'lifecycleStatusIds',
    'autoStatusIds',
    'waiverStateIds',
    'scopeIds',
    'policyTypeIds',
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
  /** Policy display names → Ana {@code policy} TERMS on policyWaiverPolicyName. */
  readonly policy?: ReadonlyArray<string>;
  readonly policyThreatLevel?: readonly [number, number];
  readonly lifecycleStatus?: ReadonlyArray<WaiversLifecycleStatusId>;
  /** Classic: {@code false}=manual only; omitted otherwise (true is not sent). */
  readonly includeAutoWaivers?: boolean;
  /** Ana Auto-only chip → {@code isAuto:["true"]} (do not overload includeAutoWaivers). */
  readonly isAuto?: ReadonlyArray<'true' | 'false'>;
  readonly waiverStates?: ReadonlyArray<WaiversStateId>;
  readonly scope?: ReadonlyArray<WaiversScopeId>;
  readonly policyTypes?: ReadonlyArray<WaiversPolicyTypeId>;
}

/**
 * Maps sidebar filter state into the {@code filters} bag understood by
 * {@code POST /rest/search/index-query} with {@code entityType: WAIVER}.
 *
 * Auto/Manual wire shape (matches Classic naming + Ana Auto-only):
 * - Manual only → {@code includeAutoWaivers:false}
 * - Auto only → {@code isAuto:["true"]}
 * - both / neither → omit both keys
 *
 * Never emits {@code waiverStates:excluded}.
 */
export function waiversListFiltersToRequest(
  filters: WaiversListFilterState,
): WaiversIndexQueryFilterFields {
  const out: {
    organizations?: ReadonlyArray<string>;
    applications?: ReadonlyArray<string>;
    policy?: ReadonlyArray<string>;
    policyThreatLevel?: readonly [number, number];
    lifecycleStatus?: ReadonlyArray<WaiversLifecycleStatusId>;
    includeAutoWaivers?: boolean;
    isAuto?: ReadonlyArray<'true' | 'false'>;
    waiverStates?: ReadonlyArray<WaiversStateId>;
    scope?: ReadonlyArray<WaiversScopeId>;
    policyTypes?: ReadonlyArray<WaiversPolicyTypeId>;
  } = {};

  if (filters.organizationIds.size > 0) {
    out.organizations = Array.from(filters.organizationIds);
  }
  if (filters.applicationIds.size > 0) {
    out.applications = Array.from(filters.applicationIds);
  }
  if (filters.policyIds.size > 0) {
    out.policy = Array.from(filters.policyIds);
  }
  const range = buildThreatLevelRange(filters.threatLevelIds);
  if (range) {
    out.policyThreatLevel = range;
  }
  if (filters.lifecycleStatusIds.size > 0) {
    out.lifecycleStatus = Array.from(filters.lifecycleStatusIds);
  }
  if (filters.waiverStateIds.size > 0) {
    out.waiverStates = Array.from(filters.waiverStateIds);
  }
  if (filters.scopeIds.size > 0) {
    out.scope = Array.from(filters.scopeIds);
  }
  if (filters.policyTypeIds.size > 0) {
    out.policyTypes = Array.from(filters.policyTypeIds);
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

/** Static lifecycle status rows for the Ana {@code status} facet. */
const STATIC_LIFECYCLE_STATUS_FACETS = [
  { id: 'active', label: 'Active', count: 0 },
  { id: 'expiring', label: 'Expires Soon', count: 0 },
  { id: 'expired', label: 'Expired', count: 0 },
  { id: 'auto-waived', label: 'Auto-waived', count: 0 },
];

export function staticLifecycleStatusFacets(): ReadonlyArray<{
  readonly id: string;
  readonly label: string;
  readonly count: number;
}> {
  return STATIC_LIFECYCLE_STATUS_FACETS;
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

/** Static waiver-state rows — existing / requested / rejected only (never excluded). */
const STATIC_WAIVER_STATE_FACETS = [
  { id: 'existing', label: 'Existing', count: 0 },
  { id: 'requested', label: 'Requested', count: 0 },
  { id: 'rejected', label: 'Rejected', count: 0 },
];

export function staticWaiverStateFacets(): ReadonlyArray<{
  readonly id: string;
  readonly label: string;
  readonly count: number;
}> {
  return STATIC_WAIVER_STATE_FACETS;
}

const SCOPE_LABELS: Record<WaiversScopeId, string> = {
  application: 'Application',
  organization: 'Organization',
  component: 'Component',
};

const STATIC_SCOPE_FACETS = (['application', 'organization', 'component'] as const).map((id) => ({
  id,
  label: SCOPE_LABELS[id],
  count: 0,
}));

export function staticScopeFacets(): ReadonlyArray<{
  readonly id: string;
  readonly label: string;
  readonly count: number;
}> {
  return STATIC_SCOPE_FACETS;
}

const POLICY_TYPE_LABELS: Record<WaiversPolicyTypeId, string> = {
  security: 'Security',
  license: 'License',
  quality: 'Quality',
  other: 'Other',
};

const STATIC_POLICY_TYPE_FACETS = (['security', 'license', 'quality', 'other'] as const).map((id) => ({
  id,
  label: POLICY_TYPE_LABELS[id],
  count: 0,
}));

export function staticPolicyTypeFacets(): ReadonlyArray<{
  readonly id: string;
  readonly label: string;
  readonly count: number;
}> {
  return STATIC_POLICY_TYPE_FACETS;
}

export function scopeFacetLabel(id: string): string {
  return SCOPE_LABELS[id as WaiversScopeId] ?? id;
}

export function policyTypeFacetLabel(id: string): string {
  return POLICY_TYPE_LABELS[id as WaiversPolicyTypeId] ?? id;
}
