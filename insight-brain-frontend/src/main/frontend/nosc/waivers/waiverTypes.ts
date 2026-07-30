/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Phase 1 / CLM-39545 (P1-F7d): Native Nexus One Waivers list & detail pages.
 *
 * These types model the `PolicyWaiverDTO` returned by two endpoints we use:
 *
 *   POST /rest/dashboard/policy/policyWaivers?includeAutoWaivers=true
 *     Body: createDashboardDataRequestPayload({...filter, pageSize, page})
 *     Response: { dashboardResults: PolicyWaiverDTO[]; hasNextPage: boolean }
 *
 *   GET  /api/v2/policyWaivers/{ownerType}/{ownerId}/{waiverId}
 *     Response: PolicyWaiverDetailDTO (superset of the dashboard row).
 *
 * Field set was hard-ported from the Classic implementation:
 *   - dashboard/results/waivers/DashboardWaiversTableRow.jsx (list shape)
 *   - waivers/waiverDetails/WaiverDetails.jsx + util/waiverUtils.js
 *     (detail shape; see formatWaiverDetails for the raw fields).
 *
 * We deliberately stay nominal about optional vs required: only `id`,
 * `ownerId`, `ownerType`, `scope`, and `threatLevel` are guaranteed by
 * the backend. Everything else (policyName, expiryTime, createTime,
 * componentIdentifier, ...) can legitimately be missing on auto-waivers
 * or pre-migration rows.
 */

/**
 * Raw waiver row as returned in `dashboardResults` from
 * /rest/dashboard/policy/policyWaivers.
 */
export interface PolicyWaiverDTO {
  id: string;
  threatLevel: number;
  createTime?: string | number | null;
  expiryTime?: string | number | null;
  policyName?: string | null;
  policyId?: string | null;
  ownerId: string;
  ownerName?: string | null;
  ownerType: string;
  scope: string;
  componentMatchStrategy?: string | null;
  componentUpgradeAvailable?: boolean | null;
  isAutoWaiver?: boolean;
  isExpireWhenRemediationAvailable?: boolean;
  componentIdentifier?: ComponentIdentifier | null;
  displayName?: { parts?: ReadonlyArray<{ value?: string }> } | string | null;
  matcherStrategy?: string | null;
}

export interface ComponentIdentifier {
  format?: string;
  coordinates?: Record<string, string>;
}

export interface PolicyWaiverConditionFact {
  reason?: string | null;
}

export interface PolicyWaiverConstraintFact {
  constraintName?: string | null;
  conditionFacts?: ReadonlyArray<PolicyWaiverConditionFact> | null;
}

/**
 * Detailed waiver, returned by /api/v2/policyWaivers/{type}/{ownerId}/{id}.
 * Adds reason text, comments, creator name, scope hierarchy details, and
 * the constraint-conditions structure used to render "what was waived".
 */
export interface PolicyWaiverDetailDTO extends PolicyWaiverDTO {
  comment?: string | null;
  creatorName?: string | null;
  reasonText?: string | null;
  vulnerabilityId?: string | null;
  associatedPackageUrl?: string | null;
  scopeOwnerType?: string | null;
  scopeOwnerName?: string | null;
  /**
   * The v2 detail payload spells this without the `is` prefix the dashboard list
   * row uses; both are read when presenting expiry.
   */
  expireWhenRemediationAvailable?: boolean;
  forContainerImage?: boolean;
  constraintFacts?: ReadonlyArray<PolicyWaiverConstraintFact> | null;
}

export type WaiversListResponse = {
  dashboardResults: ReadonlyArray<PolicyWaiverDTO>;
  hasNextPage: boolean;
};

/**
 * Raw response shape of `GET /api/v2/autoPolicyWaivers/{ownerType}/{ownerId}/{autoPolicyWaiverId}`
 * (`ApiAutoPolicyWaiverDTO` on the backend). Auto-waivers live in a separate
 * table from manual waivers, so this has a materially different field set —
 * no policy/constraint/vulnerability data, since an auto-waiver applies broadly
 * rather than to one specific violation.
 */
export interface ApiAutoPolicyWaiverDTO {
  autoPolicyWaiverId: string;
  ownerId: string;
  ownerType: string;
  ownerName?: string | null;
  publicId?: string | null;
  // Backend field is `@JsonInclude(Include.NON_EMPTY)` on a primitive int, so
  // Jackson omits it entirely when the value is 0 — must be optional here too.
  threatLevel?: number;
  reachability?: boolean | null;
  pathForward?: boolean | null;
  creatorId?: string | null;
  creatorName?: string | null;
  createTime?: string | null;
  scopesOperatorAny?: boolean;
}
