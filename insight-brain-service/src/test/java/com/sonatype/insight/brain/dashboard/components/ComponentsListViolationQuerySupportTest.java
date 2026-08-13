/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.List;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentsListViolationQuerySupportTest
{
  @Test
  public void threatLevelFilterForCardEnrichment_returnsNullWhenUnset() {
    assertThat(ComponentsListViolationQuerySupport.threatLevelFilterForCardEnrichment(null)).isNull();
    assertThat(ComponentsListViolationQuerySupport.threatLevelFilterForCardEnrichment(new ComponentsListRequestDTO()))
        .isNull();
  }

  @Test
  public void threatLevelFilterForCardEnrichment_returnsSingularRange() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.policyThreatLevelRange = new PolicyThreatLevelFilter(3, 9);

    PolicyThreatLevelFilter filter =
        ComponentsListViolationQuerySupport.threatLevelFilterForCardEnrichment(request);

    assertThat(filter).isNotNull();
    assertThat(filter.getMinPolicyThreatLevel()).isEqualTo(3);
    assertThat(filter.getMaxPolicyThreatLevel()).isEqualTo(9);
    assertThat(filter).isNotInstanceOf(PolicyThreatLevelOrFilter.class);
  }

  @Test
  public void threatLevelFilterForCardEnrichment_orsMultipleBuckets() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.policyThreatLevelRanges = List.of(
        new PolicyThreatLevelFilter(1, 2),
        new PolicyThreatLevelFilter(8, 10));

    PolicyThreatLevelFilter filter =
        ComponentsListViolationQuerySupport.threatLevelFilterForCardEnrichment(request);

    assertThat(filter).isInstanceOf(PolicyThreatLevelOrFilter.class);
    assertThat(filter.getMinPolicyThreatLevel()).isEqualTo(1);
    assertThat(filter.getMaxPolicyThreatLevel()).isEqualTo(10);
    assertThat(filter.test(2)).isTrue();
    assertThat(filter.test(9)).isTrue();
    assertThat(filter.test(5)).isFalse();
  }
}
