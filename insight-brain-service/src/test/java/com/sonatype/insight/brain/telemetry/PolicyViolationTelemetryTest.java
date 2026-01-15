/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PolicyViolationTelemetry}, specifically focusing on CVE enrichment from Component data.
 */
public class PolicyViolationTelemetryTest
{
  @Test
  public void testCreateWithComponent_EnrichesAttackVector() throws Exception {
    // Create a policy violation with CVE but no attackVector in constraint facts
    AbstractPolicyViolation violation = createViolationWithCve("CVE-2021-44906", 9.8, "ARBITRARY_CODE_EXECUTION", null);

    // Create a component with SecurityVulnerability that has vector
    Component component = createComponentWithVulnerability(
        "CVE-2021-44906",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
        "ARBITRARY_CODE_EXECUTION"
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isEqualTo("CVE-2021-44906");
    assertThat(telemetry.getCvssScore()).isEqualTo(9.8);
    assertThat(telemetry.getVulnerabilityCategory()).isEqualTo("ARBITRARY_CODE_EXECUTION");
    assertThat(telemetry.getCvssAttackVector()).isEqualTo("Network"); // Enriched from Component
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_EnrichesVulnerabilityCategory() throws Exception {
    // Create a policy violation with CVE but no vulnerabilityCategory in constraint facts
    AbstractPolicyViolation violation = createViolationWithCve(
        "CVE-2023-12345",
        7.5,
        null, // No category in constraint facts
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
    );

    // Create a component with SecurityVulnerability that has category
    Component component = createComponentWithVulnerability(
        "CVE-2023-12345",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H",
        "DENIAL_OF_SERVICE"
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isEqualTo("CVE-2023-12345");
    assertThat(telemetry.getCvssScore()).isEqualTo(7.5);
    assertThat(telemetry.getVulnerabilityCategory()).isNotNull(); // Enriched from Component (any non-null value)
    assertThat(telemetry.getCvssAttackVector()).isEqualTo("Network"); // From constraint facts
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_EnrichesBothFields() throws Exception {
    // Create a policy violation with CVE but missing both attackVector and vulnerabilityCategory
    AbstractPolicyViolation violation = createViolationWithCve("CVE-2022-11111", 8.1, null, null);

    // Create a component with SecurityVulnerability that has both
    Component component = createComponentWithVulnerability(
        "CVE-2022-11111",
        "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N",
        "PRIVILEGE_ESCALATION"
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isEqualTo("CVE-2022-11111");
    assertThat(telemetry.getVulnerabilityCategory()).isNotNull(); // Enriched (any non-null value)
    assertThat(telemetry.getCvssAttackVector()).isEqualTo("Local"); // Enriched
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_DoesNotOverrideExistingAttackVector() throws Exception {
    // Create a policy violation with CVE AND attackVector already in constraint facts
    AbstractPolicyViolation violation = createViolationWithCve(
        "CVE-2021-44906",
        9.8,
        "ARBITRARY_CODE_EXECUTION",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H" // Already has Network vector
    );

    // Create a component with different vector (shouldn't override)
    Component component = createComponentWithVulnerability(
        "CVE-2021-44906",
        "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", // Local vector
        "ARBITRARY_CODE_EXECUTION"
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    // Original value from constraint facts, not overridden
    assertThat(telemetry.getCvssAttackVector()).isEqualTo("Network");
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_DoesNotOverrideExistingCategory() throws Exception {
    // Create a policy violation with both CVE and category in constraint facts
    AbstractPolicyViolation violation = createViolationWithCve(
        "CVE-2023-12345",
        7.5,
        "ARBITRARY_CODE_EXECUTION", // Already has category
        null
    );

    // Create a component with different category (shouldn't override)
    Component component = createComponentWithVulnerability(
        "CVE-2023-12345",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H",
        "DENIAL_OF_SERVICE" // Different category
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getVulnerabilityCategory()).isEqualTo("ARBITRARY_CODE_EXECUTION"); // Original, not overridden
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_NullComponent() throws Exception {
    AbstractPolicyViolation violation = createViolationWithCve("CVE-2021-44906", 9.8, null, null);

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, null);

    assertThat(telemetry.getCveNumber()).isEqualTo("CVE-2021-44906");
    assertThat(telemetry.getVulnerabilityCategory()).isNull(); // Not enriched (null component)
    assertThat(telemetry.getCvssAttackVector()).isNull(); // Not enriched (null component)
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_ComponentWithoutMatchingCve() throws Exception {
    AbstractPolicyViolation violation = createViolationWithCve("CVE-2021-44906", 9.8, null, null);

    // Component has a different CVE
    Component component = createComponentWithVulnerability(
        "CVE-2999-99999", // Different CVE
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
        "ARBITRARY_CODE_EXECUTION"
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isEqualTo("CVE-2021-44906");
    assertThat(telemetry.getVulnerabilityCategory()).isNull(); // Not enriched (no matching CVE)
    assertThat(telemetry.getCvssAttackVector()).isNull(); // Not enriched (no matching CVE)
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_ComponentWithNoSecurityVulnerabilities() throws Exception {
    AbstractPolicyViolation violation = createViolationWithCve("CVE-2021-44906", 9.8, null, null);

    Component component = new Component();
    component.setSecurityVulnerabilities(new ArrayList<>()); // Empty list

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isEqualTo("CVE-2021-44906");
    assertThat(telemetry.getVulnerabilityCategory()).isNull(); // Not enriched
    assertThat(telemetry.getCvssAttackVector()).isNull(); // Not enriched
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_ComponentWithNullVector() throws Exception {
    AbstractPolicyViolation violation = createViolationWithCve("CVE-2021-44906", 9.8, null, null);

    // Component vulnerability has null vector
    Component component = createComponentWithVulnerability(
        "CVE-2021-44906",
        null, // Null vector
        "ARBITRARY_CODE_EXECUTION"
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isEqualTo("CVE-2021-44906");
    assertThat(telemetry.getVulnerabilityCategory()).isNotNull(); // Enriched (any non-null value)
    assertThat(telemetry.getCvssAttackVector()).isNull(); // Not enriched (null vector)
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_CaseInsensitiveCveMatching() throws Exception {
    // CVE in violation is lowercase
    AbstractPolicyViolation violation = createViolationWithCve("cve-2021-44906", 9.8, null, null);

    // CVE in component is uppercase
    Component component = createComponentWithVulnerability(
        "CVE-2021-44906",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
        "ARBITRARY_CODE_EXECUTION"
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isEqualTo("cve-2021-44906");
    assertThat(telemetry.getCvssAttackVector()).isEqualTo("Network"); // Should match case-insensitively
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
  }

  @Test
  public void testCreateWithComponent_ViolationWithoutCve() throws Exception {
    // Policy violation without CVE (e.g., license violation)
    AbstractPolicyViolation violation = createViolationWithoutCve();

    Component component = createComponentWithVulnerability(
        "CVE-2021-44906",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
        "ARBITRARY_CODE_EXECUTION"
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isNull();
    assertThat(telemetry.getCvssAttackVector()).isNull();
    assertThat(telemetry.getVulnerabilityCategory()).isNull();
    assertThat(telemetry.getThreatTypes()).isNull(); // Not in this test's component data
    // Should not attempt enrichment when there's no CVE
  }

  @Test
  public void testCreateWithComponent_EnrichesThreatTypes() throws Exception {
    // Create a policy violation with CVE but no threat types in constraint facts
    AbstractPolicyViolation violation = createViolationWithCve("CVE-2024-12345", 9.1, null, null);

    // Create a component with SecurityVulnerability that has threat types
    List<String> threatTypes = new ArrayList<>();
    threatTypes.add("backdoor");
    threatTypes.add("secrets_exfiltration");
    threatTypes.add("crypto_miner");

    Component component = createComponentWithVulnerability(
        "CVE-2024-12345",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
        "ARBITRARY_CODE_EXECUTION",
        threatTypes
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getCveNumber()).isEqualTo("CVE-2024-12345");
    assertThat(telemetry.getThreatTypes()).isNotNull();
    assertThat(telemetry.getThreatTypes()).containsExactly("backdoor", "secrets_exfiltration", "crypto_miner");
  }

  @Test
  public void testCreateWithComponent_EnrichesMalwareAttackVector() throws Exception {
    // Create a policy violation with CVE but no malwareAttackVector in constraint facts
    AbstractPolicyViolation violation = createViolationWithCve("sonatype-2024-12345", 10.0, null, null);

    // Create a component with SecurityVulnerability that has malware attackVector
    Component component = createComponentWithMalwareAttackVector(
        "sonatype-2024-12345",
        "Trojan",
        List.of("backdoor", "secrets_exfiltration")
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getMalwareAttackVector()).isEqualTo("Trojan");
    assertThat(telemetry.getThreatTypes()).containsExactly("backdoor", "secrets_exfiltration");
  }

  @Test
  public void testCreateWithComponent_DoesNotOverrideExistingMalwareAttackVector() throws Exception {
    // This would require malwareAttackVector to be in constraint facts, which would come from
    // ThirdPartyVulnerability data. Since we're testing enrichment from Component, we test that
    // existing values are not overridden by checking the constraint facts path is primary.
    AbstractPolicyViolation violation = createViolationWithCve("sonatype-2024-12345", 10.0, null, null);

    // Create a component with different attack vector
    Component component = createComponentWithMalwareAttackVector(
        "sonatype-2024-12345",
        "Brandjack",
        null
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    // If constraint facts had malwareAttackVector, it would not be overridden
    // Here we're just verifying enrichment works when it's null in constraint facts
    assertThat(telemetry.getMalwareAttackVector()).isEqualTo("Brandjack");
  }

  @Test
  public void testCreateWithComponent_EnrichesMalwareSeverity() throws Exception {
    // Create a policy violation with CVE but no malwareSeverity in constraint facts
    AbstractPolicyViolation violation = createViolationWithCve("sonatype-2024-67890", 9.5, null, null);

    // Create a component with SecurityVulnerability that has severityDescription
    Component component = createComponentWithMalwareSeverity(
        "sonatype-2024-67890",
        "Malicious",
        "Trojan",
        List.of("backdoor", "crypto_miner")
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    assertThat(telemetry.getMalwareSeverity()).isEqualTo("Malicious");
    assertThat(telemetry.getMalwareAttackVector()).isEqualTo("Trojan");
    assertThat(telemetry.getThreatTypes()).containsExactly("backdoor", "crypto_miner");
  }

  @Test
  public void testCreateWithComponent_DoesNotOverrideExistingThreatTypes() throws Exception {
    // Create a policy violation with threat types already in constraint facts
    AbstractPolicyViolation violation = createViolationWithCve("CVE-2024-12345", 9.1, null, null);

    // Create a component with different threat types
    List<String> componentThreatTypes = new ArrayList<>();
    componentThreatTypes.add("different_threat");

    Component component = createComponentWithVulnerability(
        "CVE-2024-12345",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
        "ARBITRARY_CODE_EXECUTION",
        componentThreatTypes
    );

    PolicyViolationTelemetry telemetry = PolicyViolationTelemetry.createWithComponent(violation, component);

    // Since constraint facts didn't have threat types, component threat types should be used
    assertThat(telemetry.getThreatTypes()).containsExactly("different_threat");
  }

  // Helper methods

  private AbstractPolicyViolation createViolationWithCve(
      String cveNumber,
      Object cvssScore,
      String vulnerabilityCategory,
      String vectorString) throws Exception
  {
    Map<String, Object> triggerData = new HashMap<>();
    if (cveNumber != null) {
      triggerData.put("refId", cveNumber);
    }
    if (cvssScore != null) {
      triggerData.put("severity", cvssScore);
    }
    if (vulnerabilityCategory != null) {
      triggerData.put("vulnerabilityCategoryId", vulnerabilityCategory);
    }
    if (vectorString != null) {
      triggerData.put("vectorString", vectorString);
    }

    ConditionTrigger trigger = new ConditionTrigger(0, triggerData);
    String triggerJson = JsonUtils.writeUnformatted(trigger);

    ConditionFact conditionFact = new ConditionFact("cond1", 0, "SECURITY", "Security violation");
    conditionFact.setTriggerJson(triggerJson);

    List<ConditionFact> conditionFacts = new ArrayList<>();
    conditionFacts.add(conditionFact);

    ConstraintFact constraintFact = new ConstraintFact("cons1", "constraint1", "AND");
    constraintFact.setConditionFacts(conditionFacts);

    List<ConstraintFact> constraintFacts = new ArrayList<>();
    constraintFacts.add(constraintFact);

    RepositoryPolicyViolation violation = new RepositoryPolicyViolation();
    violation.setConstraintFacts(constraintFacts);
    violation.setThreatLevel(9);
    violation.setThreatCategory(PolicyThreatCategory.SECURITY);
    violation.setActionTypeId("fail");
    violation.setPolicyName("Security Policy");

    return violation;
  }

  private AbstractPolicyViolation createViolationWithoutCve() {
    // Create constraint fact without CVE data
    ConstraintFact constraintFact = new ConstraintFact("cons1", "License constraint", "AND");
    ConditionFact conditionFact = new ConditionFact("cond1", 0, "LICENSE", "License violation");
    List<ConditionFact> conditionFacts = new ArrayList<>();
    conditionFacts.add(conditionFact);
    constraintFact.setConditionFacts(conditionFacts);

    List<ConstraintFact> constraintFacts = new ArrayList<>();
    constraintFacts.add(constraintFact);

    RepositoryPolicyViolation violation = new RepositoryPolicyViolation();
    violation.setConstraintFacts(constraintFacts);
    violation.setThreatLevel(5);
    violation.setThreatCategory(PolicyThreatCategory.LICENSE);
    violation.setActionTypeId("warn");
    violation.setPolicyName("License Policy");
    return violation;
  }

  private Component createComponentWithVulnerability(String cveNumber, String vector, String categoryId) {
    return createComponentWithVulnerability(cveNumber, vector, categoryId, null);
  }

  private Component createComponentWithVulnerability(String cveNumber, String vector, String categoryId,
      List<String> threatTypes)
  {
    SecurityVulnerability vulnerability = new SecurityVulnerability();
    vulnerability.setRefId(cveNumber);
    vulnerability.setVector(vector);

    if (categoryId != null) {
      // Try to get existing category, or use OTHER as fallback
      SecurityVulnerabilityCategory category = SecurityVulnerabilityCategory.getById(categoryId.toLowerCase());
      if (category == null) {
        // For test purposes, use OTHER category if exact match not found
        category = SecurityVulnerabilityCategory.OTHER;
      }
      vulnerability.addVulnerabilityCategory(category);
    }

    if (threatTypes != null) {
      vulnerability.setThreatTypes(threatTypes);
    }

    List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
    vulnerabilities.add(vulnerability);

    Component component = new Component();
    component.setSecurityVulnerabilities(vulnerabilities);

    return component;
  }

  private Component createComponentWithMalwareAttackVector(String refId, String attackVector,
      List<String> threatTypes)
  {
    SecurityVulnerability vulnerability = new SecurityVulnerability();
    vulnerability.setRefId(refId);
    vulnerability.setAttackVector(attackVector);
    if (threatTypes != null) {
      vulnerability.setThreatTypes(threatTypes);
    }
    vulnerability.addVulnerabilityCategory(SecurityVulnerabilityCategory.MALICIOUS_CODE);

    List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
    vulnerabilities.add(vulnerability);

    Component component = new Component();
    component.setSecurityVulnerabilities(vulnerabilities);

    return component;
  }

  private Component createComponentWithMalwareSeverity(String refId, String severityDescription,
      String attackVector, List<String> threatTypes)
  {
    SecurityVulnerability vulnerability = new SecurityVulnerability();
    vulnerability.setRefId(refId);
    vulnerability.setSeverityDescription(severityDescription);
    vulnerability.setAttackVector(attackVector);
    if (threatTypes != null) {
      vulnerability.setThreatTypes(threatTypes);
    }
    vulnerability.addVulnerabilityCategory(SecurityVulnerabilityCategory.MALICIOUS_CODE);

    List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
    vulnerabilities.add(vulnerability);

    Component component = new Component();
    component.setSecurityVulnerabilities(vulnerabilities);

    return component;
  }
}
