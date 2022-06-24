/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.policy.comparison.ConstraintFactsListComparator;

/**
 * @since 1.140
 */
public class PolicyWaiverMatcherWrapper
{
  // Exposed to the same package for testing purposes
  final PolicyWaiver policyWaiver;

  public PolicyWaiverMatcherWrapper(PolicyWaiver waiver) {
    this.policyWaiver = waiver;
  }

  public boolean matchesPolicyId(String policyId) {
    return policyWaiver.getPolicyId().equals(policyId);
  }

  public boolean matchesComponent(ComponentFact componentFact) {
    ComponentMatcherStrategyForWaiver componentMatcherStrategy = policyWaiver.getComponentMatchStrategy() == null ?
        ComponentMatcherStrategyForWaiver.DEFAULT :
        policyWaiver.getComponentMatchStrategy();

    switch (componentMatcherStrategy) {
      case EXACT_COMPONENT:
        return componentFactNotNull(componentFact).matchesComponentHash(componentFact);
      case ALL_COMPONENTS:
        return matchesAllComponents();
      case ALL_VERSIONS:
        return componentFactNotNull(componentFact).matchesAllVersionsOfComponent(componentFact);
      default:
        return matchesAllComponents() || componentFactNotNull(componentFact).matchesComponentHash(componentFact);
    }
  }

  private PolicyWaiverMatcherWrapper componentFactNotNull(ComponentFact componentFact) {
    if (componentFact == null) {
      throw new RuntimeException("componentFact is required but got null instead");
    }
    else {
      return this;
    }
  }

  private boolean matchesAllComponents() {
    return policyWaiver.getHash() == null;
  }

  private boolean matchesComponentHash(ComponentFact componentFact) {
    return policyWaiver.getHash().equals(componentFact.getHash());
  }

  private boolean matchesAllVersionsOfComponent(ComponentFact componentFact) {
    if (policyWaiver.getComponentIdentifier() == null) {
      return false;
    }

    ComponentIdentifier componentFactAllVersionsIdentifier =
        componentFact.getComponentIdentifier().createAlternativeVersion("*");
    // The stored version in the waiver should already be the wildcard version so no need to create
    // an alternative version
    ComponentIdentifier waiverAllVersionsIdentifier = policyWaiver.getComponentIdentifier();

    componentFactAllVersionsIdentifier.ensureComplete();

    return waiverAllVersionsIdentifier.compareTo(componentFactAllVersionsIdentifier) == 0;
  }

  public boolean matchesConstraintFactsJson(String constraintFactsJson) {
    return !isLegacyWaiver() && policyWaiver.getConstraintFactsJson().equals(constraintFactsJson);
  }

  boolean matchesConstraintFacts(List<ConstraintFact> constraintFacts) {
    return ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR.compare(
        policyWaiver.getConstraintFacts(), constraintFacts) == 0;
  }

  boolean isLegacyWaiver() {
    return policyWaiver.getConstraintFacts() == null;
  }
}
