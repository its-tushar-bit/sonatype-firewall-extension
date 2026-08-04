/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Disable reasons for Create / Request waiver CTAs on violation detail.
 * Prefer greyed buttons + hover text over hiding actions the user cannot take.
 */
export type WaiverActionDisableReason =
  | 'checking_permission'
  | 'no_permission'
  | 'already_waived'
  | 'pending_request'
  | 'enterprise_feature'
  | 'not_eligible';

export const WAIVER_ACTION_DISABLE_MESSAGES: Record<WaiverActionDisableReason, string> = {
  checking_permission: 'Checking permissions…',
  no_permission: "You don't have permission to create waivers",
  already_waived: 'This violation is already waived',
  pending_request: 'A waiver request already exists for this violation',
  enterprise_feature: 'Enterprise feature. Contact your administrator to enable waiver requests.',
  not_eligible: 'Not eligible for a waiver',
};

export function messageForWaiverActionDisableReason(
  reason: WaiverActionDisableReason | null | undefined,
): string | undefined {
  return reason ? WAIVER_ACTION_DISABLE_MESSAGES[reason] : undefined;
}

/**
 * Create Waiver: requires WAIVE_POLICY_VIOLATIONS; blocked when already waived.
 * Returns null when the action may proceed.
 */
export function resolveCreateWaiverDisableReason(input: {
  readonly hasWaivePermission: boolean | null;
  readonly isWaived: boolean;
}): WaiverActionDisableReason | null {
  if (input.isWaived) return 'already_waived';
  if (input.hasWaivePermission === null) return 'checking_permission';
  if (input.hasWaivePermission === false) return 'no_permission';
  return null;
}

/**
 * Request Waiver: READ + workflow; blocked when already waived, pending request, or license-gated.
 * Call only when the request-workflow feature flag is on (otherwise hide the CTA).
 */
export function resolveRequestWaiverDisableReason(input: {
  readonly isWaived: boolean;
  readonly hasPendingRequest: boolean;
  readonly isEnterpriseGated: boolean;
}): WaiverActionDisableReason | null {
  if (input.isEnterpriseGated) return 'enterprise_feature';
  if (input.isWaived) return 'already_waived';
  if (input.hasPendingRequest) return 'pending_request';
  return null;
}

/**
 * Browser-session hint that a REQUESTED waiver exists for this violation.
 * Avoids an unbounded app-wide request-list fetch on detail mount; not a
 * cross-device source of truth (server still rejects duplicates).
 */
export function pendingWaiverRequestSessionKey(policyViolationId: string): string {
  return `nosc.pendingWaiverRequest.${policyViolationId}`;
}

export function readPendingWaiverRequestSessionFlag(policyViolationId: string): boolean {
  if (!policyViolationId || typeof sessionStorage === 'undefined') return false;
  try {
    return sessionStorage.getItem(pendingWaiverRequestSessionKey(policyViolationId)) === '1';
  } catch {
    return false;
  }
}

export function writePendingWaiverRequestSessionFlag(policyViolationId: string): void {
  if (!policyViolationId || typeof sessionStorage === 'undefined') return;
  try {
    sessionStorage.setItem(pendingWaiverRequestSessionKey(policyViolationId), '1');
  } catch {
    // ignore quota / private-mode failures
  }
}

/** Clear the same-tab pending gate after approve / reject / withdraw. */
export function clearPendingWaiverRequestSessionFlag(policyViolationId: string): void {
  if (!policyViolationId || typeof sessionStorage === 'undefined') return;
  try {
    sessionStorage.removeItem(pendingWaiverRequestSessionKey(policyViolationId));
  } catch {
    // ignore private-mode failures
  }
}
