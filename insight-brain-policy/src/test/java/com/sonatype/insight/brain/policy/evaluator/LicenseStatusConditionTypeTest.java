/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerLicenseStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LicenseStatusConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", LicenseStatusConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateIs() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", LicenseOverrideStatus.OPEN.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // A component with license status OPEN
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);
    // A component with license status CONFIRMED
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setLicenseOverrideStatus(LicenseOverrideStatus.CONFIRMED);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseStatus(LicenseOverrideStatus.OPEN.getId()));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LicenseStatusConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("License status was Open");
  }

  @Test
  public void testEvaluateIsNot() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", LicenseOverrideStatus.OPEN.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // A component with license status OPEN
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);
    // A component with license status CONFIRMED
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setLicenseOverrideStatus(LicenseOverrideStatus.CONFIRMED);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseStatus(LicenseOverrideStatus.OPEN.getId()));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, LicenseStatusConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("License status was Confirmed, not Open");
  }

  @Test
  public void testValidateCondition_ValueNotAStatusId() {
    Condition condition = new Condition(LicenseStatusConditionType.ID, "is", "abc");
    assertThatThrownBy(
        () -> new LicenseStatusConditionType().validateCondition(null, condition, null /* applicationId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Value not supported: abc");
  }
}
