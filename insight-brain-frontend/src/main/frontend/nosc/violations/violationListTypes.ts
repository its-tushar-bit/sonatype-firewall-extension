/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Frontend mirror of the Nexus One Violations list API contract (CLM-42254).
 * Source of truth: {@code com.sonatype.insight.brain.dashboard.violations}
 * (ViolationsListRequestDTO / ViolationsListResponseDTO / ViolationRowDTO /
 * ViolationsListFacetsDTO). Fields the backend marks {@code @JsonInclude(NON_NULL)}
 * are optional here.
 */

/** Loosely-typed component coordinates from {@code ApiComponentIdentifierDTOV2}. */
export type ViolationComponentIdentifier = {
  readonly format?: string;
  readonly coordinates?: Record<string, string | undefined>;
};

/** A single violation card row — mirrors backend {@code ViolationRowDTO} (no SQL enrichment). */
export type ViolationRow = {
  /** Native policy violation id — the drill-in / detail link id. */
  readonly policyViolationId: string;
  /** Policy threat level 0–10. */
  readonly threatLevel?: number;
  /** Derived severity band (low / moderate / severe / critical). */
  readonly severity?: string;
  /** Policy threat category (security / license / quality / other). */
  readonly threatCategory?: string;
  readonly policyId?: string;
  readonly policyName?: string;
  readonly organizationId?: string;
  readonly organizationName?: string;
  readonly applicationId?: string;
  readonly applicationPublicId?: string;
  readonly applicationName?: string;
  readonly componentName?: string;
  readonly componentIdentifier?: ViolationComponentIdentifier;
  /** Latest policy evaluation stage (display name). */
  readonly stage?: string;
  /** Violation state (OPEN / WAIVED). */
  readonly state?: string;
  /** True when waived by an auto-waiver rather than a manual waiver. */
  readonly waivedWithAutoWaiver?: boolean;
  readonly constraintName?: string;
};

/**
 * Sidebar facet counts — mirrors backend {@code ViolationsListFacetsDTO}. Maps are keyed by a
 * stable id (OPEN/WAIVED, security/license/…, stage id, owner internal id) and omitted when empty.
 */
export type ViolationsListFacets = {
  readonly totalViolations: number;
  readonly states?: Readonly<Record<string, number>>;
  readonly threatCategories?: Readonly<Record<string, number>>;
  readonly stages?: Readonly<Record<string, number>>;
  readonly organizations?: Readonly<Record<string, number>>;
  readonly applications?: Readonly<Record<string, number>>;
};

/** Paginated response — mirrors backend {@code ViolationsListResponseDTO}. */
export type ViolationsListResponse = {
  readonly violations: ReadonlyArray<ViolationRow>;
  readonly facets?: ViolationsListFacets;
  readonly total: number;
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
  readonly source: string;
};

/**
 * Request body — mirrors the validator-safe subset of backend {@code ViolationsListRequestDTO}.
 * {@code page} is 0-based. Filters that the backend validator currently rejects (ageInDays,
 * applicationCategoryIds, waivedWithAutoWaiver) are intentionally omitted until CLM-42258 / CLM-42261.
 */
export type ViolationsListRequest = {
  readonly search?: string;
  readonly page: number;
  readonly pageSize: number;
  readonly includeFacets?: boolean;
  readonly orderBy?: string;
};
