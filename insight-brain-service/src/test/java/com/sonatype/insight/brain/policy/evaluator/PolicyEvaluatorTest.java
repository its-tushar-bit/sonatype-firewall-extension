/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PolicyEvaluatorTest
    extends AbstractPolicyEvaluationTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private String applicationId = "PolicyEvaluatorTest_AppId";

  @Test
  public void testEvaluate_TwoConstraintsWithConditions() {
    final Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    constraints.add(constraint1);
    final Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraint2);

    final Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));

    final List<Component> components = new ArrayList<Component>();
    // A component with one security vulnerability
    final Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);
    // A component with Apache-2.0 license
    final Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    final List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy),
        components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(2, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId2",
        "Constraint Name 2", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_UnknowComponent_NoMatchStateConditionType() {
    Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    List<Constraint> constraints = new ArrayList<Constraint>();
    Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is not", "Apache-2.0"));
    constraints.add(constraint1);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component();
    component1.setMatchState(MatchState.UNKNOWN);
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy),
        components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());
  }

  @Test
  public void testEvaluate_UnknowComponent_MatchStateConditionType_SimpleConstraints() {
    Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    List<Constraint> constraints = new ArrayList<Constraint>();
    Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is not", "Apache-2.0"));
    constraints.add(constraint1);
    Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(MatchStateConditionType.ID, "is", MatchState.UNKNOWN.getId()));
    constraints.add(constraint2);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component();
    component1.setMatchState(MatchState.UNKNOWN);
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy),
        components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    List<ComponentFact> componentFacts = policyAlerts.get(0).getTrigger().getComponentFacts();
    Assert.assertEquals(1, componentFacts.size());
    List<ConstraintFact> constraintFacts = componentFacts.get(0).getConstraintFacts();
    Assert.assertEquals(constraintFacts.toString(), 1, constraintFacts.size());

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId2",
        "Constraint Name 2", MatchStateConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_UnknowComponent_MatchStateConditionType_ComplexConstraints() {
    Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    List<Constraint> constraints = new ArrayList<Constraint>();
    Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.OR);
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is not", "GPL-2.0"));
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", MatchState.UNKNOWN.getId()));
    constraints.add(constraint1);
    Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.OR);
    constraint2.addCondition(new Condition(LicenseConditionType.ID, "is not", "Apache-2.0"));
    constraints.add(constraint2);
    Constraint constraint3 = new Constraint("ConstraintId3", "Constraint Name 3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(LicenseConditionType.ID, "is not", "Apache-2.0"));
    constraints.add(constraint3);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));

    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component();
    component1.setMatchState(MatchState.UNKNOWN);
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy),
        components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    List<ComponentFact> componentFacts = policyAlerts.get(0).getTrigger().getComponentFacts();
    Assert.assertEquals(1, componentFacts.size());
    List<ConstraintFact> constraintFacts = componentFacts.get(0).getConstraintFacts();
    Assert.assertEquals(constraintFacts.toString(), 1, constraintFacts.size());

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", MatchStateConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_OneConstraintWithCompositeConditionAll() {
    final Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraint1);

    final Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));

    final List<Component> components = new ArrayList<Component>();
    // A component with one security vulnerability
    final Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy),
        components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // A component with Apache-2.0 license
    final Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // A component with one security vulnerability and Apache-2.0 license
    final Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    component3.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv2", 3F));
    component3.addDeclaredLicenseId("Apache-2.0");
    components.add(component3);

    // Evaluate the policy
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);

    // Another component with one security vulnerability and Apache-2.0 license
    final Component component4 = new Component("g4", "a4", "v4", MatchState.EXACT);
    component4.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv4", 3F));
    component4.addDeclaredLicenseId("Apache-2.0");
    components.add(component4);

    // Evaluate the policy
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(2, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_OneConstraintWithCompositeConditionAny() {
    final Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.OR);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraint1);

    final Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));

    final List<Component> components = new ArrayList<Component>();
    // A component with one security vulnerability
    final Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy),
        components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);

    // A component with Apache-2.0 license
    final Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(2, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);

    // A component with one security vulnerability and Apache-2.0 license
    final Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    component3.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv2", 3F));
    component3.addDeclaredLicenseId("Apache-2.0");
    components.add(component3);

    // Evaluate the policy
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(3, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);

    // Another component with one security vulnerability and Apache-2.0 license
    final Component component4 = new Component("g4", "a4", "v4", MatchState.EXACT);
    component4.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv4", 3F));
    component4.addDeclaredLicenseId("Apache-2.0");
    components.add(component4);

    // Evaluate the policy
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy), components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(4, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
        "Constraint Name 1", LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_ContextBasedActions() {
    // Create policy constraints
    final List<Constraint> constraints = new ArrayList<Constraint>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    constraints.add(constraint1);
    final Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraint2);

    final Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);

    policy.addAction(DevelopStageType.ID, new Action(WarnActionType.ID));
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    policy.addAction(ReleaseStageType.ID, new Action(NotifyActionType.ID));
    policy.getActions(ReleaseStageType.ID).get(0).setTarget("manager@some.com");

    final List<Component> components = new ArrayList<Component>();
    // A component with one security vulnerability
    final Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);
    // A component with Apache-2.0 license
    final Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    List<PolicyAlert> policyAlerts;
    List<? extends Action> actions;

    // Evaluate the policy when developing
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, new Stage(DevelopStageType.ID), Arrays.asList(policy),
        components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    actions = policyAlerts.get(0).getActions();
    Assert.assertEquals(1, actions.size());
    Assert.assertEquals(WarnActionType.ID, actions.get(0).getActionTypeId());
    Assert.assertNull(actions.get(0).getTarget());

    // Evaluate the policy when building
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, new Stage(BuildStageType.ID), Arrays.asList(policy),
        components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    actions = policyAlerts.get(0).getActions();
    Assert.assertEquals(1, actions.size());
    Assert.assertEquals(FailActionType.ID, actions.get(0).getActionTypeId());
    Assert.assertNull(actions.get(0).getTarget());

    // Evaluate the policy when releasing
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, new Stage(ReleaseStageType.ID), Arrays.asList(policy),
        components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    actions = policyAlerts.get(0).getActions();
    Assert.assertEquals(1, actions.size());
    Assert.assertEquals(NotifyActionType.ID, actions.get(0).getActionTypeId());
    Assert.assertEquals("manager@some.com", actions.get(0).getTarget());
  }

  @Test
  public void testEvaluate_SortedAlerts() {
    final List<Policy> policies = new ArrayList<Policy>();

    // randomly generate a series of policies
    for (int i = 0; i <= 25 * Math.random(); i++) {
      policies.add(randomPolicy());
    }

    final List<Component> components = new ArrayList<Component>();

    // A component with one security vulnerability
    final Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);

    // A component with Apache-2.0 license
    final Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // A component with one security vulnerability and Apache-2.0 license
    final Component component3 = new Component("g3", "a3", "v3", MatchState.EXACT);
    component3.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv2", 3F));
    component3.addDeclaredLicenseId("Apache-2.0");
    components.add(component3);

    // Another component with one security vulnerability and Apache-2.0 license
    final Component component4 = new Component("g4", "a4", "v4", MatchState.EXACT);
    component4.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv4", 3F));
    component4.addDeclaredLicenseId("Apache-2.0");
    components.add(component4);

    // Evaluate the facts
    final List<MatchFact> facts = PolicyEvaluator.evaluateFacts(applicationId, policies, components);

    // Sort facts by policy then component then constraint then condition
    Collections.sort(facts, PolicyEvaluator.MATCHES_BY_POLICY_COMPONENT_CONSTRAINT_CONDITION);
    final List<MatchFact> expectedFacts = new ArrayList<MatchFact>(facts);

    // Check sorting is consistent
    for (int i = 0; i < 100; i++) {
      Collections.shuffle(facts);

      Collections.sort(facts, PolicyEvaluator.MATCHES_BY_POLICY_COMPONENT_CONSTRAINT_CONDITION);

      Assert.assertEquals(expectedFacts, facts);
    }

    // Slice facts into alerts
    final List<PolicyAlert> expectedAlerts = PolicyEvaluator.createAlerts(policies, facts,
        new Stage(BuildStageType.ID), false /* forMonitoring */);

    // Check slicing is consistent
    for (int i = 0; i < 100; i++) {
      Collections.shuffle(facts);
      Collections.shuffle(policies);

      final List<PolicyAlert> alerts = PolicyEvaluator.createAlerts(policies, facts, new Stage(BuildStageType.ID),
          false /* forMonitoring */);

      Assert.assertEquals(alertsToString(expectedAlerts), alertsToString(alerts));
    }
  }

  @Test
  public void testEvaluate_OrgAndAppPolicies() throws Exception {
    OrganizationDAO orgDAO = new OrganizationDAO();
    Organization org = new Organization("testEvaluateOrgAndAppPolicies");
    orgDAO.insert(org);
    ApplicationDAO appDAO = new ApplicationDAO();
    Application app = new Application("testEvaluateOrgAndAppPolicies", "testEvaluateOrgAndAppPolicies", org.getId());
    appDAO.insert(app);
    PolicyDAO policyDAO = new PolicyDAO(tempDir.newFolder());

    Stage stage = new Stage(BuildStageType.ID);

    // Create org policy
    List<Constraint> constraints = new ArrayList<Constraint>();
    Constraint constraintOrg = new Constraint(null, "Constraint Name Org", LogicalOperator.AND);
    constraintOrg.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    constraints.add(constraintOrg);
    Policy policyOrg = new Policy(null, "Policy Name Org");
    policyOrg.setConstraints(constraints);
    policyOrg.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));
    policyDAO.insert(org.getId(), policyOrg);

    // Create app policy
    constraints = new ArrayList<Constraint>();
    Constraint constraintApp = new Constraint(null, "Constraint Name App", LogicalOperator.AND);
    constraintApp.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraintApp);
    Policy policyApp = new Policy(null, "Policy Name App");
    policyApp.setConstraints(constraints);
    policyApp.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));
    policyDAO.insert(app.getId(), policyApp);

    List<Component> components = new ArrayList<Component>();
    // A component with one security vulnerability
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);
    // A component with Apache-2.0 license
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policies
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(app.getId(), stage, policyDAO, components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(2, policyAlerts.size());
    assertContainsPolicyAlert(component1, policyOrg.getId(), "Policy Name Org", FailActionType.ID,
        constraintOrg.getId(), "Constraint Name Org", SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policyApp.getId(), "Policy Name App", FailActionType.ID,
        constraintApp.getId(), "Constraint Name App", LicenseConditionType.ID, policyAlerts);

    appDAO.delete(app);
    orgDAO.delete(org);
  }

  private static String alertsToString(final List<PolicyAlert> policyAlerts) {
    final StringBuilder buf = new StringBuilder();
    for (final PolicyAlert a : policyAlerts) {
      buf.append(a.getTrigger().toString());
    }
    return buf.toString();
  }

  private static final AtomicInteger policyCounter = new AtomicInteger();

  private static final AtomicInteger constraintCounter = new AtomicInteger();

  private static Policy randomPolicy() {
    final int n = policyCounter.getAndIncrement();
    final Policy policy = new Policy("PolicyId" + n, "Policy Name " + n);
    for (int i = 0; i <= 25 * Math.random(); i++) {
      policy.addConstraint(randomConstraint());
    }
    return policy;
  }

  private static Constraint randomConstraint() {
    final int n = constraintCounter.getAndIncrement();
    final Constraint constraint = new Constraint("ConstraintId" + n, "Constraint Name " + n, LogicalOperator.OR);
    final double r = Math.random();
    if (r <= 0.33) {
      constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
      constraint.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    }
    else if (r >= 0.66) {
      constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    }
    else {
      constraint.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    }
    return constraint;
  }

  @Test
  public void testEvaluate_PolicyWaived() {
    Stage stage = new Stage(BuildStageType.ID);

    // Create two policies
    List<Constraint> constraints1 = new ArrayList<Constraint>();
    Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    constraints1.add(constraint1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setConstraints(constraints1);
    policy1.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));
    List<Constraint> constraints2 = new ArrayList<Constraint>();
    Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    constraints2.add(constraint2);
    Policy policy2 = new Policy("PolicyId2", "Policy Name 2");
    policy2.setConstraints(constraints2);
    policy2.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));

    // Create two components
    List<Component> components = new ArrayList<Component>();
    Component component1 = new Component("g1", "a1", "v1", MatchState.EXACT);
    component1.setHash("hash1");
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    component1.addDeclaredLicenseId("Apache-2.0");
    components.add(component1);
    Component component2 = new Component("g2", "a2", "v2", MatchState.EXACT);
    component2.setHash("hash2");
    component2.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage,
        Arrays.asList(policy1, policy2), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(2, policyAlerts.size());
    Assert.assertEquals(2, policyAlerts.get(0).getTrigger().getComponentFacts().size());
    Assert.assertEquals(2, policyAlerts.get(1).getTrigger().getComponentFacts().size());
    assertContainsPolicyAlert(component1, policy1.getId(), policy1.getName(), FailActionType.ID, constraint1.getId(),
        constraint1.getName(), SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy1.getId(), policy1.getName(), FailActionType.ID, constraint1.getId(),
        constraint1.getName(), SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component1, policy2.getId(), policy2.getName(), FailActionType.ID, constraint2.getId(),
        constraint2.getName(), SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy2.getId(), policy2.getName(), FailActionType.ID, constraint2.getId(),
        constraint2.getName(), SecurityVulnerabilityConditionType.ID, policyAlerts);

    // Waive policy1 for component1 and re-evaluate
    PolicyWaiver policyWaiver = new PolicyWaiver("hash1", policy1.getId(), applicationId, null /* comment */);
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);
    policyAlerts = new PolicyEvaluator().evaluate(applicationId, stage, Arrays.asList(policy1, policy2), components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(2, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());
    Assert.assertEquals(2, policyAlerts.get(1).getTrigger().getComponentFacts().size());
    assertContainsPolicyAlert(component2, policy1.getId(), policy1.getName(), FailActionType.ID, constraint1.getId(),
        constraint1.getName(), SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component1, policy2.getId(), policy2.getName(), FailActionType.ID, constraint2.getId(),
        constraint2.getName(), SecurityVulnerabilityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy2.getId(), policy2.getName(), FailActionType.ID, constraint2.getId(),
        constraint2.getName(), SecurityVulnerabilityConditionType.ID, policyAlerts);

    policyWaiverDAO.delete(policyWaiver);
  }
}
