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
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AgeInDaysConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", AgeInDaysConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateOlderThan() {
    // Create policy constraints
    Constraint constraint = createConstraint("older than", "11");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // A component without age
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);
    // A component with age 10
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setCatalogDate(System.currentTimeMillis() - 10 * AgeInDaysConditionType.DAY_IN_MILLISECONDS - 1);
    components.add(component2);
    // A component with age 20
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.setCatalogDate(System.currentTimeMillis() - 20 * AgeInDaysConditionType.DAY_IN_MILLISECONDS - 1);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component3, policy, constraint, FailActionType.ID, AgeInDaysConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Found component older than 11 days");
  }

  @Test
  public void testEvaluateComponentWithoutCatalogDate() {
    // Create policy constraints
    Constraint constraint = createConstraint("older than", "11");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // A component without age
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);
    // A component with age 0
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setCatalogDate(0L);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testEvaluateYoungerThan() {
    // Create policy constraints
    Constraint constraint = createConstraint("younger than", "11");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    // A component without age
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);
    // A component with age 10
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setCatalogDate(System.currentTimeMillis() - 10 * AgeInDaysConditionType.DAY_IN_MILLISECONDS - 1);
    components.add(component2);
    // A component with age 20
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.setCatalogDate(System.currentTimeMillis() - 20 * AgeInDaysConditionType.DAY_IN_MILLISECONDS - 1);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, AgeInDaysConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Found component younger than 11 days");
  }

  @Test
  public void testValidateCondition_ValueNotANumber() {
    Condition condition = new Condition(AgeInDaysConditionType.ID, "older than", "abc");
    assertThatThrownBy(() -> new AgeInDaysConditionType().validateCondition(null, condition, null /* applicationId */))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("Invalid age (in days): abc");
  }

  @Test
  public void testExplainMatch_CalculatesDateDurationFromDays() {
    Condition condition = new Condition(AgeInDaysConditionType.ID, "older than", "1");
    String actualReason = new AgeInDaysConditionType().explainMatch(condition, null);
    assertThat(actualReason).isEqualTo("Found component older than 1 days");

    condition = new Condition(AgeInDaysConditionType.ID, "older than", "7");
    actualReason = new AgeInDaysConditionType().explainMatch(condition, null);
    assertThat(actualReason).isEqualTo("Found component older than 1 weeks");

    condition = new Condition(AgeInDaysConditionType.ID, "older than", String.valueOf(7 * 2));
    actualReason = new AgeInDaysConditionType().explainMatch(condition, null);
    assertThat(actualReason).isEqualTo("Found component older than 2 weeks");

    condition = new Condition(AgeInDaysConditionType.ID, "older than", "30");
    actualReason = new AgeInDaysConditionType().explainMatch(condition, null);
    assertThat(actualReason).isEqualTo("Found component older than 1 months");

    condition = new Condition(AgeInDaysConditionType.ID, "older than", String.valueOf(30 * 2));
    actualReason = new AgeInDaysConditionType().explainMatch(condition, null);
    assertThat(actualReason).isEqualTo("Found component older than 2 months");

    condition = new Condition(AgeInDaysConditionType.ID, "older than", "365");
    actualReason = new AgeInDaysConditionType().explainMatch(condition, null);
    assertThat(actualReason).isEqualTo("Found component older than 1 years");

    condition = new Condition(AgeInDaysConditionType.ID, "older than", String.valueOf(365 * 2));
    actualReason = new AgeInDaysConditionType().explainMatch(condition, null);
    assertThat(actualReason).isEqualTo("Found component older than 2 years");

    condition = new Condition(AgeInDaysConditionType.ID, "older than", "1000");
    actualReason = new AgeInDaysConditionType().explainMatch(condition, null);
    assertThat(actualReason).isEqualTo("Found component older than 1000 days");
  }
}
