/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoPolicyWaiverRevocationMatcherWrapper
{
  private static final Logger log = LoggerFactory.getLogger(AutoPolicyWaiverRevocationMatcherWrapper.class);

  private final AutoPolicyWaiverRevocation autoPolicyWaiverRevocation;

  public AutoPolicyWaiverRevocationMatcherWrapper(AutoPolicyWaiverRevocation autoPolicyWaiverRevocation) {
    this.autoPolicyWaiverRevocation = autoPolicyWaiverRevocation;
  }

  public boolean matchesComponent(ComponentFact componentFact) {
    ComponentMatcherStrategyForRevocation componentMatcherStrategy =
        autoPolicyWaiverRevocation.getComponentMatchStrategy() == null ?
            ComponentMatcherStrategyForRevocation.EXACT_COMPONENT :
            autoPolicyWaiverRevocation.getComponentMatchStrategy();

    switch (componentMatcherStrategy) {
      case EXACT_COMPONENT:
        if (componentFact != null && componentFact.getHash() != null) {
          return componentFactNotNull(componentFact).matchesComponentHash(componentFact);
        }
        else {
          return componentFactNotNull(componentFact).matchesComponentIdentifier(componentFact);
        }
      case ALL_VERSIONS:
        return componentFactNotNull(componentFact).matchesAllVersionsOfComponent(componentFact);
      default:
        return componentFactNotNull(componentFact).matchesComponentHash(componentFact);
    }
  }

  private AutoPolicyWaiverRevocationMatcherWrapper componentFactNotNull(ComponentFact componentFact) {
    if (componentFact == null) {
      throw new RuntimeException("componentFact is required but got null instead");
    }
    else {
      return this;
    }
  }

  private boolean matchesComponentHash(ComponentFact componentFact) {
    return autoPolicyWaiverRevocation.getHash().equals(componentFact.getHash());
  }

  private boolean matchesComponentIdentifier(ComponentFact componentFact) {
    ComponentIdentifier revocationComponentIdentifier = autoPolicyWaiverRevocation.getComponentIdentifier();
    ComponentIdentifier componentFactComponentIdentifier = componentFact.getComponentIdentifier();

    if (revocationComponentIdentifier == null || componentFactComponentIdentifier == null) {
      return false;
    }

    try {
      revocationComponentIdentifier.ensureComplete();
      componentFactComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      return compareWhenMissingRequiredCoordinates(revocationComponentIdentifier, componentFactComponentIdentifier);
    }

    return revocationComponentIdentifier.compareTo(componentFactComponentIdentifier) == 0;
  }

  boolean compareWhenMissingRequiredCoordinates(
      ComponentIdentifier revocationIdentifier,
      ComponentIdentifier componentIdentifier)
  {
    return componentIdentifier.getCoordinates().entrySet().stream()
        .allMatch(compCoord -> compCoord.getValue().equals(revocationIdentifier.get(compCoord.getKey())));
  }

  private boolean isDifferentFormat(ComponentIdentifier compIdentif, ComponentIdentifier waiverIdentif) {
    return !waiverIdentif.getFormat().equals(compIdentif.getFormat());
  }

  private boolean matchesAllVersionsOfComponent(ComponentFact componentFact) {
    if (autoPolicyWaiverRevocation.getComponentIdentifier() == null ||
        componentFact.getComponentIdentifier() == null ||
        isDifferentFormat(componentFact.getComponentIdentifier(),
            autoPolicyWaiverRevocation.getComponentIdentifier())) {
      return false;
    }

    ComponentIdentifier waiverAllVersionsComponentIdentifier =
        autoPolicyWaiverRevocation.getComponentIdentifier().createAlternativeVersion("*");
    try {
      waiverAllVersionsComponentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      log.warn("Failed to ensureComplete for purl {} with the following error: {}",
          autoPolicyWaiverRevocation.getAssociatedPackageUrl(), e.getMessage());
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
}
