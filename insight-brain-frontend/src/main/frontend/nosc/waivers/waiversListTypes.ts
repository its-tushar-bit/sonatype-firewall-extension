/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Ana-indexed waiver row (CLM-43204) — flattened projection of an IQ-index
 * WAIVER doc returned by {@code POST /rest/search/index-query}. Distinct from
 * the legacy {@link ./waiverTypes#PolicyWaiverDTO} which comes from
 * {@code /rest/dashboard/policy/policyWaivers}: this row set does NOT carry
 * {@code componentIdentifier} / {@code displayName} — the index projects only
 * the fields the list needs. Row-click navigates to the shared native detail
 * page which continues to hit the {@code /api/v2/policyWaivers/...} endpoint
 * for the full waiver payload.
 */
export interface AnaWaiverRow {
  readonly id: string;
  readonly policyId: string | null;
  readonly policyName: string | null;
  readonly threatLevel: number;
  readonly reason: string | null;
  readonly comment: string | null;
  /** ISO-8601 waiver creation timestamp when the index provides it. */
  readonly createdAt: string | null;
  /** ISO-8601 waiver expiry timestamp; null means never or auto (see {@link #isAuto}). */
  readonly expiresAt: string | null;
  /** Owner (application / organization / repository-container) the waiver scopes to. */
  readonly scopeOwnerType: string | null;
  readonly scopeOwnerId: string | null;
  readonly waivedBy: string | null;
  readonly organizationName: string | null;
  readonly organizationId: string | null;
  readonly applicationName: string | null;
  readonly applicationId: string | null;
  /** True when the waiver was created automatically by IQ (no policyId on the source row). */
  readonly isAuto: boolean;
}

export type WaiversFilterFacetEntry = {
  readonly id: string;
  readonly label: string;
  readonly count: number;
};

/**
 * Filter-rail facet counts for the top-level waivers page. Threat levels and expiry statuses
 * are static UI-side buckets; organizations / applications / policies come from the API
 * response's whole-corpus, RBAC-scoped counts (values seeded by the current page rows).
 */
export type WaiversFilterFacetCounts = {
  readonly totalWaivers: number;
  readonly threatLevels: ReadonlyArray<WaiversFilterFacetEntry>;
  readonly expiryStatuses: ReadonlyArray<WaiversFilterFacetEntry>;
  readonly autoStatuses: ReadonlyArray<WaiversFilterFacetEntry>;
  readonly organizations: ReadonlyArray<WaiversFilterFacetEntry>;
  readonly applications: ReadonlyArray<WaiversFilterFacetEntry>;
  readonly policies: ReadonlyArray<WaiversFilterFacetEntry>;
};
