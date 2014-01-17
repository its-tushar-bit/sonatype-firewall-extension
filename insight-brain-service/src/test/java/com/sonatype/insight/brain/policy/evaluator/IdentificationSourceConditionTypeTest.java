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
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Assert;
import org.junit.Test;

public class IdentificationSourceConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", IdentificationSourceConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateIs() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", IdentificationSource.SONATYPE.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setIdentificationSource(IdentificationSource.SONATYPE);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setIdentificationSource(IdentificationSource.MANUAL);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(null /* applicationId */, new Stage(
        BuildStageType.ID), Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", IdentificationSourceConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsNot() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", IdentificationSource.SONATYPE.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setIdentificationSource(IdentificationSource.SONATYPE);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setIdentificationSource(IdentificationSource.MANUAL);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(null /* applicationId */, new Stage(
        BuildStageType.ID), Arrays.asList(policy), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", IdentificationSourceConditionType.ID, policyAlerts);
  }

  @Test
  public void testValidateCondition_InvalidValue() {
    Condition condition = new Condition(IdentificationSourceConditionType.ID, "is", "abc");
    try {
      new IdentificationSourceConditionType().validateCondition(condition, null /* applicationId */);
      Assert.fail("Expected InvalidConditionException");
    }
    catch (InvalidConditionException expected) {
      if (!expected.getMessage().endsWith("Value not supported: abc")) {
        throw expected;
      }
    }
  }
}
