/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyViolation;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import com.sonatype.insight.brain.policy.evaluator.PolicyResultsAccess;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuidePolicyComplianceMapperTest
{
  private static final String OWNER_ID = Organization.ROOT_ORGANIZATION_ID;

  private static final Stage STAGE = new Stage(Stage.ID_RELEASE);

  private final Component component = newComponent("pkg:maven/org.example/lib@1.0");

  @Test
  public void noViolations_returnsEmptyButPresent() {
    PolicyResults emptyResults = new PolicyResults();

    GuidePolicyCompliance compliance = GuidePolicyComplianceMapper.toCompliance(
        emptyResults, component, OWNER_ID, STAGE,
        Map.of(), Map.of());

    assertThat(compliance.compliant()).isTrue();
    assertThat(compliance.complianceLevel()).isEqualTo(GuidePolicyComplianceLevel.PASS);
    assertThat(compliance.stage()).isEqualTo("release");
    assertThat(compliance.ownerId()).isEqualTo(OWNER_ID);
    assertThat(compliance.violations()).isEmpty();
    assertThat(compliance.summary().highestThreatLevel()).isZero();
    assertThat(compliance.summary().worstAction()).isEqualTo("none");
    assertThat(compliance.summary().activeViolationCount()).isZero();
    assertThat(compliance.summary().waivedViolationCount()).isZero();
    assertThat(compliance.summary().violationCountsByCategory()).containsOnlyKeys(
        "SECURITY", "LICENSE", "QUALITY", "OTHER");
    assertThat(compliance.summary().violationCountsByCategory().values()).allMatch(v -> v == 0);
  }

  @Test
  public void activeViolationsRolledUp() {
    Policy securityPolicy = newPolicy("policy-1", "Security-Critical", PolicyThreatCategory.SECURITY);
    Policy qualityPolicy = newPolicy("policy-2", "Quality-Score", PolicyThreatCategory.QUALITY);
    Map<String, Policy> policiesById = Map.of(
        securityPolicy.getId(), securityPolicy,
        qualityPolicy.getId(), qualityPolicy);

    PolicyResults results = new PolicyResults();
    GuidePolicyResultsFactory.with(results)
        .activeAlert(component, "policy-1", "Security-Critical", 9, "fail",
            GuidePolicyResultsFactory.constraint("c-1", "Critical CVSS",
                GuidePolicyResultsFactory.reason("Found CVE-2021-44228",
                    TriggerReference.Type.SECURITY_VULNERABILITY_REFID, "CVE-2021-44228")))
        .activeAlert(component, "policy-2", "Quality-Score", 5, "warn",
            GuidePolicyResultsFactory.constraint("c-2", "Score < 50",
                GuidePolicyResultsFactory.reason("Score 30", null, null)))
        .build();

    GuidePolicyCompliance compliance = GuidePolicyComplianceMapper.toCompliance(
        results, component, OWNER_ID, STAGE, policiesById, Map.of());

    assertThat(compliance.compliant()).isFalse();
    assertThat(compliance.complianceLevel()).isEqualTo(GuidePolicyComplianceLevel.FAIL);
    assertThat(compliance.summary().highestThreatLevel()).isEqualTo(9);
    assertThat(compliance.summary().worstAction()).isEqualTo("fail");
    assertThat(compliance.summary().activeViolationCount()).isEqualTo(2);
    assertThat(compliance.summary().waivedViolationCount()).isZero();
    assertThat(compliance.summary().violationCountsByCategory().get("SECURITY")).isEqualTo(1);
    assertThat(compliance.summary().violationCountsByCategory().get("QUALITY")).isEqualTo(1);
    assertThat(compliance.summary().violationCountsByCategory().get("LICENSE")).isZero();
    assertThat(compliance.summary().violationCountsByCategory().get("OTHER")).isZero();

    assertThat(compliance.violations()).hasSize(2);
    assertThat(compliance.violations()).extracting(GuidePolicyViolation::policyName)
        .containsExactlyInAnyOrder("Security-Critical", "Quality-Score");
    assertThat(compliance.violations()).extracting(GuidePolicyViolation::actions)
        .containsExactlyInAnyOrder(List.of("fail"), List.of("warn"));
    GuidePolicyViolation security = compliance.violations()
        .stream()
        .filter(v -> "Security-Critical".equals(v.policyName()))
        .findFirst()
        .orElseThrow();
    assertThat(security.threatLevel()).isEqualTo(9);
    assertThat(security.waived()).isFalse();
    assertThat(security.waiver()).isNull();
    assertThat(security.constraintViolations()).hasSize(1);
    assertThat(security.constraintViolations().get(0).constraintName()).isEqualTo("Critical CVSS");
    assertThat(security.constraintViolations().get(0).reasons().get(0).reason())
        .isEqualTo("Found CVE-2021-44228");
    assertThat(security.constraintViolations().get(0).reasons().get(0).reference().type())
        .isEqualTo("SECURITY_VULNERABILITY_REFID");
    assertThat(security.constraintViolations().get(0).reasons().get(0).reference().value())
        .isEqualTo("CVE-2021-44228");
  }

  @Test
  public void activeWarnOnly_isCompliantTrueWithWarnLevel() {
    Policy qualityPolicy = newPolicy("p", "Quality-Score", PolicyThreatCategory.QUALITY);
    Map<String, Policy> policiesById = Map.of("p", qualityPolicy);

    PolicyResults results = new PolicyResults();
    GuidePolicyResultsFactory.with(results)
        .activeAlert(component, "p", "Quality-Score", 3, "warn",
            GuidePolicyResultsFactory.constraint("c", "Score < 50",
                GuidePolicyResultsFactory.reason("Score 30", null, null)))
        .build();

    GuidePolicyCompliance compliance = GuidePolicyComplianceMapper.toCompliance(
        results, component, OWNER_ID, STAGE, policiesById, Map.of());

    // An active warn-action violation is compliant=true (the "check") but amber WARN — the shift
    // from the old boolean, where any active violation counted as non-compliant.
    assertThat(compliance.compliant()).isTrue();
    assertThat(compliance.complianceLevel()).isEqualTo(GuidePolicyComplianceLevel.WARN);
    assertThat(compliance.summary().worstAction()).isEqualTo("warn");
    assertThat(compliance.summary().activeViolationCount()).isEqualTo(1);
  }

  @Test
  public void waivedAlert_attachesWaiverInfo_andDoesNotMakeNonCompliant() {
    Policy policy = newPolicy("p", "License-Copyleft", PolicyThreatCategory.LICENSE);
    Map<String, Policy> policiesById = Map.of("p", policy);

    // PolicyWaiver only carries ownerId (no scope-type accessor); the wire shape's
    // scopeOwnerType comes from the ownerTypeByOwnerId lookup map (see mapper).
    // setExpiryTime takes Date, NOT Instant — convert.
    PolicyWaiver waiver = new PolicyWaiver();
    waiver.setOwnerId(OWNER_ID);
    waiver.setExpiryTime(Date.from(Instant.parse("2026-12-01T00:00:00Z")));
    waiver.setComment("Approved by legal");

    PolicyResults results = new PolicyResults();
    GuidePolicyResultsFactory factory = GuidePolicyResultsFactory.with(results);
    factory.waivedAlert(component, "p", "License-Copyleft", 5, "warn",
        GuidePolicyResultsFactory.constraint("c", "Copyleft License",
            GuidePolicyResultsFactory.reason("GPL-3.0", null, null)));
    PolicyAlert alert = factory.lastAlert();
    PolicyResultsAccess.addPolicyWaiver(results, alert.getTrigger().getComponentFacts().getFirst(), waiver);

    GuidePolicyCompliance compliance = GuidePolicyComplianceMapper.toCompliance(
        results, component, OWNER_ID, STAGE, policiesById,
        Map.of(OWNER_ID, OwnerType.ORGANIZATION));

    assertThat(compliance.compliant()).isTrue();
    // Fully-waived → amber WARN (compliant, but worth attention), not green PASS.
    assertThat(compliance.complianceLevel()).isEqualTo(GuidePolicyComplianceLevel.WARN);
    assertThat(compliance.summary().activeViolationCount()).isZero();
    assertThat(compliance.summary().waivedViolationCount()).isEqualTo(1);
    assertThat(compliance.violations()).hasSize(1);

    GuidePolicyViolation v = compliance.violations().get(0);
    assertThat(v.waived()).isTrue();
    assertThat(v.waiver()).isNotNull();
    assertThat(v.waiver().scopeOwnerType()).isEqualTo("organization");
    assertThat(v.waiver().comment()).isEqualTo("Approved by legal");
  }

  @Test
  public void waivedAlert_withMissingWaiverLookup_stillReportsWaived() {
    Policy policy = newPolicy("p", "License-Copyleft", PolicyThreatCategory.LICENSE);
    Map<String, Policy> policiesById = Map.of("p", policy);

    // Waived alert whose waiver is absent from the identity-keyed waiver map (getPolicyWaiver
    // returns null). The waived-alerts loop is still the authoritative "waived" signal, so the
    // wire shape must report waived=true with a null waiver detail — not waived=false.
    PolicyResults results = new PolicyResults();
    GuidePolicyResultsFactory.with(results)
        .waivedAlert(component, "p", "License-Copyleft", 5, "warn",
            GuidePolicyResultsFactory.constraint("c", "Copyleft License",
                GuidePolicyResultsFactory.reason("GPL-3.0", null, null)))
        .build();

    GuidePolicyCompliance compliance = GuidePolicyComplianceMapper.toCompliance(
        results, component, OWNER_ID, STAGE, policiesById, Map.of());

    assertThat(compliance.compliant()).isTrue();
    // Fully-waived → amber WARN (compliant, but worth attention), not green PASS.
    assertThat(compliance.complianceLevel()).isEqualTo(GuidePolicyComplianceLevel.WARN);
    assertThat(compliance.summary().activeViolationCount()).isZero();
    assertThat(compliance.summary().waivedViolationCount()).isEqualTo(1);
    assertThat(compliance.violations()).hasSize(1);

    GuidePolicyViolation v = compliance.violations().get(0);
    assertThat(v.waived()).isTrue();
    assertThat(v.waiver()).isNull();
  }

  private static Component newComponent(String purl) {
    Component c = new Component();
    TreeMap<String, String> coords = new TreeMap<>();
    coords.put("artifactId", "lib");
    coords.put("version", "1.0");
    c.setComponentIdentifier(new ComponentIdentifier("maven", coords));
    return c;
  }

  private static Policy newPolicy(String id, String name, PolicyThreatCategory category) {
    return new Policy()
    {
      @Override
      public String getId() {
        return id;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public PolicyThreatCategory getThreatCategory() {
        return category;
      }
    };
  }
}
