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
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RelativePopularityConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", RelativePopularityConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateNoPopularityData() {
    Constraint constraint = createConstraint("=", "50");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = ComponentFactory.forGav("g", "a", "v", MatchState.EXACT);
    component.setRelativePopularity(null);
    components.add(component);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testEvaluateEquals() {
    // Create policy constraints
    Constraint constraint = createConstraint("=", "30");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, RelativePopularityConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Relative popularity was 30%");
  }

  @Test
  public void testEvaluateLess() {
    // Create policy constraints
    Constraint constraint = createConstraint("<", "30");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, RelativePopularityConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Relative popularity was < 30% (relative popularity = 10%)");
  }

  @Test
  public void testEvaluateLessOrEqual() {
    // Create policy constraints
    Constraint constraint = createConstraint("<=", "30");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, RelativePopularityConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, RelativePopularityConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Relative popularity was <= 30% (relative popularity = 10%)");
    actualReason = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Relative popularity was <= 30% (relative popularity = 30%)");
  }

  @Test
  public void testEvaluateGreater() {
    // Create policy constraints
    Constraint constraint = createConstraint(">", "30");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component3, policy, constraint, FailActionType.ID, RelativePopularityConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Relative popularity was > 30% (relative popularity = 50%)");
  }

  @Test
  public void testEvaluateGreaterOrEqual() {
    // Create policy constraints
    Constraint constraint = createConstraint(">=", "30");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, RelativePopularityConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint, FailActionType.ID, RelativePopularityConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Relative popularity was >= 30% (relative popularity = 30%)");
    actualReason = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Relative popularity was >= 30% (relative popularity = 50%)");
  }

  @Test
  public void testValidateCondition_ValueNotANumber() {
    Condition condition = new Condition(RelativePopularityConditionType.ID, "=", "abc");
    assertThatThrownBy(
        () -> new RelativePopularityConditionType().validateCondition(null, condition, null /* applicationId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Invalid relative popularity: abc");
  }

  @Test
  public void testValidateCondition_ValueLessThanZero() {
    Condition condition = new Condition(RelativePopularityConditionType.ID, "=", "-1");
    assertThatThrownBy(
        () -> new RelativePopularityConditionType().validateCondition(null, condition, null /* applicationId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Relative popularity must be between 0 and 100");
  }

  @Test
  public void testValidateCondition_ValueGreaterThan100() {
    Condition condition = new Condition(RelativePopularityConditionType.ID, "=", "101");
    assertThatThrownBy(() -> new RelativePopularityConditionType().validateCondition(null, condition,
        null /* applicationId */)).isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Relative popularity must be between 0 and 100");
  }
}
