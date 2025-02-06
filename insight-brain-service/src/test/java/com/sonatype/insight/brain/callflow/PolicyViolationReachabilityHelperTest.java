/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.callflow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createNpmCoordinates;
import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.filterOnReachableSecurityViolations;
import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.updateReachabilityStatus;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.NON_REACHABLE;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.REACHABLE;
import static com.sonatype.insight.purl.PackageUrlIdentifier.fromComponentIdentifier;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationReachabilityHelperTest
{
  @Test
  public void filterOnReachableSecurityViolations_ReturnsOnlyReachableViolations() {
    // reachable violation by threat category and component identifier format
    PolicyViolation reachablePolicyViolation = createReachablePolicyViolation();

    // non-reachable violation by threat category
    PolicyViolation unreachablePolicyViolation1 = new PolicyViolation();
    unreachablePolicyViolation1.setThreatCategory(LICENSE);
    unreachablePolicyViolation1.setComponentIdentifier(createMavenCoordinates("g", "a", "v"));

    // non-reachable violation by component identifier format
    PolicyViolation unreachablePolicyViolation2 = new PolicyViolation();
    unreachablePolicyViolation2.setThreatCategory(SECURITY);
    unreachablePolicyViolation2.setComponentIdentifier(createNpmCoordinates("packageId", "version"));

    List<PolicyViolation> result = filterOnReachableSecurityViolations(
        List.of(reachablePolicyViolation, unreachablePolicyViolation1, unreachablePolicyViolation2)
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(reachablePolicyViolation);
  }

  @Test
  public void filterOnReachableSecurityViolations_ReturnsEmptyListWhenNoReachableViolations() {
    // non-reachable violation by threat category
    PolicyViolation unreachablePolicyViolation = new PolicyViolation();
    unreachablePolicyViolation.setThreatCategory(LICENSE);
    unreachablePolicyViolation.setComponentIdentifier(createMavenCoordinates("g", "a", "v"));

    assertThat(filterOnReachableSecurityViolations(List.of(unreachablePolicyViolation))).isEmpty();
  }

  @Test
  public void updateReachabilityStatus_UpdatesStatusToReachable() {
    // reachable violation by threat category, component identifier format, and constraint fact
    PolicyViolation reachablePolicyViolation = createReachablePolicyViolation();

    assertThat(reachablePolicyViolation.getReachabilityStatus()).isNull();

    updateReachabilityStatus(
        reachablePolicyViolation,
        createVulnerabilitiesByPurlIdentifiers(reachablePolicyViolation)
    );

    assertThat(reachablePolicyViolation.getReachabilityStatus()).isEqualTo(REACHABLE);
  }

  @Test
  public void updateReachabilityStatusWithPurlIdentifiersWithVulnerabilities_UpdatesStatusToReachable() {
    // reachable violation by threat category, component identifier format, and constraint fact
    PolicyViolation reachablePolicyViolation = createReachablePolicyViolation();

    assertThat(reachablePolicyViolation.getReachabilityStatus()).isNull();

    PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities =
        new PurlIdentifiersWithVulnerabilities(
            "applicationId",
            "scanId",
            createVulnerabilitiesByPurlIdentifiers(reachablePolicyViolation)
        );

    updateReachabilityStatus(
        reachablePolicyViolation,
        purlIdentifiersWithVulnerabilities
    );

    assertThat(reachablePolicyViolation.getReachabilityStatus()).isEqualTo(REACHABLE);
  }

  @Test
  public void updateReachabilityStatus_UpdatesStatusToNonReachable() {
    // unreachable violation by missing proper constraint fact
    PolicyViolation unreachablePolicyViolation = new PolicyViolation();
    unreachablePolicyViolation.setThreatCategory(SECURITY);
    unreachablePolicyViolation.setComponentIdentifier(createMavenCoordinates("g", "a", "v"));

    ConstraintFact constraintFact = new ConstraintFact();
    unreachablePolicyViolation.setConstraintFacts(List.of(
        constraintFact
    ));

    assertThat(unreachablePolicyViolation.getReachabilityStatus()).isNull();

    updateReachabilityStatus(
        unreachablePolicyViolation,
        createVulnerabilitiesByPurlIdentifiers(unreachablePolicyViolation)
    );

    assertThat(unreachablePolicyViolation.getReachabilityStatus()).isEqualTo(NON_REACHABLE);
  }

  @Test
  public void updateReachabilityStatus_DoesNotCauseIssuesWithNullOrEmptyData() {
    // confirm no issues when providing null or empty data
    updateReachabilityStatus(null, Map.of());
    updateReachabilityStatus(new PolicyViolation(), (Map<PackageUrlIdentifier, Set<String>>) null);
    updateReachabilityStatus(new PolicyViolation(), (PurlIdentifiersWithVulnerabilities) null);
    updateReachabilityStatus(new PolicyViolation(), new PurlIdentifiersWithVulnerabilities(null, null, null));
  }

  private PolicyViolation createReachablePolicyViolation() {
    TriggerReference triggerReference = new TriggerReference();
    triggerReference.setType(SECURITY_VULNERABILITY_REFID);
    triggerReference.setValue("CVE-1234");

    ConditionFact conditionFact = new ConditionFact();
    conditionFact.setReference(triggerReference);

    ConstraintFact constraintFact = new ConstraintFact();
    constraintFact.setConditionFacts(List.of(conditionFact));

    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setThreatCategory(SECURITY);
    policyViolation.setComponentIdentifier(createMavenCoordinates("g", "a", "v"));
    policyViolation.setConstraintFacts(List.of(constraintFact));
    return policyViolation;
  }

  private Map<PackageUrlIdentifier, Set<String>> createVulnerabilitiesByPurlIdentifiers(
      final PolicyViolation policyViolation)
  {
    return Map.of(
        fromComponentIdentifier(policyViolation.getComponentIdentifier()), Set.of("CVE-1234")
    );
  }
}
