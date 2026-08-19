/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.List;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;

/**
 * OR-combines multiple {@link PolicyThreatLevelFilter} buckets for Classic card enrichment, which
 * only accepts a single filter instance. {@link #test(Integer)} matches any bucket; min/max expose
 * the envelope so {@link com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader} can
 * still pre-filter the load window.
 */
final class PolicyThreatLevelOrFilter
    extends PolicyThreatLevelFilter
{
  private final List<PolicyThreatLevelFilter> ranges;

  PolicyThreatLevelOrFilter(final List<PolicyThreatLevelFilter> ranges) {
    super(envelopeMin(ranges), envelopeMax(ranges));
    this.ranges = List.copyOf(ranges);
  }

  @Override
  public boolean test(final Integer threatLevel) {
    if (threatLevel == null) {
      return false;
    }
    for (PolicyThreatLevelFilter range : ranges) {
      if (range.test(threatLevel)) {
        return true;
      }
    }
    return false;
  }

  private static void requireNonEmpty(final List<PolicyThreatLevelFilter> ranges) {
    if (ranges == null || ranges.isEmpty()) {
      throw new IllegalArgumentException("PolicyThreatLevelOrFilter requires at least one threat range.");
    }
    for (PolicyThreatLevelFilter range : ranges) {
      if (range == null) {
        throw new IllegalArgumentException("PolicyThreatLevelOrFilter ranges must not contain null elements.");
      }
    }
  }

  private static int envelopeMin(final List<PolicyThreatLevelFilter> ranges) {
    requireNonEmpty(ranges);
    int min = Integer.MAX_VALUE;
    for (PolicyThreatLevelFilter range : ranges) {
      min = Math.min(min, range.getMinPolicyThreatLevel());
    }
    return min;
  }

  private static int envelopeMax(final List<PolicyThreatLevelFilter> ranges) {
    requireNonEmpty(ranges);
    int max = Integer.MIN_VALUE;
    for (PolicyThreatLevelFilter range : ranges) {
      max = Math.max(max, range.getMaxPolicyThreatLevel());
    }
    return max;
  }
}
