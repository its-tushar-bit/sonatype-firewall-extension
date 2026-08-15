/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  AnaWaiverRow,
  WaiversFilterFacetCounts,
  WaiversFilterFacetEntry,
} from 'MainRoot/nosc/waivers/waiversListTypes';
import {
  WaiversListFilterState,
  isSelectablePolicyTypeId,
  isSelectableScopeId,
  isSelectableThreatLevelId,
  policyTypeFacetLabel,
  scopeFacetLabel,
  staticAutoStatusFacets,
  staticLifecycleStatusFacets,
  staticPolicyTypeFacets,
  staticScopeFacets,
  staticThreatLevelFacets,
  staticWaiverStateFacets,
  waiversListFiltersToRequest,
} from 'MainRoot/nosc/waivers/waiversListFilters';
import type { WaiversListOrderBy } from 'MainRoot/nosc/waivers/waiversListQuery';
import { classifyThreat } from 'MainRoot/nosc/applications/applicationDetailUtils';

export const WAIVERS_LIST_PAGE_SIZE = 50;

/** Mirrors backend {@code IndexQueryRow} — a WAIVER row's identifying fields + open bag. */
export type ApiWaiverRow = {
  readonly entityType?: string;
  readonly source?: string;
  readonly id?: string;
  readonly title?: string | null;
  readonly subtitle?: string | null;
  readonly fields?: Readonly<Record<string, unknown>> | null;
  readonly href?: string | null;
};

/**
 * Mirrors backend {@code IndexQueryFacetBucket}. The id-carrying entity facets
 * (organizations/applications/policy) emit {@code value} = the entity's opaque id and
 * {@code displayName} = the resolved human-readable name; fixed-vocabulary / enum facets
 * (status, auto, threatLevel, scope, policyType) carry no {@code displayName} and `value` IS the
 * label.
 */
export type ApiIndexQueryFacetBucket = {
  readonly value?: string;
  readonly displayName?: string;
  readonly count?: number;
};

/** Mirrors backend {@code IndexQueryResponse}. */
export type WaiversIndexQueryResponse = {
  readonly entityType?: string;
  readonly page?: number;
  readonly pageSize?: number;
  readonly totalEstimate?: number;
  readonly exactTotalEstimate?: boolean;
  readonly rows?: ReadonlyArray<ApiWaiverRow> | null;
  readonly facets?: Readonly<Record<string, ReadonlyArray<ApiIndexQueryFacetBucket>>> | null;
  readonly facetsOverPageOnly?: boolean;
  readonly nextSearchAfter?: string | null;
  readonly warnings?: ReadonlyArray<string> | null;
};

export interface WaiversIndexQueryRequest {
  readonly entityType: 'WAIVER';
  readonly filters?: Readonly<Record<string, unknown>>;
  /** 1-based page index — page 1 must omit {@link #searchAfter}. */
  readonly page?: number;
  readonly pageSize?: number;
  readonly sort?: WaiversListOrderBy;
  readonly searchAfter?: string;
  readonly includeFacets?: boolean;
}

/**
 * Build the {@code POST /rest/search/index-query} body for a top-level waivers list request.
 * Page > 1 requires a {@code searchAfter} cursor returned by the prior response; when a caller
 * asks for a page > 1 without a stored cursor (e.g. deep link), we clamp back to page 1 so the
 * request cannot 400 on the backend's cursor consistency check.
 */
export function buildWaiversIndexQueryRequest(params: {
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly sort?: WaiversListOrderBy;
  readonly includeFacets?: boolean;
  readonly filters?: WaiversListFilterState;
  readonly searchAfter?: string;
}): WaiversIndexQueryRequest {
  const structured = params.filters ? waiversListFiltersToRequest(params.filters) : undefined;
  const trimmedSearch = params.search?.trim();
  const filterBag: Record<string, unknown> = {
    ...(trimmedSearch ? { query: trimmedSearch } : {}),
    ...(structured ?? {}),
  };

  // Cursor-based paging: page > 1 requires a searchAfter. Deep links without a cached cursor
  // clamp to page 1 (see the useAnaWaiversList effect that syncs setPage back to the client).
  const page = params.page >= 1 ? params.page : 1;
  const canSendCursor = page > 1 && !!params.searchAfter?.trim();

  return {
    entityType: 'WAIVER',
    page,
    pageSize: params.pageSize ?? WAIVERS_LIST_PAGE_SIZE,
    includeFacets: params.includeFacets ?? true,
    ...(params.sort ? { sort: params.sort } : {}),
    ...(Object.keys(filterBag).length > 0 ? { filters: filterBag } : {}),
    ...(canSendCursor ? { searchAfter: params.searchAfter!.trim() } : {}),
  };
}

function asString(fields: Readonly<Record<string, unknown>> | null | undefined, key: string): string | null {
  const value = fields?.[key];
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

function asNumber(fields: Readonly<Record<string, unknown>> | null | undefined, key: string): number {
  const value = fields?.[key];
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string') {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return 0;
}

function asBoolean(fields: Readonly<Record<string, unknown>> | null | undefined, key: string): boolean {
  const value = fields?.[key];
  if (typeof value === 'boolean') return value;
  if (typeof value === 'string') return value.toLowerCase() === 'true';
  return false;
}

/**
 * Map an Ana WAIVER {@link ApiWaiverRow} into the flattened row the list page renders.
 * Returns {@code null} when the row is missing its identifying {@code id} — the caller
 * drops it and the backend already emits a warning for the same case.
 */
export function mapApiWaiverRow(row: ApiWaiverRow): AnaWaiverRow | null {
  const id = row.id?.trim();
  if (!id) return null;
  const fields = row.fields ?? undefined;
  return {
    id,
    policyId: asString(fields, 'policyId'),
    policyName: asString(fields, 'policyName') ?? row.title?.trim() ?? null,
    threatLevel: asNumber(fields, 'threatLevel'),
    reason: asString(fields, 'reason'),
    comment: asString(fields, 'comment'),
    createdAt: asString(fields, 'createdAt'),
    expiresAt: asString(fields, 'expiresAt'),
    scopeOwnerType: asString(fields, 'scopeOwnerType'),
    scopeOwnerId: asString(fields, 'scopeOwnerId'),
    waivedBy: asString(fields, 'waivedBy'),
    organizationName: asString(fields, 'organizationName'),
    organizationId: asString(fields, 'organizationId'),
    applicationName: asString(fields, 'applicationName'),
    applicationId: asString(fields, 'applicationId'),
    isAuto: asBoolean(fields, 'isAuto'),
    isRequested: asBoolean(fields, 'isRequested'),
    status: asString(fields, 'status'),
  };
}

function facetEntriesFromBuckets(
  buckets: ReadonlyArray<ApiIndexQueryFacetBucket> | null | undefined,
): ReadonlyArray<WaiversFilterFacetEntry> {
  if (!buckets?.length) return [];
  return buckets
    .filter((bucket) => typeof bucket.value === 'string' && bucket.value.trim().length > 0)
    .map((bucket) => {
      const id = bucket.value!.trim();
      // The bucket value is the entity id; displayName carries the resolved name for rendering/sorting.
      // Falls back to the id when displayName is absent, which happens for an id the backend could not
      // resolve to a name -- a deleted entity, or one outside the caller's scope.
      const displayName = bucket.displayName?.trim();
      return {
        id,
        label: displayName && displayName.length > 0 ? displayName : id,
        count: typeof bucket.count === 'number' ? bucket.count : 0,
      };
    })
    .sort((left, right) => left.label.localeCompare(right.label));
}

/**
 * Overlay whole-corpus counts from the API onto a static facet vocabulary so the rail always
 * shows every selectable chip (even when the current page seeds no bucket for that value).
 */
function mergeStaticFacetsWithCounts(
  staticEntries: ReadonlyArray<WaiversFilterFacetEntry>,
  buckets: ReadonlyArray<ApiIndexQueryFacetBucket> | null | undefined,
  labelFor?: (id: string) => string,
): ReadonlyArray<WaiversFilterFacetEntry> {
  const countById = new Map<string, number>();
  (buckets ?? []).forEach((bucket) => {
    if (typeof bucket.value !== 'string' || !bucket.value.trim()) return;
    countById.set(bucket.value.trim().toLowerCase(), typeof bucket.count === 'number' ? bucket.count : 0);
  });
  return staticEntries.map((entry) => ({
    id: entry.id,
    label: labelFor ? labelFor(entry.id) : entry.label,
    count: countById.get(entry.id.toLowerCase()) ?? entry.count,
  }));
}

function mergeLifecycleFacets(
  buckets: ReadonlyArray<ApiIndexQueryFacetBucket> | null | undefined,
): ReadonlyArray<WaiversFilterFacetEntry> {
  return mergeStaticFacetsWithCounts(staticLifecycleStatusFacets(), buckets);
}

function mergeAutoFacets(
  buckets: ReadonlyArray<ApiIndexQueryFacetBucket> | null | undefined,
): ReadonlyArray<WaiversFilterFacetEntry> {
  const normalized = (buckets ?? []).flatMap((bucket): ApiIndexQueryFacetBucket[] => {
    const value = bucket.value?.trim().toLowerCase();
    if (value === 'true') return [{ ...bucket, value: 'Auto' }];
    if (value === 'false') return [{ ...bucket, value: 'Manual' }];
    return [];
  });
  return mergeStaticFacetsWithCounts(staticAutoStatusFacets(), normalized);
}

function mergeThreatFacets(
  buckets: ReadonlyArray<ApiIndexQueryFacetBucket> | null | undefined,
): ReadonlyArray<WaiversFilterFacetEntry> {
  const countById = new Map<string, number>();
  (buckets ?? []).forEach((bucket) => {
    const parsed = Number(bucket.value);
    if (!Number.isFinite(parsed)) return;
    const id = classifyThreat(parsed).label;
    if (!isSelectableThreatLevelId(id)) return;
    countById.set(id, (countById.get(id) ?? 0) + (typeof bucket.count === 'number' ? bucket.count : 0));
  });
  return staticThreatLevelFacets().map((entry) => ({
    ...entry,
    count: countById.get(entry.id) ?? entry.count,
  }));
}

/**
 * Build the filter-rail facet counts from the API response. Static sections always render;
 * organizations / applications / policies use page-seeded whole-corpus buckets from Ana.
 */
export function mapWaiversFacets(
  response: WaiversIndexQueryResponse,
  _rows: ReadonlyArray<AnaWaiverRow>,
  totalWaivers: number,
): WaiversFilterFacetCounts {
  const facets = response.facets ?? {};
  const scopeBuckets = (facets.scope ?? []).filter(
    (bucket) => typeof bucket.value === 'string' && isSelectableScopeId(bucket.value.trim().toLowerCase()),
  );
  const policyTypeBuckets = (facets.policyType ?? []).filter(
    (bucket) => typeof bucket.value === 'string' && isSelectablePolicyTypeId(bucket.value.trim().toLowerCase()),
  );
  return {
    totalWaivers,
    threatLevels: mergeThreatFacets(facets.threatLevel),
    lifecycleStatuses: mergeLifecycleFacets(facets.status),
    autoStatuses: mergeAutoFacets(facets.auto),
    waiverStates: staticWaiverStateFacets(),
    scopes: mergeStaticFacetsWithCounts(staticScopeFacets(), scopeBuckets, scopeFacetLabel),
    policyTypes: mergeStaticFacetsWithCounts(
      staticPolicyTypeFacets(),
      policyTypeBuckets,
      policyTypeFacetLabel,
    ),
    // The WAIVER organizations/applications/policy facets are all keyed by their id-carrying facet
    // key (see IndexQueryService#FACET_FIELDS[WAIVER] and IndexQueryServiceResultsFacetsTest's
    // waiversTab_returnsWaiverFacetSet, which asserts this exact key set) — organizationName is not a
    // key the backend ever emits here.
    organizations: facetEntriesFromBuckets(facets.organizations),
    applications: facetEntriesFromBuckets(facets.applications),
    policies: facetEntriesFromBuckets(facets.policy),
  };
}

export function mapWaiversIndexQueryResponse(response: WaiversIndexQueryResponse): {
  readonly waivers: ReadonlyArray<AnaWaiverRow>;
  readonly facets: WaiversFilterFacetCounts;
  readonly total: number;
  /** False when the backend capped/truncated the total estimate — render as N+. */
  readonly exactTotalEstimate: boolean;
  /** 1-based page index returned by the backend. */
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
  readonly nextSearchAfter: string | null;
  readonly warnings: ReadonlyArray<string>;
} {
  const waivers = (response.rows ?? [])
    .map(mapApiWaiverRow)
    .filter((row): row is AnaWaiverRow => row != null);
  const pageSize = response.pageSize ?? WAIVERS_LIST_PAGE_SIZE;
  const page = typeof response.page === 'number' && response.page >= 1 ? response.page : 1;
  const total = typeof response.totalEstimate === 'number' ? response.totalEstimate : waivers.length;
  const exactTotalEstimate = response.exactTotalEstimate !== false;
  const nextSearchAfter = response.nextSearchAfter?.trim() || null;
  // WAIVER pagination is cursor-only past page 1: hasNextPage tracks whether the backend
  // returned a cursor, not a page-arithmetic estimate. This matches the componentsList
  // local-source contract and avoids showing a Next button that would 400 on send.
  const hasNextPage = Boolean(nextSearchAfter);

  return {
    waivers,
    facets: mapWaiversFacets(response, waivers, total),
    total,
    exactTotalEstimate,
    page,
    pageSize,
    hasNextPage,
    nextSearchAfter,
    warnings: response.warnings ?? [],
  };
}
