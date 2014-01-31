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
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LicenseThreatGroupConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private ComponentDAO componentDAO = new ComponentDAO();

  private Organization org;

  private Application app;

  @Before
  public void before() {
    org = tempEntity.newOrganization("LicenseThreatGroupConditionTypeTest", false /* createLicenseThreatGroups */);
    app = tempEntity.newApplication("test", "LicenseThreatGroupConditionTypeTest_AppId", org.getId());

    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup(app.getId(), "Copyleft", 8);
    new LicenseThreatGroupDAO().insert(licenseThreatGroup);
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense(app.getId(),
        licenseThreatGroup.getId(), "GPL-2.0");
    new LicenseThreatGroupLicenseDAO().insert(licenseThreatGroupLicense);

    licenseThreatGroup = new LicenseThreatGroup(app.getId(), "Liberal", 2);
    new LicenseThreatGroupDAO().insert(licenseThreatGroup);
    licenseThreatGroupLicense = new LicenseThreatGroupLicense(app.getId(), licenseThreatGroup.getId(), "Apache-2.0");
    new LicenseThreatGroupLicenseDAO().insert(licenseThreatGroupLicense);
  }

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", LicenseThreatGroupConditionType.ID, operator, value);
  }

  @Test
  public void testExplainMatchIs() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId());
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);

    // Component has licenses in both the 'Liberal' and 'Copyleft' license threat groups,
    // but only 'Copyleft' should be reported as that's what the condition is matching on.

    component1.addDeclaredLicenseId("Apache-2.0");
    component1.addDeclaredLicenseId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);

    Assert.assertEquals("Found a License in the 'Copyleft' License Threat Group",
        new LicenseThreatGroupConditionType().explainMatch(condition, component1));
  }

  @Test
  public void testExplainMatchIsNot() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is not", licenseThreatGroup.getId());
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    Assert.assertEquals("Found no License Threat Groups",
        new LicenseThreatGroupConditionType().explainMatch(condition, component1));

    component1.addDeclaredLicenseId("Apache-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    Assert.assertEquals("Found 'Liberal' License Threat Group",
        new LicenseThreatGroupConditionType().explainMatch(condition, component1));

    component1.addDeclaredLicenseId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    Assert.assertEquals("Found 'Liberal' and 'Copyleft' License Threat Groups",
        new LicenseThreatGroupConditionType().explainMatch(condition, component1));
  }

  @Test
  public void testEvaluateIs_Declared() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    // Create policy constraints
    Constraint constraint = createConstraint("is", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsNot_Declared() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    // Create policy constraints
    Constraint constraint = createConstraint("is not", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIs_Observed() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    // Create policy constraints
    Constraint constraint = createConstraint("is", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addObservedLicenseId("Apache-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addObservedLicenseId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsNot_Observed() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    // Create policy constraints
    Constraint constraint = createConstraint("is not", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addObservedLicenseId("Apache-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addObservedLicenseId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIs_Overridden() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    // Create policy constraints
    Constraint constraint = createConstraint("is", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    component1.addObservedLicenseId("Apache-2.0");
    component1.setLicenseOverrideId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    component2.addObservedLicenseId("GPL-2.0");
    component2.setLicenseOverrideId("Apache-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsNot_Overridden() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    // Create policy constraints
    Constraint constraint = createConstraint("is not", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    component1.addObservedLicenseId("Apache-2.0");
    component1.setLicenseOverrideId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    component2.addObservedLicenseId("GPL-2.0");
    component2.setLicenseOverrideId("Apache-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts);
  }

  @Test
  public void testValidateCondition_InvalidLicenseThreatGroupId() {
    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is", "abc");
    try {
      new LicenseThreatGroupConditionType().validateCondition(condition, app.getId());
      Assert.fail("Expected InvalidConditionException");
    }
    catch (InvalidConditionException expected) {
      if (!expected.getMessage().endsWith("Invalid license threat group id: abc")) {
        throw expected;
      }
    }
  }

  @Test
  public void testEvaluate_LicenseThreatGroupFromOrganization() {
    LicenseThreatGroup orgLicenseThreatGroup = new LicenseThreatGroup(org.getId(),
        "testEvaluate-LicenseThreatGroupFromOrganization", 5);
    new LicenseThreatGroupDAO().insert(orgLicenseThreatGroup);
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense(org.getId(),
        orgLicenseThreatGroup.getId(), "Apache-2.0");
    new LicenseThreatGroupLicenseDAO().insert(licenseThreatGroupLicense);

    // Create policy constraints
    Constraint constraint = createConstraint("is", orgLicenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    componentDAO.loadLicenseThreatGroups(app.getId(), component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts);
  }
}
