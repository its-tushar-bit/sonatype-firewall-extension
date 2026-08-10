/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  EMPTY_WAIVERS_LIST_FILTERS,
  WaiversAutoStatusId,
  WaiversExpiryStatusId,
  WaiversLifecycleStatusId,
  WaiversListFilterState,
  WaiversPolicyTypeId,
  WaiversScopeId,
  WaiversStateId,
  WaiversThreatLevelId,
  isSelectableAutoStatusId,
  isSelectableExpiryStatusId,
  isSelectableLifecycleStatusId,
  isSelectablePolicyTypeId,
  isSelectableScopeId,
  isSelectableStateId,
  isSelectableThreatLevelId,
} from 'MainRoot/nosc/waivers/waiversListFilters';

/**
 * Wire sort tokens accepted by {@code POST /rest/search/index-query} for the WAIVER entity type.
 * The backend's {@code GlobalSearchSortAllowlist} accepts these keys against the WAIVER tab
 * (see {@code IndexQueryService.validateSort}); default is descending created-at.
 * {@code expiration} is the allowlisted soonest-expiry-first token (not a field-style alias).
 */
export type WaiversListOrderBy =
  | '-policyWaiverCreatedAt'
  | 'policyWaiverCreatedAt'
  | '-policyWaiverThreatLevel'
  | 'policyWaiverThreatLevel'
  | 'expiration';

export const DEFAULT_WAIVERS_LIST_ORDER_BY: WaiversListOrderBy = '-policyWaiverCreatedAt';

/** URL-friendly sort slugs persisted in the hash query — same shape as Applications. */
type WaiversListSortSlug = 'newest' | 'oldest' | 'severity' | 'severity-asc' | 'expiration';

const ORDER_BY_TO_SORT_SLUG: Record<WaiversListOrderBy, WaiversListSortSlug> = {
  '-policyWaiverCreatedAt': 'newest',
  policyWaiverCreatedAt: 'oldest',
  '-policyWaiverThreatLevel': 'severity',
  policyWaiverThreatLevel: 'severity-asc',
  expiration: 'expiration',
};

const SORT_SLUG_TO_ORDER_BY: Record<WaiversListSortSlug, WaiversListOrderBy> = {
  newest: '-policyWaiverCreatedAt',
  oldest: 'policyWaiverCreatedAt',
  severity: '-policyWaiverThreatLevel',
  'severity-asc': 'policyWaiverThreatLevel',
  expiration: 'expiration',
};

export function orderByToSortSlug(orderBy: WaiversListOrderBy): WaiversListSortSlug {
  return ORDER_BY_TO_SORT_SLUG[orderBy];
}

export function sortSlugToOrderBy(slug: string | null | undefined): WaiversListOrderBy {
  if (!slug) return DEFAULT_WAIVERS_LIST_ORDER_BY;
  const mapped = SORT_SLUG_TO_ORDER_BY[slug as WaiversListSortSlug];
  return mapped ?? DEFAULT_WAIVERS_LIST_ORDER_BY;
}

export function waiversListOrderByLabel(orderBy: WaiversListOrderBy): string {
  switch (orderBy) {
    case 'policyWaiverCreatedAt':
      return 'Oldest first';
    case '-policyWaiverThreatLevel':
      return 'Highest threat first';
    case 'policyWaiverThreatLevel':
      return 'Lowest threat first';
    case 'expiration':
      return 'Expiring soonest';
    case '-policyWaiverCreatedAt':
    default:
      return 'Newest first';
  }
}

function parseCsvParam(value: string | null | undefined): ReadonlyArray<string> {
  if (!value?.trim()) return [];
  return value.split(',').map((part) => part.trim()).filter(Boolean);
}

function parseTypedSet<T extends string>(
  values: ReadonlyArray<string>,
  guard: (value: string) => value is T,
): ReadonlySet<T> {
  const ids = values.filter(guard);
  return new Set(ids);
}

function serializeCsvParam(values: ReadonlySet<string>): string | undefined {
  if (values.size === 0) return undefined;
  return Array.from(values).sort().join(',');
}

export interface WaiversListQueryState {
  readonly search: string;
  readonly orderBy: WaiversListOrderBy;
  /** 1-based page index — matches the {@code /rest/search/index-query} contract. */
  readonly page: number;
  readonly filters: WaiversListFilterState;
}

/**
 * Parse a 1-based page index from UI-Router params. Query params are usually strings, but
 * UI-Router may coerce numeric values — treat both forms as valid so a local page advance
 * that round-trips through the hash is not misread as page 1 (which snaps pagination back).
 */
function parsePageParam(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value >= 1 ? Math.floor(value) : 1;
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number.parseInt(value, 10);
    if (Number.isFinite(parsed) && parsed >= 1) return parsed;
  }
  return 1;
}

/** Parse UI-Router params for the Ana Waivers list (CLM-43204 / CLM-43962). */
export function parseWaiversListParams(
  params: Record<string, unknown>,
): WaiversListQueryState {
  const search = typeof params.q === 'string' ? params.q.trim() : '';
  const orderBy = sortSlugToOrderBy(typeof params.sort === 'string' ? params.sort : null);
  const page = parsePageParam(params.page);

  return {
    search,
    orderBy,
    page,
    filters: {
      ...EMPTY_WAIVERS_LIST_FILTERS,
      threatLevelIds: parseTypedSet<WaiversThreatLevelId>(
        parseCsvParam(typeof params.threat === 'string' ? params.threat : null),
        isSelectableThreatLevelId,
      ),
      lifecycleStatusIds: parseTypedSet<WaiversLifecycleStatusId>(
        parseCsvParam(typeof params.lifecycle === 'string' ? params.lifecycle : null),
        isSelectableLifecycleStatusId,
      ),
      expiryStatusIds: parseTypedSet<WaiversExpiryStatusId>(
        parseCsvParam(typeof params.expiry === 'string' ? params.expiry : null),
        isSelectableExpiryStatusId,
      ),
      autoStatusIds: parseTypedSet<WaiversAutoStatusId>(
        parseCsvParam(typeof params.auto === 'string' ? params.auto : null),
        isSelectableAutoStatusId,
      ),
      waiverStateIds: parseTypedSet<WaiversStateId>(
        parseCsvParam(typeof params.state === 'string' ? params.state : null),
        isSelectableStateId,
      ),
      scopeIds: parseTypedSet<WaiversScopeId>(
        parseCsvParam(typeof params.scope === 'string' ? params.scope : null),
        isSelectableScopeId,
      ),
      policyTypeIds: parseTypedSet<WaiversPolicyTypeId>(
        parseCsvParam(typeof params.policyType === 'string' ? params.policyType : null),
        isSelectablePolicyTypeId,
      ),
      organizationIds: new Set(parseCsvParam(typeof params.org === 'string' ? params.org : null)),
      applicationIds: new Set(parseCsvParam(typeof params.app === 'string' ? params.app : null)),
      policyIds: new Set(parseCsvParam(typeof params.policy === 'string' ? params.policy : null)),
    },
  };
}

export function buildWaiversListRouteParams(state: WaiversListQueryState): Record<string, string | undefined> {
  const sort = orderByToSortSlug(state.orderBy);
  return {
    q: state.search.trim() || undefined,
    sort: sort === 'newest' ? undefined : sort,
    page: state.page > 1 ? String(state.page) : undefined,
    threat: serializeCsvParam(state.filters.threatLevelIds),
    lifecycle: serializeCsvParam(state.filters.lifecycleStatusIds),
    expiry: serializeCsvParam(state.filters.expiryStatusIds),
    auto: serializeCsvParam(state.filters.autoStatusIds),
    state: serializeCsvParam(state.filters.waiverStateIds),
    scope: serializeCsvParam(state.filters.scopeIds),
    policyType: serializeCsvParam(state.filters.policyTypeIds),
    org: serializeCsvParam(state.filters.organizationIds),
    app: serializeCsvParam(state.filters.applicationIds),
    policy: serializeCsvParam(state.filters.policyIds),
  };
}
