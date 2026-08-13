/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.ComponentEndOfLifeStatus;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentEndOfLifeConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentEndOfLifeConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator) {
    return createConstraint("ConstraintId1", "Constraint Name 1", ComponentEndOfLifeConditionType.ID, operator,
        null /* value */);
  }

  @Test
  public void testEvaluateIsTrue() {
    // Create policy constraints
    Constraint constraint = createConstraint("is true");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forCoordinates("maven", "g1", "a1", "v1", "jar", "");
    component1.setMatchState(MatchState.EXACT);
    component1.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_TRUE);
    components.add(component1);
    Component component2 = ComponentFactory.forCoordinates("maven", "g2", "a2", "v2", "jar", "");
    component2.setMatchState(MatchState.EXACT);
    component2.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_FALSE);
    components.add(component2);
    Component component3 = new Component();
    component3.setMatchState(MatchState.UNKNOWN);
    component3.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_TRUE);
    components.add(component3);
    Component component4 = new Component();
    component4.setMatchState(MatchState.UNKNOWN);
    component4.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_UNKNOWN);
    components.add(component4);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, ComponentEndOfLifeConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testEvaluateIsFalse() {
    // Create policy constraints
    Constraint constraint = createConstraint("is false");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forCoordinates("maven", "g1", "a1", "v1", "jar", "");
    component1.setMatchState(MatchState.EXACT);
    component1.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_TRUE);
    components.add(component1);
    Component component2 = ComponentFactory.forCoordinates("maven", "g2", "a2", "v2", "jar", "");
    component2.setMatchState(MatchState.EXACT);
    component2.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_FALSE);
    components.add(component2);
    Component component3 = new Component();
    component3.setMatchState(MatchState.UNKNOWN);
    component3.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_TRUE);
    components.add(component3);
    Component component4 = new Component();
    component4.setMatchState(MatchState.UNKNOWN);
    component4.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_UNKNOWN);
    components.add(component4);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, ComponentEndOfLifeConditionType.ID,
        policyAlerts);
  }

  @Test
  public void testHandlesNullEndOfLife() {
    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forCoordinates("maven", "g1", "a1", "v1", "jar", "");
    component1.setMatchState(MatchState.EXACT);
    component1.setEndOfLife(null);
    components.add(component1);

    // Create "is false" policy constraints
    Constraint isFalseConstraint = createConstraint("is false");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(isFalseConstraint);

    // Create "is false" policy
    Policy isFalsePolicy = new Policy("PolicyId1", "is false");
    isFalsePolicy.setConstraints(constraints);
    isFalsePolicy.setAction(BuildStageType.ID, FailActionType.ID);

    // Evaluate the "is false" policy
    List<PolicyAlert> policyAlerts = evaluate(isFalsePolicy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, isFalsePolicy, isFalseConstraint, FailActionType.ID,
        ComponentEndOfLifeConditionType.ID, policyAlerts);

    // Create "is true" policy constraints
    Constraint isTrueConstraint = createConstraint("is true");
    constraints = new ArrayList<>();
    constraints.add(isTrueConstraint);

    // Create "is true" policy
    Policy isTruePolicy = new Policy("PolicyId2", "is true");
    isTruePolicy.setConstraints(constraints);
    isTruePolicy.setAction(BuildStageType.ID, FailActionType.ID);

    // Evaluate the "is true" policy
    policyAlerts = evaluate(isTruePolicy, components);
    assertThat(policyAlerts).isEmpty();
  }
}
