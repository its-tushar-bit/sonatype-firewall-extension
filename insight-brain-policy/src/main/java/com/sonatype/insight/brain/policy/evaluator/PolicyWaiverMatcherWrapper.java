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
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.policy.comparison.ConstraintFactsListComparator;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.140
 */
public class PolicyWaiverMatcherWrapper
{
  private static final Logger log = LoggerFactory.getLogger(PolicyWaiverMatcherWrapper.class);

  private final PolicyWaiver policyWaiver;

  public PolicyWaiverMatcherWrapper(PolicyWaiver policyWaiver) {
    this.policyWaiver = policyWaiver;
  }

  public boolean matchesPolicyId(String policyId) {
    return policyWaiver.getPolicyId().equals(policyId);
  }

  public boolean matchesComponent(ComponentFact componentFact) {
    ComponentMatcherStrategyForWaiver componentMatcherStrategy = policyWaiver.getComponentMatchStrategy() == null
        ? ComponentMatcherStrategyForWaiver.DEFAULT
        : policyWaiver.getComponentMatchStrategy();

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
    ComponentMatcherStrategyForWaiver componentMatchStrategy = policyWaiver.getComponentMatchStrategy();
    return isWaiverForAllComponents(componentMatchStrategy) ||
        isWaiverForExactComponent(componentFact, componentMatchStrategy) ||
        matchesAllVersionsOfComponent(componentFact);
  }

  private boolean isWaiverForAllComponents(final ComponentMatcherStrategyForWaiver componentMatchStrategy) {
    return ComponentMatcherStrategyForWaiver.ALL_COMPONENTS.equals(componentMatchStrategy) && matchesAllComponents();
  }

  private boolean isWaiverForExactComponent(
      final ComponentFact componentFact,
      final ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    return ComponentMatcherStrategyForWaiver.EXACT_COMPONENT.equals(componentMatchStrategy) &&
        matchesComponentHash(componentFact);
  }

  private boolean matchesComponentIdentifier(ComponentFact componentFact) {
    ComponentIdentifier policyWaiverComponentIdentifier = policyWaiver.getComponentIdentifier();
    ComponentIdentifier componentFactComponentIdentifier = componentFact.getComponentIdentifier();

    if (policyWaiverComponentIdentifier == null ||
        componentFactComponentIdentifier == null ||
        isDifferentFormat(componentFactComponentIdentifier, policyWaiverComponentIdentifier))
    {
      return false;
    }

    try {
      policyWaiverComponentIdentifier.ensureComplete();
      componentFactComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      log.warn("Failed to ensureComplete for purl {} (or) component identifier {} with the following error: {}",
          policyWaiver.getAssociatedPackageUrl(), componentFactComponentIdentifier, e.getMessage());
      return compareWhenMissingRequiredCoordinates(policyWaiverComponentIdentifier, componentFactComponentIdentifier);
    }

    return policyWaiverComponentIdentifier.compareTo(componentFactComponentIdentifier) == 0;
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

  private boolean isDifferentFormat(ComponentIdentifier compIdentif, ComponentIdentifier waiverIdentif) {
    return !waiverIdentif.getFormat().equals(compIdentif.getFormat());
  }

  private boolean matchesAllVersionsOfComponent(ComponentFact componentFact) {
    if (policyWaiver.getComponentIdentifier() == null ||
        componentFact.getComponentIdentifier() == null ||
        isDifferentFormat(componentFact.getComponentIdentifier(), policyWaiver.getComponentIdentifier()))
    {
      return false;
    }

    ComponentIdentifier waiverAllVersionsComponentIdentifier =
        policyWaiver.getComponentIdentifier().createAlternativeVersion("*");
    // It seems at one point some identifiers where introduced that don't have all required coordinates, which
    // would make the call that is done to ensureComplete to fail. We'll log this occurrence for future reference
    // FIXME After CLM-22252 an additional database migrator should be implemented to update these
    try {
      waiverAllVersionsComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      log.warn("Failed to ensureComplete for purl {} with the following error: {}",
          policyWaiver.getAssociatedPackageUrl(), e.getMessage());
    }

    // The policy waiver component identifier is converted from a purl, and purl applies some name normalizations.
    // So we have to convert the component fact component identifier to purl and back to ensure the same normalizations
    // are applied.
    ComponentIdentifier componentFactComponentIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(componentFact.getComponentIdentifier()).toComponentIdentifier();
    ComponentIdentifier componentFactAllVersionsComponentIdentifier =
        componentFactComponentIdentifier.createAlternativeVersion("*");
    // FIXME This code block was introduced in CLM-22177 and it is dependent on the resolution of CLM-22252
    // FIXME After CLM-22252 task is done remove the try catch block and leave only the ensureComplete call
    try {
      componentFactAllVersionsComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      log.warn("Failed to ensureComplete for component identifier {} with the following error: {}",
          componentFactAllVersionsComponentIdentifier, e.getMessage());
      return compareWhenMissingRequiredCoordinates(waiverAllVersionsComponentIdentifier,
          componentFactAllVersionsComponentIdentifier);
    }

    return waiverAllVersionsComponentIdentifier.compareTo(componentFactAllVersionsComponentIdentifier) == 0;
  }

  boolean compareWhenMissingRequiredCoordinates(
      ComponentIdentifier waiverIdentifier,
      ComponentIdentifier componentIdentifier)
  {
    return componentIdentifier.getCoordinates()
        .entrySet()
        .stream()
        .allMatch(compCoord -> compCoord.getValue().equals(waiverIdentifier.get(compCoord.getKey())));
  }

  public boolean matchesConstraintFactsJson(String constraintFactsJson) {
    return !isLegacyWaiver() && policyWaiver.getConstraintFactsJson().equals(constraintFactsJson);
  }

  public boolean matchesConstraintFacts(List<ConstraintFact> constraintFacts) {
    return policyWaiver.getConstraintFacts() != null && ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR
        .compare(policyWaiver.getConstraintFacts(), constraintFacts) == 0;
  }

  boolean isLegacyWaiver() {
    return policyWaiver.getConstraintFacts() == null;
  }
}
