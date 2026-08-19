/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.Date;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationStateFilterTest
{
  @Test
  public void testSinglePolicyViolationState() {
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    PolicyViolation trueViolation = new PolicyViolation();
    trueViolation.setWaiveTime(null);

    PolicyViolation falseViolation = new PolicyViolation();
    falseViolation.setWaiveTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().test(trueViolation)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(falseViolation)).isFalse();
  }

  @Test
  public void testMultiplePolicyViolationStates() {
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter(PolicyViolationState.OPEN,
        PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION);

    PolicyViolation v1 = new PolicyViolation();
    v1.setWaiveTime(null);
    v1.setLegacyViolationTime(null);

    PolicyViolation v2 = new PolicyViolation();
    v2.setWaiveTime(new Date());
    v2.setLegacyViolationTime(null);

    PolicyViolation v3 = new PolicyViolation();
    v3.setWaiveTime(null);
    v3.setLegacyViolationTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v3)).isTrue();
  }

  @Test
  public void testLegacyViolationAndWaivedSet() {
    PolicyViolationStateFilter legacyViolationFilter =
        new PolicyViolationStateFilter(PolicyViolationState.LEGACY_VIOLATION);
    PolicyViolationStateFilter waivedFilter = new PolicyViolationStateFilter(PolicyViolationState.WAIVED);
    PolicyViolationStateFilter bothFilter = new PolicyViolationStateFilter(PolicyViolationState.WAIVED,
        PolicyViolationState.LEGACY_VIOLATION);

    PolicyViolation v1 = new PolicyViolation();
    v1.setWaiveTime(new Date());
    v1.setLegacyViolationTime(new Date());

    assertThat(legacyViolationFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(waivedFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(bothFilter.asPolicyViolationPredicate().test(v1)).isTrue();
  }

  @Test
  public void testLegacyViolationStates() {
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter(PolicyViolationState.LEGACY_VIOLATION);

    PolicyViolation v1 = new PolicyViolation();
    v1.setWaiveTime(null);
    v1.setLegacyViolationTime(null);

    PolicyViolation v2 = new PolicyViolation();
    v2.setWaiveTime(new Date());
    v2.setLegacyViolationTime(null);

    PolicyViolation v3 = new PolicyViolation();
    v3.setWaiveTime(null);
    v3.setLegacyViolationTime(new Date());

    PolicyViolation v4 = new PolicyViolation();
    v4.setWaiveTime(new Date());
    v4.setLegacyViolationTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isFalse();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isFalse();
    assertThat(filter.asPolicyViolationPredicate().test(v3)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v4)).isTrue();
  }

  @Test
  public void testEmptyPolicyViolationState() {
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter();

    PolicyViolation v1 = new PolicyViolation();
    v1.setWaiveTime(null);
    v1.setLegacyViolationTime(null);

    PolicyViolation v2 = new PolicyViolation();
    v2.setWaiveTime(new Date());
    v2.setLegacyViolationTime(null);

    PolicyViolation v3 = new PolicyViolation();
    v3.setWaiveTime(null);
    v3.setLegacyViolationTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v3)).isTrue();
  }

  @Test
  public void testNullPolicyViolationState() {
    Set<PolicyViolationState> nullPolicyViolationState = null;
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter(nullPolicyViolationState);
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    v1.setWaiveTime(null);
    v2.setWaiveTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isTrue();
  }
}
