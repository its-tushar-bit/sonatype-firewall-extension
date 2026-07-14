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
 * The three status strings are bound directly to the canonical constants written by
 * {@link DocumentBuilderHelper} when building {@code POLICY_VIOLATION} documents, so a change to the
 * indexed vocabulary is a compile-time break here rather than a silent runtime divergence (which would
 * otherwise map every violation to {@code OPEN} and zero out state facets).
 */
final class ViolationWaiverStatus
{
  static final String ACTIVE = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_ACTIVE;

  static final String WAIVED = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_WAIVED;

  static final String AUTO_WAIVED = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_AUTO_WAIVED;

  private ViolationWaiverStatus() {
  }

  /** Maps an indexed waiver status to the API violation state. */
  static PolicyViolationState toState(final String waiverStatus) {
    if (WAIVED.equals(waiverStatus) || AUTO_WAIVED.equals(waiverStatus)) {
      return PolicyViolationState.WAIVED;
    }
    // ACTIVE — and any absent/unknown status — is OPEN. ViolationsListIndexQueryBuilder.buildStateClause
    // expresses the OPEN filter as "NOT (Waived AutoWaived)" for exactly this reason, so filter, facet
    // count, and row state stay in agreement.
    return PolicyViolationState.OPEN;
  }

  static boolean isAutoWaived(final String waiverStatus) {
    return AUTO_WAIVED.equals(waiverStatus);
  }
}
