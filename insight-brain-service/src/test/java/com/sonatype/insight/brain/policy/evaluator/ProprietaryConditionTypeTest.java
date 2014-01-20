/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Assert;
import org.junit.Test;

public class ProprietaryConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator) {
    return createConstraint("ConstraintId1", "Constraint Name 1", ProprietaryConditionType.ID, operator, null /* value */);
  }

  @Test
  public void testEvaluateIsTrue() {
    // Create policy constraints
    Constraint constraint = createConstraint("is true");
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setProprietary(true);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setProprietary(false);
    components.add(component2);
    Component component3 = new Component();
    component3.setMatchState(MatchState.UNKNOWN);
    component3.setProprietary(true);
    components.add(component3);
    Component component4 = new Component();
    component4.setMatchState(MatchState.UNKNOWN);
    component4.setProprietary(false);
    components.add(component4);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(null /* applicationId */, new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 2, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", ProprietaryConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", ProprietaryConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsFalse() {
    // Create policy constraints
    Constraint constraint = createConstraint("is false");
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setProprietary(true);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setProprietary(false);
    components.add(component2);
    Component component3 = new Component();
    component3.setMatchState(MatchState.UNKNOWN);
    component3.setProprietary(true);
    components.add(component3);
    Component component4 = new Component();
    component4.setMatchState(MatchState.UNKNOWN);
    component4.setProprietary(false);
    components.add(component4);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(null /* applicationId */, new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 2, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", ProprietaryConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", ProprietaryConditionType.ID, policyAlerts);
  }
}
