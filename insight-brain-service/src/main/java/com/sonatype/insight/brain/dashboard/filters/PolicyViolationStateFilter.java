/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * @since 1.27
 */
public class PolicyViolationStateFilter
    implements Predicate<PolicyViolationState>
{
  private Set<PolicyViolationState> policyViolationStates = EnumSet.noneOf(PolicyViolationState.class);

  public PolicyViolationStateFilter() {
    // No argument constructor for convenience.
  }

  /**
   * @param states A set of {@link PolicyViolationState}s.
   */
  @JsonCreator
  public PolicyViolationStateFilter(Set<PolicyViolationState> states) {
    if (states != null) {
      policyViolationStates.addAll(states);
    }
  }

  public PolicyViolationStateFilter(PolicyViolationState... states) {
    if (states != null) {
      Collections.addAll(policyViolationStates, states);
    }
  }

  @Override
  public boolean apply(PolicyViolationState state) {
    return policyViolationStates.isEmpty() || policyViolationStates.contains(state);
  }

  /**
   * Transforms this predicate into one that applies the same filtering to policy violations.
   */
  public Predicate<PolicyViolation> asPolicyViolationPredicate() {
    return Predicates.compose(this, new Function<PolicyViolation, PolicyViolationState>()
    {
      @Override
      @Nullable
      public PolicyViolationState apply(@Nullable PolicyViolation input) {
        return input.isWaived() ? PolicyViolationState.WAIVED : PolicyViolationState.OPEN;
      }
    });
  }
}
