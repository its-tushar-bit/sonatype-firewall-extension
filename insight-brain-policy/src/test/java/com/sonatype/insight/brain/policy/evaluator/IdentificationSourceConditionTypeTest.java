/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IdentificationSourceConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", IdentificationSourceConditionType.ID, operator,
        value);
  }

  @Test
  public void testEvaluateIs() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", IdentificationSource.SONATYPE.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setIdentificationSource(IdentificationSource.SONATYPE);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setIdentificationSource(IdentificationSource.MANUAL);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, IdentificationSourceConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Identification Source was Sonatype");
  }

  @Test
  public void testEvaluateIsNot() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", IdentificationSource.SONATYPE.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setIdentificationSource(IdentificationSource.SONATYPE);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setIdentificationSource(IdentificationSource.MANUAL);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, IdentificationSourceConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Identification Source was Manual, not Sonatype");
  }

  @Test
  public void testValidateCondition_InvalidValue() {
    Condition condition = new Condition(IdentificationSourceConditionType.ID, "is", "abc");
    assertThatThrownBy(
        () -> new IdentificationSourceConditionType().validateCondition(null, condition, null /* applicationId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Value not supported: abc");
  }
}
