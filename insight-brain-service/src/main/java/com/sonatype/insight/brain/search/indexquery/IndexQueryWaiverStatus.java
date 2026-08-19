/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;

/**
 * Indexed {@code policyViolationWaiverStatus} values and their mapping to the API-facing violation
 * state and waiver type. Bound to the canonical constants written by {@link DocumentBuilderHelper}
 * so a change to the indexed vocabulary is a compile-time break here, not a silent divergence that
 * would map every violation to {@code OPEN} and zero out state facets.
 */
final class IndexQueryWaiverStatus
{
  static final String ACTIVE = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_ACTIVE;

  static final String WAIVED = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_WAIVED;

  static final String AUTO_WAIVED = DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_AUTO_WAIVED;

  /** API state values. */
  static final String STATE_OPEN = "OPEN";

  static final String STATE_WAIVED = "WAIVED";

  /** API waiver-type values (only meaningful for WAIVED violations). */
  static final String WAIVER_TYPE_MANUAL = "MANUAL";

  static final String WAIVER_TYPE_AUTO = "AUTO";

  private IndexQueryWaiverStatus() {
  }

  /** Maps an indexed waiver status to the API violation state. Absent/unknown status is OPEN. */
  static String toState(final String waiverStatus) {
    return isWaived(waiverStatus) ? STATE_WAIVED : STATE_OPEN;
  }

  /** {@code MANUAL}/{@code AUTO} for waived violations; {@code null} for open (no waiver type). */
  static String toWaiverType(final String waiverStatus) {
    if (AUTO_WAIVED.equals(waiverStatus)) {
      return WAIVER_TYPE_AUTO;
    }
    if (WAIVED.equals(waiverStatus)) {
      return WAIVER_TYPE_MANUAL;
    }
    return null;
  }

  static boolean isWaived(final String waiverStatus) {
    return WAIVED.equals(waiverStatus) || AUTO_WAIVED.equals(waiverStatus);
  }

  /**
   * The {@code (field:"Waived" OR field:"AutoWaived")} clause that defines a waived violation, built
   * once here so the filter compiler and the state facet share a single source of truth (adding a
   * third waived status only changes this method). {@code field} is the caller's waiver-status field.
   */
  static String waivedClause(final String field) {
    return "(" + field + ":\"" + WAIVED + "\" OR " + field + ":\"" + AUTO_WAIVED + "\")";
  }
}
