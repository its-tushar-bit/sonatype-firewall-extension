/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Frontend mirror of the Nexus One Violations list API contract.
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

/** A single violation card row — mirrors backend {@code ViolationRowDTO}. */
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
  /** Component version when present on the index row ({@code ViolationRowDTO.componentVersion}). */
  readonly componentVersion?: string;
  /**
   * Component hash — used by Legal V1 card drill (ALP Classic Legal / non-ALP Component Details).
   * Not returned on POLICY_VIOLATION list rows today.
   */
  readonly componentHash?: string;
  /** Report/scan id for Component Details drill context (Legal V1 stash). */
  readonly reportId?: string;
  readonly componentIdentifier?: ViolationComponentIdentifier;
  /** Latest policy evaluation stage (display name). */
  readonly stage?: string;
  /** Violation state (OPEN / WAIVED). */
  readonly state?: string;
  /** True when waived by an auto-waiver rather than a manual waiver. */
  readonly waivedWithAutoWaiver?: boolean;
  readonly constraintName?: string;
  /**
   * Epoch millis the violation was first seen ({@code PolicyViolation.openTime} via page SQL enrich).
   * When absent the card omits the "first seen" line.
   */
  readonly firstOccurredTime?: number;
};

/**
 * Sidebar facet counts — mirrors backend {@code ViolationsListFacetsDTO}. Maps are keyed by a
 * stable id (OPEN/WAIVED, security/license/…, stage id, owner internal id) and omitted when empty.
 */
export type ViolationsListFacets = {
  readonly totalViolations: number;
  readonly states?: Readonly<Record<string, number>>;
  /** Auto- vs manually-waived counts, keyed AUTO / MANUAL (backend ViolationsListFacetsBuilder). */
  readonly waiverTypes?: Readonly<Record<string, number>>;
  readonly threatCategories?: Readonly<Record<string, number>>;
  readonly stages?: Readonly<Record<string, number>>;
  readonly organizations?: Readonly<Record<string, number>>;
  readonly applications?: Readonly<Record<string, number>>;
  /**
   * Friendly org display names keyed by the same internal ids as {@link organizations}.
   * Emitted by {@code ViolationsListFacetsDTO} (CLM-42757); absent maps fall back to page-row names.
   * Also populated for server-side facet-search matches (CLM-42912).
   */
  readonly organizationNames?: Readonly<Record<string, string>>;
  /**
   * Friendly app display names keyed by the same internal ids as {@link applications}.
   * Emitted by {@code ViolationsListFacetsDTO} (CLM-42757); absent maps fall back to page-row names.
   * Also populated for server-side facet-search matches (CLM-42912).
   */
  readonly applicationNames?: Readonly<Record<string, string>>;
};

/**
 * Waiver-type selection for the sidebar radio. {@code ANY} applies no waiver-type narrowing;
 * {@code AUTO} / {@code MANUAL} map to the backend {@code waivedWithAutoWaiver} boolean (true / false).
 */
export type ViolationWaiverType = 'ANY' | 'AUTO' | 'MANUAL';

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
 * {@code page} is 0-based. Wire formats match the backend filter DTOs (and the Classic dashboard
 * precedent): {@code policyViolationStates} is an array of enum names
 * ({@code PolicyViolationStateFilter}'s {@code @JsonCreator Set} constructor), while
 * {@code policyThreatCategories} and {@code policyThreatLevelRange} are comma-delimited strings
 * (their {@code String} constructors — {@code "security,license"}, {@code "0,10"}).
 *
 * The waiver-type filter maps to {@code waivedWithAutoWaiver} (true = auto-waived only, false =
 * manually-waived only, omitted = no narrowing). Filters the backend validator still rejects
 * (ageInDays, applicationCategoryIds, and the LEGACY_VIOLATION state) are intentionally omitted until
 * the API supports them.
 */
export type ViolationsListRequest = {
  readonly search?: string;
  readonly page: number;
  readonly pageSize: number;
  readonly includeFacets?: boolean;
  readonly orderBy?: string;
  readonly policyViolationStates?: ReadonlyArray<string>;
  readonly policyThreatCategories?: string;
  readonly policyThreatLevelRange?: string;
  readonly stageIds?: ReadonlyArray<string>;
  readonly organizationIds?: ReadonlyArray<string>;
  readonly applicationIds?: ReadonlyArray<string>;
  readonly waivedWithAutoWaiver?: boolean;
  /**
   * Optional org-name substring for the Organizations facet map. When set, the server replaces the
   * uncapped top-N org facets with name-matched owners that still have scoped violation counts.
   */
  readonly organizationFacetSearch?: string;
  /** Optional application-name substring for the Applications facet map (same replace semantics). */
  readonly applicationFacetSearch?: string;
};

/** Inclusive policy-threat-level domain for the range slider (matches the backend 0–10 clamp). */
export const VIOLATION_THREAT_MIN = 0;
export const VIOLATION_THREAT_MAX = 10;

/** [min, max] policy-threat-level selection; defaults to the full [0, 10] domain (no narrowing). */
export type ViolationThreatRange = readonly [number, number];

/**
 * Selected filter state owned by the Violations list container and rendered by the filter rail.
 * Multi-selects are id sets keyed to match the API facet maps (states OPEN/WAIVED, categories
 * security/license/…, stage ids, owner internal ids). {@code threatRange} maps to
 * {@code policyThreatLevelRange}.
 */
export type ViolationsFilterState = {
  readonly states: ReadonlySet<string>;
  readonly threatCategories: ReadonlySet<string>;
  readonly stageIds: ReadonlySet<string>;
  readonly organizationIds: ReadonlySet<string>;
  readonly applicationIds: ReadonlySet<string>;
  readonly threatRange: ViolationThreatRange;
  /** Single-select waiver-type narrowing; {@code ANY} is the unfiltered default. */
  readonly waiverType: ViolationWaiverType;
};

/** The set-valued filter groups (everything in {@link ViolationsFilterState} except {@code threatRange}). */
export type ViolationFilterSetGroup =
  | 'states'
  | 'threatCategories'
  | 'stageIds'
  | 'organizationIds'
  | 'applicationIds';
