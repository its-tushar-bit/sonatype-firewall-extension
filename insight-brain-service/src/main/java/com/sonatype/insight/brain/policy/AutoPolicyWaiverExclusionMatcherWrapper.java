/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.comparison.ConstraintFactsListComparator;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoPolicyWaiverExclusionMatcherWrapper
{
  private static final Logger log = LoggerFactory.getLogger(AutoPolicyWaiverExclusionMatcherWrapper.class);

  private final AutoPolicyWaiverExclusion autoPolicyWaiverExclusion;

  public AutoPolicyWaiverExclusionMatcherWrapper(AutoPolicyWaiverExclusion autoPolicyWaiverExclusion) {
    this.autoPolicyWaiverExclusion = autoPolicyWaiverExclusion;
  }

  public boolean matchesViolation(PolicyViolation policyViolation) {
    ComponentMatcherStrategyForExclusion componentMatcherStrategy =
        autoPolicyWaiverExclusion.getComponentMatchStrategy() == null ?
            ComponentMatcherStrategyForExclusion.EXACT_COMPONENT :
            autoPolicyWaiverExclusion.getComponentMatchStrategy();

    policyViolationNotNull(policyViolation);
    ComponentFact componentFact =
        new ComponentFact(policyViolation.getComponentIdentifier(), policyViolation.getHash());

    switch (componentMatcherStrategy) {
      case POLICY_VIOLATION:
        return matchesPolicyViolation(policyViolation);
      case ALL_VERSIONS:
        return componentFactNotNull(componentFact).matchesAllVersionsOfComponent(componentFact);
      case EXACT_COMPONENT:
        if (componentFact.getHash() != null) {
          return componentFactNotNull(componentFact).matchesComponentHash(componentFact);
        }
        else {
          return componentFactNotNull(componentFact).matchesComponentIdentifier(componentFact);
        }
      default:
        return componentFactNotNull(componentFact).matchesComponentHash(componentFact);
    }
  }

  private AutoPolicyWaiverExclusionMatcherWrapper componentFactNotNull(ComponentFact componentFact) {
    if (componentFact == null) {
      throw new RuntimeException("componentFact is required but got null instead");
    }
    else {
      return this;
    }
  }

  private AutoPolicyWaiverExclusionMatcherWrapper policyViolationNotNull(PolicyViolation policyViolation) {
    if (policyViolation == null) {
      throw new RuntimeException("policyViolation is required but got null instead");
    }
    else {
      return this;
    }
  }

  private boolean matchesComponentHash(ComponentFact componentFact) {
    return autoPolicyWaiverExclusion.getHash().equals(componentFact.getHash());
  }

  private boolean matchesComponentIdentifier(ComponentFact componentFact) {
    ComponentIdentifier exclusionComponentIdentifier =
        autoPolicyWaiverExclusion.getComponentIdentifier();
    ComponentIdentifier componentFactComponentIdentifier = componentFact.getComponentIdentifier();

    if (exclusionComponentIdentifier == null || componentFactComponentIdentifier == null) {
      return false;
    }

    try {
      exclusionComponentIdentifier.ensureComplete();
      componentFactComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      return compareWhenMissingRequiredCoordinates(exclusionComponentIdentifier, componentFactComponentIdentifier);
    }

    return exclusionComponentIdentifier.compareTo(componentFactComponentIdentifier) == 0;
  }

  boolean compareWhenMissingRequiredCoordinates(
      ComponentIdentifier exclusionIdentifier,
      ComponentIdentifier componentIdentifier)
  {
    return componentIdentifier.getCoordinates().entrySet().stream()
        .allMatch(compCoord -> compCoord.getValue().equals(exclusionIdentifier.get(compCoord.getKey())));
  }

  private boolean isDifferentFormat(ComponentIdentifier compIdentif, ComponentIdentifier waiverIdentif) {
    return !waiverIdentif.getFormat().equals(compIdentif.getFormat());
  }

  private boolean matchesAllVersionsOfComponent(ComponentFact componentFact) {
    if (autoPolicyWaiverExclusion.getComponentIdentifier() == null ||
        componentFact.getComponentIdentifier() == null ||
        isDifferentFormat(componentFact.getComponentIdentifier(),
            autoPolicyWaiverExclusion.getComponentIdentifier())) {
      return false;
    }

    ComponentIdentifier waiverAllVersionsComponentIdentifier =
        autoPolicyWaiverExclusion.getComponentIdentifier().createAlternativeVersion("*");
    try {
      waiverAllVersionsComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      log.warn("Failed to ensureComplete for purl {} with the following error: {}",
          autoPolicyWaiverExclusion.getAssociatedPackageUrl(), e.getMessage());
    }

    ComponentIdentifier componentFactComponentIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(componentFact.getComponentIdentifier()).toComponentIdentifier();
    ComponentIdentifier componentFactAllVersionsComponentIdentifier =
        componentFactComponentIdentifier.createAlternativeVersion("*");
    try {
      componentFactAllVersionsComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      return compareWhenMissingRequiredCoordinates(waiverAllVersionsComponentIdentifier,
          componentFactAllVersionsComponentIdentifier);
    }

    return waiverAllVersionsComponentIdentifier.compareTo(componentFactAllVersionsComponentIdentifier) == 0;
  }

  private boolean matchesPolicyViolation(PolicyViolation policyViolation) {
    // Policy ID
    if (autoPolicyWaiverExclusion.getPolicyId() == null || policyViolation.getPolicyId() == null) {
      return false;
    }
    if (autoPolicyWaiverExclusion.getPolicyId().compareTo(policyViolation.getPolicyId()) != 0) {
      return false;
    }

    // Threat level
    if (autoPolicyWaiverExclusion.getThreatLevel() == null) {
      return false;
    }
    if (autoPolicyWaiverExclusion.getThreatLevel() != policyViolation.getThreatLevel()) {
      return false;
    }

    // Hash and component identifier
    if (autoPolicyWaiverExclusion.getHash() == null || policyViolation.getHash() == null) {
      return false;
    }
    if (!Objects.equals(autoPolicyWaiverExclusion.getHash(), policyViolation.getHash())) {
      return false;
    }
    
    try {
      autoPolicyWaiverExclusion.getComponentIdentifier().ensureComplete();
      if (autoPolicyWaiverExclusion.getComponentIdentifier().compareTo(policyViolation.getComponentIdentifier()) !=
          0) {
        return false;
      }
    }
    catch (InvalidComponentIdentifierException e) {
      return false;
    }
    try {
      // Constraint facts
      if (autoPolicyWaiverExclusion.getConstraintFacts() == null || policyViolation.getConstraintFacts() == null) {
        return false;
      }
      return ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR.compare(
          policyViolation.getConstraintFacts(),
          autoPolicyWaiverExclusion.getConstraintFacts()
      ) == 0;
    }
    catch (NullPointerException e) {
      return false;
    }
  }
}
