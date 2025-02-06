/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.callflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.NON_REACHABLE;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.REACHABLE;
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
   * Filters a {@link List} of {@link PolicyViolation}s to only include the reachable security violations.
   *
   * @param policyViolations - the list of {@link PolicyViolation} to filter
   * @return the list of reachable security violations
   */
  public static List<PolicyViolation> filterOnReachableSecurityViolations(
      final List<PolicyViolation> policyViolations)
  {
    return policyViolations.stream().filter(PolicyViolationReachabilityHelper::isReachableSecurityViolation).toList();
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
   * violation is not a reachable security violation, then it will not be updated.
   *
   * @param policyViolation - the {@link PolicyEvaluation} to update
   * @param purlIdentifiers - {@link Map} of {@link PackageUrlIdentifier} mapped to a set of vulnerabilities to map the
   *                        reachability status of the policy violation.
   */
  public static void updateReachabilityStatus(
      final PolicyViolation policyViolation,
      final Map<PackageUrlIdentifier, Set<String>> purlIdentifiers)
  {
    if (policyViolation == null || purlIdentifiers == null) {
      return;
    }

    logger.debug("Updating policy violation with reachability data for applicationId: {}, policyId: {}",
        policyViolation.getApplicationId(), policyViolation.getId());

    if (isReachableSecurityViolation(policyViolation)) {
      boolean isReachable = isVulnerabilityReachable(policyViolation, purlIdentifiers);
      policyViolation.setReachabilityStatus(isReachable ? REACHABLE : NON_REACHABLE);
    }

    logger.debug("Finished updating policy violations with reachability data for applicationId: {}, policyId: {}",
        policyViolation.getApplicationId(), policyViolation.getId());
  }

  private static boolean isVulnerabilityReachable(
      final PolicyViolation policyViolation,
      final Map<PackageUrlIdentifier, Set<String>> purlIdentifiers)
  {
    return policyViolation.getConstraintFacts().stream()
        .flatMap(constraintFact -> constraintFact.getConditionFacts().stream())
        .map(ConditionFact::getReference)
        .filter(Objects::nonNull)
        .filter(triggerReference -> triggerReference.getType().equals(SECURITY_VULNERABILITY_REFID))
        .map(TriggerReference::getValue)
        .filter(value -> value != null && !value.isEmpty())
        .anyMatch(vulnerabilityId -> isVulnerabilityReachable(vulnerabilityId, policyViolation, purlIdentifiers));
  }

  private static boolean isVulnerabilityReachable(
      final String vulnerabilityId,
      final PolicyViolation policyViolation,
      final Map<PackageUrlIdentifier, Set<String>> vulnerabilitiesByPurlIdentifiers)
  {
    Set<String> vulnerabilities = vulnerabilitiesByPurlIdentifiers.get(
        fromComponentIdentifier(policyViolation.getComponentIdentifier())
    );

    if (vulnerabilities == null) {
      return false;
    }

    return vulnerabilities.stream()
        .map(String::toLowerCase)
        .collect(Collectors.toSet())
        .contains(vulnerabilityId.toLowerCase());
  }

  private static boolean isReachableSecurityViolation(final PolicyViolation policyViolation) {
    // only allow if it is a maven security violation, as it's the only type of violation that has
    // reachability support. More types of security violations can be added here as permitted.
    return isMavenSecurityViolation(policyViolation);
  }

  private static boolean isMavenSecurityViolation(final PolicyViolation policyViolation) {
    return SECURITY.equals(policyViolation.getThreatCategory()) && policyViolation.getComponentIdentifier().isMaven();
  }
}
