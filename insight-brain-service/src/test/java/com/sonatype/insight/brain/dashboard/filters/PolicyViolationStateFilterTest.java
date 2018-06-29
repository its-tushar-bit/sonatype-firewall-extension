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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicyViolationStateFilterTest
{

  @Test
  public void testSinglePolicyViolationState() {
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter(PolicyViolationState.OPEN);
    PolicyViolation trueViolation = new PolicyViolation();
    trueViolation.setWaiveTime(null);

    PolicyViolation falseViolation = new PolicyViolation();
    falseViolation.setWaiveTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().apply(trueViolation), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(falseViolation), is(false));
  }

  @Test
  public void testMultiplePolicyViolationStates() {
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter(PolicyViolationState.OPEN,
        PolicyViolationState.WAIVED, PolicyViolationState.GRANDFATHERED);

    PolicyViolation v1 = new PolicyViolation();
    v1.setWaiveTime(null);
    v1.setGrandfatherTime(null);

    PolicyViolation v2 = new PolicyViolation();
    v2.setWaiveTime(new Date());
    v2.setGrandfatherTime(null);

    PolicyViolation v3 = new PolicyViolation();
    v3.setWaiveTime(null);
    v3.setGrandfatherTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().apply(v1), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v2), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v3), is(true));
  }

  @Test
  public void testGrandfatheredAndWaivedSet() {
    PolicyViolationStateFilter grandfatherFilter = new PolicyViolationStateFilter(PolicyViolationState.GRANDFATHERED);
    PolicyViolationStateFilter waivedFilter = new PolicyViolationStateFilter(PolicyViolationState.WAIVED);
    PolicyViolationStateFilter bothFilter = new PolicyViolationStateFilter(PolicyViolationState.WAIVED,
        PolicyViolationState.GRANDFATHERED);

    PolicyViolation v1 = new PolicyViolation();
    v1.setWaiveTime(new Date());
    v1.setGrandfatherTime(new Date());

    assertThat(grandfatherFilter.asPolicyViolationPredicate().apply(v1), is(true));
    assertThat(waivedFilter.asPolicyViolationPredicate().apply(v1), is(true));
    assertThat(bothFilter.asPolicyViolationPredicate().apply(v1), is(true));
  }

  @Test
  public void testGrandfatheredViolationStates() {
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter(PolicyViolationState.GRANDFATHERED);

    PolicyViolation v1 = new PolicyViolation();
    v1.setWaiveTime(null);
    v1.setGrandfatherTime(null);

    PolicyViolation v2 = new PolicyViolation();
    v2.setWaiveTime(new Date());
    v2.setGrandfatherTime(null);

    PolicyViolation v3 = new PolicyViolation();
    v3.setWaiveTime(null);
    v3.setGrandfatherTime(new Date());

    PolicyViolation v4 = new PolicyViolation();
    v4.setWaiveTime(new Date());
    v4.setGrandfatherTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().apply(v1), is(false));
    assertThat(filter.asPolicyViolationPredicate().apply(v2), is(false));
    assertThat(filter.asPolicyViolationPredicate().apply(v3), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v4), is(true));
  }

  @Test
  public void testEmptyPolicyViolationState() {
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter();

    PolicyViolation v1 = new PolicyViolation();
    v1.setWaiveTime(null);
    v1.setGrandfatherTime(null);

    PolicyViolation v2 = new PolicyViolation();
    v2.setWaiveTime(new Date());
    v2.setGrandfatherTime(null);

    PolicyViolation v3 = new PolicyViolation();
    v3.setWaiveTime(null);
    v3.setGrandfatherTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().apply(v1), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v2), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v3), is(true));
  }

  @Test
  public void testNullPolicyViolationState() {
    Set<PolicyViolationState> nullPolicyViolationState = null;
    PolicyViolationStateFilter filter = new PolicyViolationStateFilter(nullPolicyViolationState);
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    v1.setWaiveTime(null);
    v2.setWaiveTime(new Date());

    assertThat(filter.asPolicyViolationPredicate().apply(v1), is(true));
    assertThat(filter.asPolicyViolationPredicate().apply(v2), is(true));
  }
}
