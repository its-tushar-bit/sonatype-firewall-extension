/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Phase-1.5 / CLM-39604 (S2-PR-D-2): typed payload for the
 * `GET /rest/dashboard/legalObligations` endpoint.
 *
 * Server-side ALP entitlement branching means a single endpoint returns one
 * of four discriminated shapes. The frontend tile reads `variant` (or the
 * permissionDenied/empty discriminators) and picks the renderer.
 *
 * Keep this file in sync with the backend DTO produced by S2-PR-D-2's
 * backend half. If the backend agent ships a different shape, reconcile
 * here and update the tile renderer accordingly — the rest of the
 * codebase has no other consumers.
 */

/** ALP-licensed grouped row: one license-threat-group with rolling-30d trend. */
export interface LegalObligationsAlpGroup {
  /** Stable group identifier (used in deep-link query string). */
  id: string;
  /** Human-readable group name (e.g., "Banned", "Copyleft"). */
  name: string;
  /** Unreviewed component count for this threat group (serialized as {@code reviewCount}). */
  reviewCount: number;
  /**
   * Trend percentage vs the previous 30 days for license-category violations.
   * Positive = increasing (bad), negative = decreasing (good), 0 = flat.
   * Backend rounds to a whole-number percent; treat as already-formatted.
   */
  trendPct: number;
}

/** Non-ALP top-legal-policy-violation row. */
export interface LegalObligationsTopViolation {
  /** Stable policy identifier (used in deep-link query string). */
  policyId: string;
  /** Human-readable policy name. */
  policyName: string;
  /** Open violation count attributed to this policy. */
  openViolationCount: number;
}

/**
 * Discriminated union mirroring the backend response shape. Use the
 * `is*` type guards below to narrow.
 */
export type LegalObligationsResponse =
  | { variant: 'ALP'; groups: LegalObligationsAlpGroup[] }
  | { variant: 'TOP_LEGAL_VIOLATIONS'; violations: LegalObligationsTopViolation[] }
  | { permissionDenied: true }
  | { empty: true };

export function isAlpVariant(
  response: LegalObligationsResponse,
): response is { variant: 'ALP'; groups: LegalObligationsAlpGroup[] } {
  return (response as { variant?: string }).variant === 'ALP';
}

export function isTopLegalViolationsVariant(
  response: LegalObligationsResponse,
): response is { variant: 'TOP_LEGAL_VIOLATIONS'; violations: LegalObligationsTopViolation[] } {
  return (response as { variant?: string }).variant === 'TOP_LEGAL_VIOLATIONS';
}

export function isPermissionDenied(
  response: LegalObligationsResponse,
): response is { permissionDenied: true } {
  return (response as { permissionDenied?: boolean }).permissionDenied === true;
}

export function isEmpty(response: LegalObligationsResponse): response is { empty: true } {
  return (response as { empty?: boolean }).empty === true;
}
