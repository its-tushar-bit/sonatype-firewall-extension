/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.KevData;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.KevStatusConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerSecurityVulnerabilityWithKev;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.vulnerability.KevStatus;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class KevStatusConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  @Test
  public void testEvaluateIs_KnownToBeExploited_KevStatusTrue() {
    Constraint constraint = createConstraint("is", KevStatus.KNOWN_TO_BE_EXPLOITED.getId(), "1");
    List<Constraint> constraints = List.of(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forCoordinates("maven","g1", "a1", "v1", "jar", "");
    Component component2 = ComponentFactory.forCoordinates("maven","g2", "a2", "v2", "jar", "");
    SecurityVulnerability securityVulnerabilityThatTriggersPolicy =
        new SecurityVulnerability("osvdb", "sv2", 3F);
    KevData kevData = new KevData(true);
    securityVulnerabilityThatTriggersPolicy.setKevData(kevData);

    component2.addSecurityVulnerability(securityVulnerabilityThatTriggersPolicy);

    List<Component> components = List.of(component1, component2);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerSecurityVulnerabilityWithKev(securityVulnerabilityThatTriggersPolicy));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        KevStatusConditionType.ID, expectedConditionTrigger, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo(
        "Vulnerability sv2 listed in the Known Exploited Vulnerabilities (KEV) database.");
  }

  @Test
  public void testEvaluateIs_KnownToBeExploited_KevStatusFalse() {
    Constraint constraint = createConstraint("is", KevStatus.KNOWN_TO_BE_EXPLOITED.getId(), "1");
    List<Constraint> constraints = List.of(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forCoordinates("maven","g1", "a1", "v1", "jar", "");
    Component component2 = ComponentFactory.forCoordinates("maven","g2", "a2", "v2", "jar", "");
    SecurityVulnerability securityVulnerabilityThatTriggersPolicy =
        new SecurityVulnerability("osvdb", "sv2", 3F);
    KevData kevData = new KevData(false);
    securityVulnerabilityThatTriggersPolicy.setKevData(kevData);

    component2.addSecurityVulnerability(securityVulnerabilityThatTriggersPolicy);

    List<Component> components = List.of(component1, component2);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testEvaluateIs_KnownToBeExploited_KevStatusNull() {
    Constraint constraint = createConstraint("is", KevStatus.KNOWN_TO_BE_EXPLOITED.getId(), "1");
    List<Constraint> constraints = List.of(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forCoordinates("maven","g1", "a1", "v1", "jar", "");
    Component component2 = ComponentFactory.forCoordinates("maven","g2", "a2", "v2", "jar", "");
    SecurityVulnerability securityVulnerabilityThatTriggersPolicy =
        new SecurityVulnerability("osvdb", "sv2", 3F);
    KevData kevData = new KevData(null);
    securityVulnerabilityThatTriggersPolicy.setKevData(kevData);

    component2.addSecurityVulnerability(securityVulnerabilityThatTriggersPolicy);

    List<Component> components = List.of(component1, component2);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testEvaluateIs_NotListed_KevStatusFalse() {
    Constraint constraint = createConstraint("is", KevStatus.NOT_LISTED.getId(), "1");
    List<Constraint> constraints = List.of(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forCoordinates("maven","g1", "a1", "v1", "jar", "");

    Component component2 = ComponentFactory.forCoordinates("maven","g2", "a2", "v2", "jar", "");
    SecurityVulnerability securityVulnerabilityThatTriggersPolicy =
        new SecurityVulnerability("osvdb", "sv2", 3F);
    KevData kevData = new KevData(false);
    securityVulnerabilityThatTriggersPolicy.setKevData(kevData);

    component2.addSecurityVulnerability(securityVulnerabilityThatTriggersPolicy);

    List<Component> components = List.of(component1, component2);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerSecurityVulnerabilityWithKev(securityVulnerabilityThatTriggersPolicy));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        KevStatusConditionType.ID, expectedConditionTrigger, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo(
        "Vulnerability sv2 not listed in the Known Exploited Vulnerabilities (KEV) database.");
  }

  @Test
  public void testValidateCondition_KevStatusTrue() {
    Constraint constraint = createConstraint("is", KevStatus.NOT_LISTED.getId(), "1");
    List<Constraint> constraints = List.of(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forCoordinates("maven","g1", "a1", "v1", "jar", "");
    SecurityVulnerability securityVulnerability =
        new SecurityVulnerability("osvdb", "sv5", 3F);
    KevData kevData = new KevData(true);
    securityVulnerability.setKevData(kevData);

    component1.addSecurityVulnerability(securityVulnerability);

    List<Component> components = List.of(component1);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testValidateCondition_KevStatusNull() {
    Constraint constraint = createConstraint("is", KevStatus.NOT_LISTED.getId(), "1");
    List<Constraint> constraints = List.of(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forCoordinates("maven","g1", "a1", "v1", "jar", "");
    SecurityVulnerability securityVulnerability =
        new SecurityVulnerability("osvdb", "sv5", 3F);
    KevData kevData = new KevData(null);
    securityVulnerability.setKevData(kevData);

    component1.addSecurityVulnerability(securityVulnerability);

    List<Component> components = List.of(component1);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  private Constraint createConstraint(String operator, String value, String constraintSequence) {
    return createConstraint("ConstraintId" + constraintSequence, "Constraint Name " + constraintSequence,
        KevStatusConditionType.ID, operator, value);
  }
}
