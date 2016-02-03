/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.DummyLicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LicenseConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private static LicenseDataUpdater savedLicenseDataUpdater;

  @BeforeClass
  public static void beforeClass() {
    savedLicenseDataUpdater = LicenseDataUpdater.getUpdater();
    LicenseDataUpdater.setUpdater(new DummyLicenseDataUpdater());
  }

  @AfterClass
  public static void afterClass() {
    LicenseDataUpdater.setUpdater(savedLicenseDataUpdater);
  }

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", LicenseConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateIs_Declared() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", "UNSPECIFIED");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("UNSPECIFIED");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("AFL-1.2");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsNot_Declared() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", "UNSPECIFIED");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("UNSPECIFIED");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIs_Observed() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", "UNSPECIFIED");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addObservedLicenseId("UNSPECIFIED");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addObservedLicenseId("AFL-1.2");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsNot_Observed() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", "UNSPECIFIED");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addObservedLicenseId("UNSPECIFIED");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addObservedLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIs_Overridden() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", "UNSPECIFIED");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("UNSPECIFIED");
    component1.addObservedLicenseId("UNSPECIFIED");
    component1.addLicenseOverrideId("Apache-2.0");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("AFL-1.2");
    component2.addObservedLicenseId("Apache-2.0");
    component2.addLicenseOverrideId("UNSPECIFIED");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIs_Overridden_Multiple_LicenseIds() {
    // Create policy constraints
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(createConstraint("constraintId1", "constraintName1", LicenseConditionType.ID, "is", "Apache-2.0"));

    // Create policy
    Policy policy = new Policy("policyId1", "policyName1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<>();
    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component.addDeclaredLicenseId("UNSPECIFIED");
    component.addObservedLicenseId("UNSPECIFIED");
    component.addLicenseOverrideId("Apache-2.0");
    component.addLicenseOverrideId("AFL-1.2");
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component, "policyId1", "policyName1", FailActionType.ID, "constraintId1",
        "constraintName1", LicenseConditionType.ID, policyAlerts);

    constraints = new ArrayList<>();
    constraints.add(createConstraint("constraintId2", "constraintName2", LicenseConditionType.ID, "is", "AFL-1.2"));

    policy.setConstraints(constraints);

    // Evaluate the policy
    policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component, "policyId1", "policyName1", FailActionType.ID, "constraintId2",
        "constraintName2", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsNot_Overridden() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", "UNSPECIFIED");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("UNSPECIFIED");
    component1.addObservedLicenseId("UNSPECIFIED");
    component1.addLicenseOverrideId("Apache-2.0");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("AFL-1.2");
    component2.addObservedLicenseId("Apache-2.0");
    component2.addLicenseOverrideId("UNSPECIFIED");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testValidateCondition_InvalidLicenseId() {
    Condition condition = new Condition(LicenseConditionType.ID, "is", "abc");
    try {
      new LicenseConditionType().validateCondition(condition, null /* applicationId */);
      Assert.fail("Expected InvalidConditionException");
    }
    catch (InvalidConditionException expected) {
      if (!expected.getMessage().endsWith("Invalid license id: abc")) {
        throw expected;
      }
    }
  }
}
