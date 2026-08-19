/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.policy.comparison.ConstraintFactsListComparator;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyWaiverRequestMatcherWrapper
{
  private static final Logger log = LoggerFactory.getLogger(PolicyWaiverRequestMatcherWrapper.class);

  private final PolicyWaiverRequest policyWaiverRequest;

  public PolicyWaiverRequestMatcherWrapper(PolicyWaiverRequest policyWaiverRequest) {
    this.policyWaiverRequest = policyWaiverRequest;
  }

  public boolean matchesPolicyId(String policyId) {
    return policyWaiverRequest.getPolicyId().equals(policyId);
  }

  public boolean matchesComponent(ComponentFact componentFact) {
    ComponentMatcherStrategyForWaiver componentMatcherStrategy =
        policyWaiverRequest.getComponentMatchStrategy() == null
            ? ComponentMatcherStrategyForWaiver.DEFAULT
            : policyWaiverRequest.getComponentMatchStrategy();

    switch (componentMatcherStrategy) {
      case EXACT_COMPONENT:
        if (componentFact != null && componentFact.getHash() != null) {
          return componentFactNotNull(componentFact).matchesComponentHash(componentFact);
        }
        else {
          return componentFactNotNull(componentFact).matchesComponentIdentifier(componentFact);
        }
      case ALL_COMPONENTS:
        return matchesAllComponents();
      case ALL_VERSIONS:
        return componentFactNotNull(componentFact).matchesAllVersionsOfComponent(componentFact);
      default:
        return matchesAllComponents() || componentFactNotNull(componentFact).matchesComponentHash(componentFact);
    }
  }

  public boolean matchesComponentOrAnyVersionOfComponent(ComponentFact componentFact) {
    componentFactNotNull(componentFact);
    ComponentMatcherStrategyForWaiver componentMatchStrategy = policyWaiverRequest.getComponentMatchStrategy();
    return isWaiverRequestForAllComponents(componentMatchStrategy)
        || isWaiverRequestForExactComponent(componentFact, componentMatchStrategy)
        || matchesAllVersionsOfComponent(componentFact);
  }

  private boolean isWaiverRequestForAllComponents(ComponentMatcherStrategyForWaiver componentMatchStrategy) {
    return ComponentMatcherStrategyForWaiver.ALL_COMPONENTS.equals(componentMatchStrategy) && matchesAllComponents();
  }

  private boolean isWaiverRequestForExactComponent(
      ComponentFact componentFact,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    return ComponentMatcherStrategyForWaiver.EXACT_COMPONENT.equals(componentMatchStrategy)
        && matchesComponentHash(componentFact);
  }

  private boolean matchesComponentIdentifier(ComponentFact componentFact) {
    ComponentIdentifier policyWaiverRequestComponentIdentifier = policyWaiverRequest.getComponentIdentifier();
    ComponentIdentifier componentFactComponentIdentifier = componentFact.getComponentIdentifier();

    if (policyWaiverRequestComponentIdentifier == null || componentFactComponentIdentifier == null
        || isDifferentFormat(componentFactComponentIdentifier, policyWaiverRequestComponentIdentifier))
    {
      return false;
    }

    try {
      policyWaiverRequestComponentIdentifier.ensureComplete();
      componentFactComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      log.warn("Failed to ensureComplete for purl {} (or) component identifier {} with the following error: {}",
          policyWaiverRequest.getAssociatedPackageUrl(), componentFactComponentIdentifier, e.getMessage());
      return compareWhenMissingRequiredCoordinates(policyWaiverRequestComponentIdentifier,
          componentFactComponentIdentifier);
    }

    return policyWaiverRequestComponentIdentifier.compareTo(componentFactComponentIdentifier) == 0;
  }

  private PolicyWaiverRequestMatcherWrapper componentFactNotNull(ComponentFact componentFact) {
    if (componentFact == null) {
      throw new RuntimeException("componentFact is required but got null instead");
    }
    else {
      return this;
    }
  }

  private boolean matchesAllComponents() {
    return policyWaiverRequest.getHash() == null;
  }

  private boolean matchesComponentHash(ComponentFact componentFact) {
    return policyWaiverRequest.getHash().equals(componentFact.getHash());
  }

  private boolean isDifferentFormat(ComponentIdentifier compIdentif, ComponentIdentifier waiverIdentif) {
    return !waiverIdentif.getFormat().equals(compIdentif.getFormat());
  }

  private boolean matchesAllVersionsOfComponent(ComponentFact componentFact) {
    if (policyWaiverRequest.getComponentIdentifier() == null || componentFact.getComponentIdentifier() == null
        || isDifferentFormat(componentFact.getComponentIdentifier(), policyWaiverRequest.getComponentIdentifier()))
    {
      return false;
    }

    ComponentIdentifier waiverRequestAllVersionsComponentIdentifier =
        policyWaiverRequest.getComponentIdentifier().createAlternativeVersion("*");
    try {
      waiverRequestAllVersionsComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      log.warn("Failed to ensureComplete for purl {} with the following error: {}",
          policyWaiverRequest.getAssociatedPackageUrl(), e.getMessage());
    }

    // The policy waiver request component identifier is converted from a purl, and purl applies some name
    // normalizations.
    // So we have to convert the component fact component identifier to purl and back to ensure the same normalizations
    // are applied.
    ComponentIdentifier componentFactComponentIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(componentFact.getComponentIdentifier()).toComponentIdentifier();
    ComponentIdentifier componentFactAllVersionsComponentIdentifier =
        componentFactComponentIdentifier.createAlternativeVersion("*");
    try {
      componentFactAllVersionsComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      log.warn("Failed to ensureComplete for component identifier {} with the following error: {}",
          componentFactAllVersionsComponentIdentifier, e.getMessage());
      return compareWhenMissingRequiredCoordinates(waiverRequestAllVersionsComponentIdentifier,
          componentFactAllVersionsComponentIdentifier);
    }

    return waiverRequestAllVersionsComponentIdentifier.compareTo(componentFactAllVersionsComponentIdentifier) == 0;
  }

  boolean compareWhenMissingRequiredCoordinates(
      ComponentIdentifier waiverRequestIdentifier,
      ComponentIdentifier componentIdentifier)
  {
    return componentIdentifier.getCoordinates()
        .entrySet()
        .stream()
        .allMatch(compCoord -> compCoord.getValue().equals(waiverRequestIdentifier.get(compCoord.getKey())));
  }

  public boolean matchesConstraintFactsJson(String constraintFactsJson) {
    return policyWaiverRequest.getConstraintFactsJson().equals(constraintFactsJson);
  }

  public boolean matchesConstraintFacts(List<ConstraintFact> constraintFacts) {
    return policyWaiverRequest.getConstraintFacts() != null
        && ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR
            .compare(policyWaiverRequest.getConstraintFacts(), constraintFacts) == 0;
  }
}
