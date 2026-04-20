/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.callflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.MissingReachableComponentVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.PresentReachableComponentVulnerabilities;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_NPM;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_NUGET;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PECOFF;
import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.UNKNOWN;
import static com.sonatype.insight.purl.PackageUrlIdentifier.fromComponentIdentifier;

/**
 * Helper class to update the reachability status of {@link PolicyViolation} based on the vulnerabilities identified.
 */
public class PolicyViolationReachabilityHelper
{
  private static final Logger logger = LoggerFactory.getLogger(PolicyViolationReachabilityHelper.class);

  private PolicyViolationReachabilityHelper() {
  }

  /**
   * Filters a {@link List} of {@link PolicyViolation}s to only include violations that support reachability analysis.
   *
   * @param policyViolations - the list of {@link PolicyViolation} to filter
   * @return the list of {@link PolicyViolation} that support reachability analysis
   */
  public static List<PolicyViolation> filterOnReachabilitySupport(final List<PolicyViolation> policyViolations) {
    return policyViolations.stream()
        .filter(PolicyViolationReachabilityHelper::supportsReachabilityAnalysis)
        .toList();
  }

  /**
   * @see #updateReachabilityStatus(PolicyViolation, Map)
   */
  public static void updateReachabilityStatus(
      final PolicyViolation policyViolation,
      final PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities)
  {
    if (policyViolation == null || purlIdentifiersWithVulnerabilities == null) {
      return;
    }

    updateReachabilityStatus(policyViolation, purlIdentifiersWithVulnerabilities.getVulnerabilitiesByPurlIdentifiers());
  }

  /**
   * Updates the reachability status of the policy violation based on the vulnerabilities identified. If the policy
   * violation is not a reachability-supporting security violation, then it will not be updated.
   *
   * @param policyViolation - the {@link PolicyEvaluation} to update
   * @param purlIdentifiers - {@link Map} of {@link PackageUrlIdentifier} mapped to a set of vulnerabilities to map the
   *          reachability status of the policy violation.
   */
  public static void updateReachabilityStatus(
      final PolicyViolation policyViolation,
      final Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> purlIdentifiers)
  {
    if (policyViolation == null || purlIdentifiers == null) {
      return;
    }

    logger.debug("Updating policy violation with reachability data for applicationId: {}, policyId: {}",
        policyViolation.getApplicationId(), policyViolation.getId());

    if (supportsReachabilityAnalysis(policyViolation)) {
      ReachabilityStatus isReachable = isVulnerabilityReachable(policyViolation, purlIdentifiers);
      policyViolation.setReachabilityStatus(isReachable);
    }

    logger.debug("Finished updating policy violations with reachability data for applicationId: {}, policyId: {}",
        policyViolation.getApplicationId(), policyViolation.getId());
  }

  /**
   * Helper method to see whether the {@link PolicyViolation} was collected within
   * {@link PurlIdentifiersWithVulnerabilities}
   *
   * @param policyViolation - the {@link PolicyViolation} to find
   * @param purlIdentifiersWithVulnerabilities - the {@link PurlIdentifiersWithVulnerabilities} to check
   * @return true if the {@link PolicyViolation} was found in the {@link PurlIdentifiersWithVulnerabilities}
   */
  public static boolean hasPolicyViolationByComponentIdentifier(
      final PolicyViolation policyViolation,
      final PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities)
  {
    return purlIdentifiersWithVulnerabilities != null &&
        policyViolation != null &&
        policyViolation.getComponentIdentifier() != null &&
        purlIdentifiersWithVulnerabilities
            .getVulnerabilitiesByPurlIdentifiers()
            .containsKey(fromComponentIdentifier(policyViolation.getComponentIdentifier()));
  }

  private static ReachabilityStatus isVulnerabilityReachable(
      final PolicyViolation policyViolation,
      final Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> purlIdentifiers)
  {
    return ReachabilityStatus.combine(policyViolation.getConstraintFacts()
        .stream()
        .flatMap(constraintFact -> constraintFact.getConditionFacts().stream())
        .map(ConditionFact::getReference)
        .filter(Objects::nonNull)
        .filter(triggerReference -> triggerReference.getType().equals(SECURITY_VULNERABILITY_REFID))
        .map(TriggerReference::getValue)
        .filter(value -> value != null && !value.isEmpty())
        .map(vulnerabilityId -> isVulnerabilityReachable(vulnerabilityId, policyViolation, purlIdentifiers)));
  }

  /**
   * @param vulnerabilitiesByPurlIdentifiers a map from purl to {@link ReachableComponentVulnerabilities}.
   * @return the {@link ReachabilityStatus}.
   */
  private static ReachabilityStatus isVulnerabilityReachable(
      final String vulnerabilityId,
      final PolicyViolation policyViolation,
      final Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> vulnerabilitiesByPurlIdentifiers)
  {
    ReachableComponentVulnerabilities reachableSignatures = vulnerabilitiesByPurlIdentifiers.get(
        fromComponentIdentifier(policyViolation.getComponentIdentifier()));

    if (reachableSignatures == null || reachableSignatures instanceof MissingReachableComponentVulnerabilities) {
      return UNKNOWN;
    }

    return ReachabilityStatus.fromBoolean(((PresentReachableComponentVulnerabilities) reachableSignatures)
        .references()
        .stream()
        .map(String::toLowerCase)
        .anyMatch(vulnerabilityId.toLowerCase()::equals));
  }

  public static boolean supportsReachabilityAnalysis(
      final ComponentIdentifier componentIdentifier,
      final PolicyThreats.PolicyViolation policyViolation)
  {
    if (componentIdentifier == null || policyViolation == null) {
      return false;
    }
    return supportsReachabilityAnalysis(policyViolation.policyThreatCategory, componentIdentifier.getFormat());
  }

  public static boolean supportsReachabilityAnalysis(final PolicyViolation policyViolation) {
    if (policyViolation == null || policyViolation.getComponentIdentifier() == null ||
        policyViolation.getThreatCategory() == null)
    {
      return false;
    }
    return supportsReachabilityAnalysis(policyViolation.getThreatCategory().toString(),
        policyViolation.getComponentIdentifier().getFormat());
  }

  private static boolean supportsReachabilityAnalysis(final String policyThreatCategory, final String format) {
    return isSecurityViolation(policyThreatCategory) && isSupportedFormat(format);
  }

  private static boolean isSecurityViolation(final String policyThreatCategory) {
    return SECURITY.getName().equalsIgnoreCase(policyThreatCategory);
  }

  private static boolean isSupportedFormat(final String format) {
    return FORMAT_MAVEN.equalsIgnoreCase(format) || FORMAT_NPM.equalsIgnoreCase(format) ||
        FORMAT_NUGET.equalsIgnoreCase(format) || FORMAT_PECOFF.equalsIgnoreCase(format);
  }
}
