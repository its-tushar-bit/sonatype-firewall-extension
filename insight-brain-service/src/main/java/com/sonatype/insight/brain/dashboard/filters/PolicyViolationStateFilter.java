/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @since 1.27
 */
public class PolicyViolationStateFilter
    implements Predicate<Set<PolicyViolationState>>
{
  private Set<PolicyViolationState> policyViolationStates = EnumSet.noneOf(PolicyViolationState.class);

  public PolicyViolationStateFilter() {
    // No argument constructor for convenience.
  }

  public Set<PolicyViolationState> getPolicyViolationStates() {
    return policyViolationStates;
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
  public boolean test(Set<PolicyViolationState> states) {
    return policyViolationStates.isEmpty() || policyViolationStates.stream().anyMatch(states::contains);
  }

  /**
   * Transforms this predicate into one that applies the same filtering to policy violations.
   */
  public Predicate<AbstractPolicyViolation> asPolicyViolationPredicate() {
    return new Predicate<>()
    {
      @Override
      public boolean test(AbstractPolicyViolation policyViolation) {
        return PolicyViolationStateFilter.this.test(getPolicyViolationStates(policyViolation));
      }

      private Set<PolicyViolationState> getPolicyViolationStates(AbstractPolicyViolation policyViolation) {
        Set<PolicyViolationState> states = EnumSet.noneOf(PolicyViolationState.class);
        if (policyViolation.isWaived()) {
          states.add(PolicyViolationState.WAIVED);
        }
        if (policyViolation.isLegacyViolation()) {
          states.add(PolicyViolationState.LEGACY_VIOLATION);
        }
        if (states.isEmpty()) {
          states.add(PolicyViolationState.OPEN);
        }
        return states;
      }
    };
  }
}
