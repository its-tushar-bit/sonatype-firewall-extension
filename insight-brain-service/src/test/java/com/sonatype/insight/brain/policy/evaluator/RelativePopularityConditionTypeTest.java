/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Assert;
import org.junit.Test;

public class RelativePopularityConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", RelativePopularityConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateEquals() {
    // Create policy constraints
    Constraint constraint = createConstraint("=", "30");
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(null /* applicationId */, new Stage(
        BuildStageType.ID), Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", RelativePopularityConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateLess() {
    // Create policy constraints
    Constraint constraint = createConstraint("<", "30");
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(null /* applicationId */, new Stage(
        BuildStageType.ID), Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", RelativePopularityConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateLessOrEqual() {
    // Create policy constraints
    Constraint constraint = createConstraint("<=", "30");
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(null /* applicationId */, new Stage(
        BuildStageType.ID), Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 2, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", RelativePopularityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", RelativePopularityConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateGreater() {
    // Create policy constraints
    Constraint constraint = createConstraint(">", "30");
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(null /* applicationId */, new Stage(
        BuildStageType.ID), Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", RelativePopularityConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateGreaterOrEqual() {
    // Create policy constraints
    Constraint constraint = createConstraint(">=", "30");
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setRelativePopularity(10);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setRelativePopularity(30);
    components.add(component2);
    Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    component3.setRelativePopularity(50);
    components.add(component3);
    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(null /* applicationId */, new Stage(
        BuildStageType.ID), Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 2, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", RelativePopularityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", RelativePopularityConditionType.ID, policyAlerts);
  }

  @Test
  public void testValidateCondition_ValueNotANumber() {
    Condition condition = new Condition(RelativePopularityConditionType.ID, "=", "abc");
    try {
      new RelativePopularityConditionType().validateCondition(condition, null /* applicationId */);
      Assert.fail("Expected InvalidConditionException");
    }
    catch (InvalidConditionException expected) {
      if (!expected.getMessage().endsWith("Invalid relative popularity: abc")) {
        throw expected;
      }
    }
  }

  @Test
  public void testValidateCondition_ValueLessThanZero() {
    Condition condition = new Condition(RelativePopularityConditionType.ID, "=", "-1");
    try {
      new RelativePopularityConditionType().validateCondition(condition, null /* applicationId */);
      Assert.fail("Expected InvalidConditionException");
    }
    catch (InvalidConditionException expected) {
      if (!expected.getMessage().endsWith("Relative popularity must be between 0 and 100")) {
        throw expected;
      }
    }
  }

  @Test
  public void testValidateCondition_ValueGreaterThan100() {
    Condition condition = new Condition(RelativePopularityConditionType.ID, "=", "101");
    try {
      new RelativePopularityConditionType().validateCondition(condition, null /* applicationId */);
      Assert.fail("Expected InvalidConditionException");
    }
    catch (InvalidConditionException expected) {
      if (!expected.getMessage().endsWith("Relative popularity must be between 0 and 100")) {
        throw expected;
      }
    }
  }
}
