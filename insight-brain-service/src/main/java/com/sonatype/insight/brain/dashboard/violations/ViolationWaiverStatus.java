/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;

/**
 * Indexed {@code policyViolationWaiverStatus} values and their mapping to the API-facing
 * {@link PolicyViolationState}.
 * <p>
 * The four status strings are bound directly to the canonical constants written by
 * {@link DocumentBuilderHelper} when building {@code POLICY_VIOLATION} documents, so a change to the
 * indexed vocabulary is a compile-time break here rather than a silent runtime divergence (which would
 * otherwise map every violation to {@code OPEN} and zero out state facets).
 * <p>
 * The field is single-valued, so a violation that is both waived and legacy indexes as {@code Waived}
 * (waiver wins; see {@link DocumentBuilderHelper} {@code deriveWaiverStatus}). It therefore reads back
 * as {@link PolicyViolationState#WAIVED} and appears under WAIVED, not LEGACY — a deliberate divergence
 * from the SQL read path (where it is a member of both states). Only pure-legacy (non-waived)
 * violations map to {@link PolicyViolationState#LEGACY_VIOLATION}.
 */
final class ViolationWaiverStatus
{
  static final String ACTIVE = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_ACTIVE;

  static final String WAIVED = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_WAIVED;

  static final String AUTO_WAIVED = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_AUTO_WAIVED;

  static final String LEGACY = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_LEGACY;

  private ViolationWaiverStatus() {
  }

  /** Maps an indexed waiver status to the API violation state. */
  static PolicyViolationState toState(final String waiverStatus) {
    if (WAIVED.equals(waiverStatus) || AUTO_WAIVED.equals(waiverStatus)) {
      return PolicyViolationState.WAIVED;
    }
    if (LEGACY.equals(waiverStatus)) {
      return PolicyViolationState.LEGACY_VIOLATION;
    }
    // ACTIVE — and any absent/unknown status — is OPEN. ViolationsListIndexQueryBuilder.buildStateClause
    // expresses the OPEN filter as "NOT (Waived AutoWaived Legacy)" for exactly this reason, so filter,
    // facet count, and row state stay in agreement (OPEN must exclude Legacy too, or Legacy leaks in).
    return PolicyViolationState.OPEN;
  }

  static boolean isAutoWaived(final String waiverStatus) {
    return AUTO_WAIVED.equals(waiverStatus);
  }

  /**
   * The set of indexed waiver statuses that are excluded from OPEN, as a Lucene clause body
   * ({@code Waived AutoWaived Legacy}). Shared by the state filter
   * ({@link ViolationsListIndexQueryBuilder}) and the OPEN facet count
   * ({@link ViolationsListFacetsBuilder}) so the two cannot drift: OPEN is {@code NOT (<this>)} on both
   * paths. Any status added here must also be added to {@link #toState} above.
   */
  static String openExclusionStatuses() {
    return WAIVED + " " + AUTO_WAIVED + " " + LEGACY;
  }
}
