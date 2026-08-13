/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.callflow;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.MissingReachableComponentVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.PresentReachableComponentVulnerabilities;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createNpmCoordinates;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createNugetCoordinates;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createPecoffCoordinates;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createPypiCoordinates;
import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.filterOnReachabilitySupport;
import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.hasPolicyViolationByComponentIdentifier;
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
  public void testFilterOnReachabilitySupport_ReturnsOnlyReachableViolations() {
    // reachable violation by threat category and component identifier format (Maven)
    PolicyViolation mvnReachablePolicyViolation = createPolicyViolation();

    // non-reachable violation by threat category
    PolicyViolation unreachablePolicyViolation1 = new PolicyViolation();
    unreachablePolicyViolation1.setThreatCategory(LICENSE);
    unreachablePolicyViolation1.setComponentIdentifier(createMavenCoordinates("g", "a", "v"));
    unreachablePolicyViolation1.setOpenTime(new Date());

    // non-reachable violation by component format
    PolicyViolation pipyNonreachablePolicyViolation2 = new PolicyViolation();
    pipyNonreachablePolicyViolation2.setThreatCategory(SECURITY);
    pipyNonreachablePolicyViolation2.setComponentIdentifier(createPypiCoordinates("n", "v", "q", "e"));
    pipyNonreachablePolicyViolation2.setOpenTime(new Date());

    // reachable violation by threat category and component identifier format (NPM)
    PolicyViolation npmReachablePolicyViolation2 = new PolicyViolation();
    npmReachablePolicyViolation2.setThreatCategory(SECURITY);
    npmReachablePolicyViolation2.setComponentIdentifier(createNpmCoordinates("packageId", "version"));
    npmReachablePolicyViolation2.setOpenTime(new Date());

    // reachable violation by threat category and component identifier format (NuGet)
    PolicyViolation nugetReachablePolicyViolation = new PolicyViolation();
    nugetReachablePolicyViolation.setThreatCategory(SECURITY);
    nugetReachablePolicyViolation.setComponentIdentifier(createNugetCoordinates("packageId", "version"));
    nugetReachablePolicyViolation.setOpenTime(new Date());

    // reachable violation by threat category and component identifier format (pecoff)
    PolicyViolation pecoffReachablePolicyViolation = new PolicyViolation();
    pecoffReachablePolicyViolation.setThreatCategory(SECURITY);
    pecoffReachablePolicyViolation.setComponentIdentifier(createPecoffCoordinates("vendor", "product", "version"));
    pecoffReachablePolicyViolation.setOpenTime(new Date());

    List<PolicyViolation> result = filterOnReachabilitySupport(
        List.of(mvnReachablePolicyViolation, unreachablePolicyViolation1, pipyNonreachablePolicyViolation2,
            npmReachablePolicyViolation2, nugetReachablePolicyViolation, pecoffReachablePolicyViolation));

    assertThat(result).hasSize(4);
    assertThat(result.get(0)).isEqualTo(mvnReachablePolicyViolation);
    assertThat(result.get(1)).isEqualTo(npmReachablePolicyViolation2);
    assertThat(result.get(2)).isEqualTo(nugetReachablePolicyViolation);
    assertThat(result.get(3)).isEqualTo(pecoffReachablePolicyViolation);
  }

  @Test
  public void testFilterOnReachabilitySupport_ReturnsEmptyListWhenNoReachableViolations() {
    // non-reachable violation by threat category
    PolicyViolation unreachablePolicyViolation = new PolicyViolation();
    unreachablePolicyViolation.setThreatCategory(LICENSE);
    unreachablePolicyViolation.setComponentIdentifier(createMavenCoordinates("g", "a", "v"));

    assertThat(filterOnReachabilitySupport(List.of(unreachablePolicyViolation))).isEmpty();
  }

  @Test
  public void testUpdateReachabilityStatus_UpdatesStatusToReachable() {
    PolicyViolation policyViolation = createPolicyViolation();
    PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities =
        new PurlIdentifiersWithVulnerabilities(
            "applicationId",
            "scanId",
            Map.of(
                fromComponentIdentifier(policyViolation.getComponentIdentifier()),
                new PresentReachableComponentVulnerabilities(Set.of("CVE-1234"))));

    updateReachabilityStatus(
        policyViolation,
        purlIdentifiersWithVulnerabilities);

    assertThat(policyViolation.getReachabilityStatus()).isEqualTo(REACHABLE);
  }

  @Test
  public void testUpdateReachabilityStatus_UpdatesStatusToNonReachable() {
    PolicyViolation policyViolation = createPolicyViolation();
    PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities =
        new PurlIdentifiersWithVulnerabilities(
            "applicationId",
            "scanId",
            Map.of(
                fromComponentIdentifier(policyViolation.getComponentIdentifier()),
                new PresentReachableComponentVulnerabilities(new HashSet<>())));

    updateReachabilityStatus(
        policyViolation,
        purlIdentifiersWithVulnerabilities);

    assertThat(policyViolation.getReachabilityStatus()).isEqualTo(NON_REACHABLE);
  }

  @Test
  public void testUpdateReachabilityStatus_NoSignatures() {
    PolicyViolation policyViolation = createPolicyViolation();
    PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities =
        new PurlIdentifiersWithVulnerabilities(
            "applicationId",
            "scanId",
            Map.of(
                fromComponentIdentifier(policyViolation.getComponentIdentifier()),
                MissingReachableComponentVulnerabilities.INSTANCE));

    updateReachabilityStatus(
        policyViolation,
        purlIdentifiersWithVulnerabilities);

    assertThat(policyViolation.getReachabilityStatus()).isEqualTo(ReachabilityStatus.UNKNOWN);
  }

  @Test
  public void testUpdateReachabilityStatus_DoesNotCauseIssuesWithNullOrEmptyData() {
    // confirm no issues when providing null or empty data
    updateReachabilityStatus(null, Map.of());
    updateReachabilityStatus(new PolicyViolation(),
        (Map<PackageUrlIdentifier, ReachableComponentVulnerabilities>) null);
    updateReachabilityStatus(new PolicyViolation(), (PurlIdentifiersWithVulnerabilities) null);
    updateReachabilityStatus(new PolicyViolation(), new PurlIdentifiersWithVulnerabilities(null, null, null));
  }

  @Test
  public void testHasPolicyViolationByComponentIdentifier_ReturnsFalseWithNullOrEmptyData() {
    assertThat(hasPolicyViolationByComponentIdentifier(null, null)).isFalse();
    assertThat(hasPolicyViolationByComponentIdentifier(new PolicyViolation(), null)).isFalse();
    assertThat(hasPolicyViolationByComponentIdentifier(null, new PurlIdentifiersWithVulnerabilities(null, null, null)))
        .isFalse();
    assertThat(hasPolicyViolationByComponentIdentifier(new PolicyViolation(),
        new PurlIdentifiersWithVulnerabilities(null, null, null))).isFalse();
  }

  @Test
  public void testHasPolicyViolationByComponentIdentifier_ReturnsTrueWithPolicyViolation() {
    PolicyViolation reachablePolicyViolation = createPolicyViolation();

    PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities =
        new PurlIdentifiersWithVulnerabilities(
            "applicationId",
            "scanId",
            Map.of(
                fromComponentIdentifier(reachablePolicyViolation.getComponentIdentifier()),
                new PresentReachableComponentVulnerabilities(Set.of("CVE-1234"))));

    assertThat(hasPolicyViolationByComponentIdentifier(reachablePolicyViolation, purlIdentifiersWithVulnerabilities))
        .isTrue();
  }

  @Test
  public void testSupportsReachabilityAnalysis_ComponentIdentifier_PolicyThreatsViolation() {
    ComponentIdentifier maven = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    ComponentIdentifier swift = ComponentIdentifier.createSwiftCoordinates("n", "v");
    ComponentIdentifier nuget = ComponentIdentifier.createNugetCoordinates("p", "v");
    ComponentIdentifier pecoff = ComponentIdentifier.createPecoffCoordinates("vendor", "product", "v");

    PolicyThreats.PolicyViolation v1 = new PolicyThreats.PolicyViolation();
    v1.policyThreatCategory = SECURITY.toString();
    PolicyThreats.PolicyViolation v2 = new PolicyThreats.PolicyViolation();
    v2.policyThreatCategory = LICENSE.toString();
    PolicyThreats.PolicyViolation v3 = new PolicyThreats.PolicyViolation();

    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(maven, v1)).isTrue();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(maven, v2)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(swift, v1)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(nuget, v1)).isTrue();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(nuget, v2)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(pecoff, v1)).isTrue();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(pecoff, v2)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(maven, null)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(null, v1)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(maven, v3)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(null, null)).isFalse();
  }

  @Test
  public void testSupportsReachabilityAnalysis_PolicyViolation() {
    ComponentIdentifier maven = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    ComponentIdentifier swift = ComponentIdentifier.createSwiftCoordinates("n", "v");
    ComponentIdentifier nuget = ComponentIdentifier.createNugetCoordinates("p", "v");
    ComponentIdentifier pecoff = ComponentIdentifier.createPecoffCoordinates("vendor", "product", "v");

    PolicyViolation v1 = new PolicyViolation();
    v1.setComponentIdentifier(maven);
    v1.setThreatCategory(SECURITY);

    PolicyViolation v2 = new PolicyViolation();
    v2.setComponentIdentifier(maven);
    v2.setThreatCategory(LICENSE);

    PolicyViolation v3 = new PolicyViolation();
    v3.setComponentIdentifier(swift);
    v3.setThreatCategory(SECURITY);

    PolicyViolation v4 = new PolicyViolation();
    v4.setComponentIdentifier(maven);

    PolicyViolation v5 = new PolicyViolation();
    v5.setThreatCategory(SECURITY);

    PolicyViolation v6 = new PolicyViolation();

    PolicyViolation v7 = new PolicyViolation();
    v7.setComponentIdentifier(nuget);
    v7.setThreatCategory(SECURITY);

    PolicyViolation v8 = new PolicyViolation();
    v8.setComponentIdentifier(pecoff);
    v8.setThreatCategory(SECURITY);

    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(v1)).isTrue();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(v2)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(v3)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(v4)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(v5)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(v6)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(null)).isFalse();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(v7)).isTrue();
    assertThat(PolicyViolationReachabilityHelper.supportsReachabilityAnalysis(v8)).isTrue();
  }

  private PolicyViolation createPolicyViolation() {
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
    policyViolation.setOpenTime(new Date());
    return policyViolation;
  }
}
