/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyThreatLevelOrFilterTest
{
  @Test
  public void sqlThreatLevelRanges_preservesDisjointBucketsNotEnvelope() {
    PolicyThreatLevelOrFilter filter = new PolicyThreatLevelOrFilter(List.of(
        new PolicyThreatLevelFilter(1, 2),
        new PolicyThreatLevelFilter(8, 10)));

    assertThat(filter.getMinPolicyThreatLevel()).isEqualTo(1);
    assertThat(filter.getMaxPolicyThreatLevel()).isEqualTo(10);
    assertThat(filter.sqlThreatLevelRanges()).containsExactly(
        Map.entry(1, 2),
        Map.entry(8, 10));
    assertThat(filter.test(5)).isFalse();
    assertThat(filter.test(9)).isTrue();
  }
}
