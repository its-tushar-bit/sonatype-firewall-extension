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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LabelConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private static String applicationPublicId = "LabelConditionTypeTest";

  private static String organizationId;

  private static String applicationId;

  @AfterClass
  public static void afterClass() {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = applicationDAO.getByIdNotNull(applicationId);
    applicationDAO.delete(application);

    OrganizationDAO organizationDAO = new OrganizationDAO();
    Organization organization = organizationDAO.getByIdNotNull(organizationId);
    organizationDAO.delete(organization);
  }

  @After
  public void cleanup() {
    for (Label label : labelDAO.getByOwnerId(applicationId)) {
      labelDAO.delete(label);
    }
    for (Label label : labelDAO.getByOwnerId(organizationId)) {
      labelDAO.delete(label);
    }
  }

  @BeforeClass
  public static void createApplication() {
    OrganizationDAO organizationDAO = new OrganizationDAO();
    Organization organization = new Organization();
    organization.setName("test-organization");
    organizationDAO.insert(organization);
    organizationId = organization.getId();

    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = new Application();
    application.setName("test-application");
    application.setPublicId(applicationPublicId);
    application.setOrganizationId(organizationId);
    applicationDAO.insert(application);
    applicationId = application.getId();
  }

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", LabelConditionType.ID, operator, value);
  }

  private LabelDAO labelDAO = new LabelDAO();

  @Test
  public void testEvaluateIs() {
    // Create some labels
    Label label1 = new Label(applicationId, "Good", Color.green);
    labelDAO.insert(label1);
    String labelId1 = label1.getId();
    Label label2 = new Label(applicationId, "Bad", Color.red);
    labelDAO.insert(label2);
    String labelId2 = label2.getId();

    // Create policy constraints
    Constraint constraint = createConstraint("is", labelId1);
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addLabelId(labelId1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addLabelId(labelId2);
    components.add(component2);
    Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(applicationId, new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LabelConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluateIsNot() {
    // Create some labels
    Label label1 = new Label(applicationId, "Good", Color.green);
    labelDAO.insert(label1);
    String labelId1 = label1.getId();
    Label label2 = new Label(applicationId, "Bad", Color.red);
    labelDAO.insert(label2);
    String labelId2 = label2.getId();

    // Create policy constraints
    Constraint constraint = createConstraint("is not", labelId1);
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addLabelId(labelId1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addLabelId(labelId2);
    components.add(component2);
    Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(applicationId, new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 2, policyAlerts.get(0));

    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LabelConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LabelConditionType.ID, policyAlerts);
  }

  @Test
  public void testOrganizationLabelIs() {
    // Create some labels
    Label label1 = new Label(organizationId, "Good", Color.green);
    labelDAO.insert(label1);
    String labelId1 = label1.getId();
    Label label2 = new Label(organizationId, "Bad", Color.red);
    labelDAO.insert(label2);
    String labelId2 = label2.getId();

    // Create policy constraints
    Constraint constraint = createConstraint("is", labelId1);
    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addLabelId(labelId1);
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addLabelId(labelId2);
    components.add(component2);
    Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluator.evaluate(applicationId, new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    assertFactCounts(1, 1, policyAlerts.get(0));

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LabelConditionType.ID, policyAlerts);
  }

  @Test
  public void testValidateCondition_InvalidLabelId() {
    Condition condition = new Condition(LabelConditionType.ID, "is", "abc");
    try {
      new LabelConditionType().validateCondition(condition, applicationId);
      Assert.fail("Expected InvalidConditionException");
    }
    catch (InvalidConditionException expected) {
      if (!expected.getMessage().endsWith("Invalid label id: abc")) {
        throw expected;
      }
    }
  }

  @Test
  public void testEvaluateLabelNameEdgeCase() {
    Label label1 = new Label(applicationId, "*/comment-end", Color.green);
    labelDAO.insert(label1);
    String labelId1 = label1.getId();

    List<Constraint> constraints = new ArrayList<Constraint>();
    constraints.add(createConstraint("is", labelId1));

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);

    List<PolicyAlert> policyAlerts = evaluator.evaluate(applicationId, new Stage(BuildStageType.ID),
        Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());
  }
}
