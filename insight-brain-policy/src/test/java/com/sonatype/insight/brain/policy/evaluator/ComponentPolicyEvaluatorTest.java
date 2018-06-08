/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
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
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.facts.TriggerSecurityVulnerabilityWithSeverity;
import com.sonatype.insight.brain.model.policy.facts.TriggerSecurityVulnerabilityWithStatus;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.policy.DroolsGenerator;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ComponentPolicyEvaluatorTest
    extends AbstractPolicyEvaluationTest
{
  @Test
  public void testEvaluate_ConstraintFactHasDisplayName() {

    List<Constraint> constraints = new ArrayList<>();
    Constraint constraint = new Constraint("ConstraintId", "Constraint Name", LogicalOperator.OR);
    constraint.addCondition(new Condition(MatchStateConditionType.ID, "is", MatchState.SIMILAR.toString()));
    constraints.add(constraint);

    Policy policy = new Policy("PolicyId", "Policy Name");
    policy.setConstraints(constraints);

    List<Component> components = new ArrayList<>();
    Component component = ComponentFactory.forGav("g", "a", "v", MatchState.SIMILAR);
    components.add(component);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());
    Assert.assertEquals("g : a : v", policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getDisplayName()
        .toString());
  }

  @Test
  public void testEvaluate_TwoConstraintsWithConditions() {
    final Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    final List<Constraint> constraints = new ArrayList<>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraints.add(constraint1);
    final Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraint2);

    final Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);

    final List<Component> components = new ArrayList<>();
    // A component with one security vulnerability
    final Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);
    // A component with Apache-2.0 license
    final Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    final List<PolicyAlert> policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(2, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());
    Assert.assertEquals(1, policyAlerts.get(1).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint2, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_UnknowComponent_NoMatchStateConditionType() {
    Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    List<Constraint> constraints = new ArrayList<>();
    Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is not", "Apache-2.0"));
    constraints.add(constraint1);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = new Component();
    component1.setMatchState(MatchState.UNKNOWN);
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(stage, policy, components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());
  }

  @Test
  public void testEvaluate_UnknowComponent_MatchStateConditionType_SimpleConstraints() {
    Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    List<Constraint> constraints = new ArrayList<>();
    Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is not", "Apache-2.0"));
    constraints.add(constraint1);
    Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(MatchStateConditionType.ID, "is", MatchState.UNKNOWN.getId()));
    constraints.add(constraint2);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = new Component();
    component1.setMatchState(MatchState.UNKNOWN);
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    List<ComponentFact> componentFacts = policyAlerts.get(0).getTrigger().getComponentFacts();
    Assert.assertEquals(1, componentFacts.size());
    List<ConstraintFact> constraintFacts = componentFacts.get(0).getConstraintFacts();
    Assert.assertEquals(constraintFacts.toString(), 1, constraintFacts.size());

    assertContainsPolicyAlert(component1, policy, constraint2, FailActionType.ID, MatchStateConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_UnknowComponent_MatchStateConditionType_ComplexConstraints() {
    Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    List<Constraint> constraints = new ArrayList<>();
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
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = new Component();
    component1.setMatchState(MatchState.UNKNOWN);
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    List<ComponentFact> componentFacts = policyAlerts.get(0).getTrigger().getComponentFacts();
    Assert.assertEquals(1, componentFacts.size());
    List<ConstraintFact> constraintFacts = componentFacts.get(0).getConstraintFacts();
    Assert.assertEquals(constraintFacts.toString(), 1, constraintFacts.size());

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID, MatchStateConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_OneConstraintWithCompositeConditionAll() {
    final Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    final List<Constraint> constraints = new ArrayList<>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraint1);

    final Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);

    final List<Component> components = new ArrayList<>();
    // A component with one security vulnerability
    final Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // A component with Apache-2.0 license
    final Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // A component with one security vulnerability and Apache-2.0 license
    final Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv2", 3F));
    component3.addDeclaredLicenseId("Apache-2.0");
    components.add(component3);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);

    // Another component with one security vulnerability and Apache-2.0 license
    final Component component4 = ComponentFactory.forGav("g4", "a4", "v4", MatchState.EXACT);
    component4.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv4", 3F));
    component4.addDeclaredLicenseId("Apache-2.0");
    components.add(component4);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(2, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());
    Assert.assertEquals(1, policyAlerts.get(1).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_OneConstraintWithCompositeConditionAny() {
    final Stage stage = new Stage(BuildStageType.ID);

    // Create policy constraints
    final List<Constraint> constraints = new ArrayList<>();
    final Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.OR);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraint1.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraint1);

    final Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);

    final List<Component> components = new ArrayList<>();
    // A component with one security vulnerability
    final Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);

    // A component with Apache-2.0 license
    final Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(2, policyAlerts.size());
    Assert.assertEquals(1, policyAlerts.get(0).getTrigger().getComponentFacts().size());
    Assert.assertEquals(1, policyAlerts.get(1).getTrigger().getComponentFacts().size());

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);

    // A component with one security vulnerability and Apache-2.0 license
    final Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv2", 3F));
    component3.addDeclaredLicenseId("Apache-2.0");
    components.add(component3);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(4, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      Assert.assertEquals(1, policyAlert.getTrigger().getComponentFacts().size());
    }

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);

    // Another component with one security vulnerability and Apache-2.0 license
    final Component component4 = ComponentFactory.forGav("g4", "a4", "v4", MatchState.EXACT);
    component4.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv4", 3F));
    component4.addDeclaredLicenseId("Apache-2.0");
    components.add(component4);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(6, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      Assert.assertEquals(1, policyAlert.getTrigger().getComponentFacts().size());
    }

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, policy, constraint1, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_ContextBasedActions() {
    // Create policy constraints
    final List<Constraint> constraints = new ArrayList<>();
    final Constraint constraint = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraints.add(constraint);

    final Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);

    policy.setAction(DevelopStageType.ID, WarnActionType.ID);
    policy.setAction(BuildStageType.ID, FailActionType.ID);
    policy.getNotifications().add(new UserNotification("manager@some.com", ReleaseStageType.ID));

    final List<Component> components = new ArrayList<>();
    // A component with one security vulnerability
    final Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);

    List<PolicyAlert> policyAlerts;
    List<? extends Action> actions;

    // Evaluate the policy when developing
    policyAlerts = evaluate(new Stage(DevelopStageType.ID), policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    actions = policyAlerts.get(0).getActions();
    Assert.assertEquals(1, actions.size());
    Assert.assertEquals(WarnActionType.ID, actions.get(0).getActionTypeId());
    Assert.assertNull(actions.get(0).getTarget());

    // Evaluate the policy when building
    policyAlerts = evaluate(new Stage(BuildStageType.ID), policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    actions = policyAlerts.get(0).getActions();
    Assert.assertEquals(1, actions.size());
    Assert.assertEquals(FailActionType.ID, actions.get(0).getActionTypeId());
    Assert.assertNull(actions.get(0).getTarget());

    // Evaluate the policy when releasing
    policyAlerts = evaluate(new Stage(ReleaseStageType.ID), policy, components);

    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    actions = policyAlerts.get(0).getActions();
    Assert.assertEquals(1, actions.size());
    Assert.assertEquals(NotifyActionType.ID, actions.get(0).getActionTypeId());
    Assert.assertEquals("manager@some.com", actions.get(0).getTarget());
  }

  @Test
  public void testEvaluate_SortedAlerts() {
    final List<Policy> policies = new ArrayList<>();

    // randomly generate a series of policies
    for (int i = 0; i <= 25 * Math.random(); i++) {
      Policy policy = randomPolicy();
      DroolsGenerator.generate(policy);
      policies.add(policy);
    }

    final List<Component> components = new ArrayList<>();

    // A component with one security vulnerability
    final Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);

    // A component with Apache-2.0 license
    final Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // A component with one security vulnerability and Apache-2.0 license
    final Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv2", 3F));
    component3.addDeclaredLicenseId("Apache-2.0");
    components.add(component3);

    // Another component with one security vulnerability and Apache-2.0 license
    final Component component4 = ComponentFactory.forGav("g4", "a4", "v4", MatchState.EXACT);
    component4.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv4", 3F));
    component4.addDeclaredLicenseId("Apache-2.0");
    components.add(component4);

    // Evaluate the facts
    final List<MatchFact> facts = ComponentPolicyEvaluator.evaluateFacts(policies, components);

    // Sort facts by policy then component then constraint then condition
    Collections.sort(facts, ComponentPolicyEvaluator.MATCHES_BY_POLICY_COMPONENT_CONSTRAINT_CONDITION);
    final List<MatchFact> expectedFacts = new ArrayList<>(facts);

    // Check sorting is consistent
    for (int i = 0; i < 100; i++) {
      Collections.shuffle(facts);

      Collections.sort(facts, ComponentPolicyEvaluator.MATCHES_BY_POLICY_COMPONENT_CONSTRAINT_CONDITION);

      Assert.assertEquals(expectedFacts, facts);
    }

    // Slice facts into alerts
    PolicyResults policyResults = new PolicyResults();
    ComponentPolicyEvaluator.toPolicyResults(policies, facts, new Stage(BuildStageType.ID), false /* forMonitoring */,
        policyResults);
    final List<PolicyAlert> expectedAlerts = policyResults.getActiveAlerts();

    // Check slicing is consistent
    for (int i = 0; i < 100; i++) {
      Collections.shuffle(facts);
      Collections.shuffle(policies);

      policyResults = new PolicyResults();
      ComponentPolicyEvaluator.toPolicyResults(policies, facts, new Stage(BuildStageType.ID),
          false /* forMonitoring */, policyResults);
      final List<PolicyAlert> alerts = policyResults.getActiveAlerts();

      Assert.assertEquals(alertsToString(expectedAlerts), alertsToString(alerts));
    }
  }

  @Test
  public void testEvaluate_OrgAndAppPolicies() throws Exception {
    Organization org = tempEntity.newOrganization("testEvaluateOrgAndAppPolicies");
    Application app = tempEntity.newApplication("testEvaluateOrgAndAppPolicies", "testEvaluateOrgAndAppPolicies",
        org.getId());

    Stage stage = new Stage(BuildStageType.ID);

    // Create parent org policy
    List<Constraint> constraints = new ArrayList<>();
    Constraint constraintParentOrg = new Constraint(null, "Constraint Name Parent Org", LogicalOperator.AND);
    constraintParentOrg.addCondition(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    constraints.add(constraintParentOrg);
    Policy policyParentOrg = new Policy(null, "Policy Name Parent Org");
    policyParentOrg.setOwnerId(org.getParentOrganizationId());
    policyParentOrg.setConstraints(constraints);
    policyParentOrg.setAction(stage.getStageTypeId(), FailActionType.ID);
    tempEntity.newPolicy(policyParentOrg);

    // Create org policy
    constraints = new ArrayList<>();
    Constraint constraintOrg = new Constraint(null, "Constraint Name Org", LogicalOperator.AND);
    constraintOrg.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraints.add(constraintOrg);
    Policy policyOrg = new Policy(null, "Policy Name Org");
    policyOrg.setOwnerId(org.getId());
    policyOrg.setConstraints(constraints);
    policyOrg.setAction(stage.getStageTypeId(), FailActionType.ID);
    tempEntity.newPolicy(policyOrg);

    // Create app policy
    constraints = new ArrayList<>();
    Constraint constraintApp = new Constraint(null, "Constraint Name App", LogicalOperator.AND);
    constraintApp.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraintApp);
    Policy policyApp = new Policy(null, "Policy Name App");
    policyApp.setOwnerId(app.getId());
    policyApp.setConstraints(constraints);
    policyApp.setAction(stage.getStageTypeId(), FailActionType.ID);
    tempEntity.newPolicy(policyApp);

    List<Component> components = new ArrayList<>();
    // A component with one security vulnerability
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    components.add(component1);
    // A component with Apache-2.0 license
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);
    // A component with GPL-2.0 license
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.addDeclaredLicenseId("GPL-2.0");
    components.add(component3);

    // Evaluate the policies
    List<PolicyAlert> policyAlerts = componentPolicyEvaluator.evaluate(app.getId(), stage, components);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(3, policyAlerts.size());
    assertContainsPolicyAlert(component3, policyParentOrg, constraintParentOrg, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component1, policyOrg, constraintOrg, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policyApp, constraintApp, FailActionType.ID, LicenseConditionType.ID, policyAlerts);
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
      constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
      constraint.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    }
    else if (r >= 0.66) {
      constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    }
    else {
      constraint.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    }
    return constraint;
  }

  @Test
  public void testEvaluate_PolicyWaived() {
    Stage stage = new Stage(BuildStageType.ID);

    // Create an application
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    // Create two policies
    List<Constraint> constraints1 = new ArrayList<>();
    Constraint constraint1 = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraints1.add(constraint1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setOwnerId(app.getId());
    policy1.setConstraints(constraints1);
    policy1.setAction(stage.getStageTypeId(), FailActionType.ID);
    PolicyDAO policyDAO = new PolicyDAO();
    policyDAO.insert(policy1);
    List<Constraint> constraints2 = new ArrayList<>();
    Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraints2.add(constraint2);
    Policy policy2 = new Policy("PolicyId2", "Policy Name 2");
    policy2.setOwnerId(app.getId());
    policy2.setConstraints(constraints2);
    policy2.setAction(stage.getStageTypeId(), FailActionType.ID);
    policyDAO.insert(policy2);

    // Create two components
    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setHash("hash1");
    component1.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    component1.addDeclaredLicenseId("Apache-2.0");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setHash("hash2");
    component2.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv1", 3F));
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policies.
    // Both policies are violated by both components.
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), stage,
        Arrays.asList(policy1, policy2), components);
    List<PolicyAlert> activePolicyAlerts = policyResults.getActiveAlerts();
    Assert.assertNotNull(activePolicyAlerts);
    Assert.assertEquals(4, activePolicyAlerts.size());
    assertContainsPolicyAlert(component1, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component1, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertThat(policyResults.getWaivedAlerts(), hasSize(0));

    // Waive policy1 for component1 and re-evaluate
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", policy1.getId(), app.getId(), null /* comment */);
    policyResults = componentPolicyEvaluator.evaluate(app.getId(), stage, Arrays.asList(policy1, policy2), components);
    activePolicyAlerts = policyResults.getActiveAlerts();
    Assert.assertNotNull(activePolicyAlerts);
    Assert.assertEquals(3, activePolicyAlerts.size());
    assertContainsPolicyAlert(component2, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component1, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);

    List<PolicyAlert> waivedPolicyAlerts = policyResults.getWaivedAlerts();
    assertThat(waivedPolicyAlerts, notNullValue());
    assertThat(waivedPolicyAlerts, hasSize(1));
    assertContainsPolicyAlert(component1, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, waivedPolicyAlerts);
    assertThat(waivedPolicyAlerts.get(0).getTrigger().getComponentFacts(), hasSize(1));
    ComponentFact waivedComponentFact = waivedPolicyAlerts.get(0).getTrigger().getComponentFacts().get(0);
    assertThat(policyResults.getPolicyWaiver(waivedComponentFact).getId(), is(policyWaiver.getId()));
  }

  @Test
  public void testEvaluate_VulnerabilityConditions_Conjunction() {
    Constraint constraint = new Constraint("cid", "CVSS >= 7 and <= 9", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7"));
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, "<=", "9"));
    constraint.addCondition(new Condition(SecurityVulnerabilityStatusConditionType.ID, "is not",
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE.getId()));

    Policy policy = new Policy("pid", "Security-High");
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setHash("12345678901234567890");
    component1.addSecurityVulnerability(new SecurityVulnerability("cve", "CVE-1234-1234", 8.0f,
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE));
    component1.addSecurityVulnerability(new SecurityVulnerability("cve", "CVE-1234-1234", 4.0f));

    Component component2 = ComponentFactory.forGav("g1", "a1", "v2", MatchState.EXACT);
    component1.setHash("12345678901234567891");
    component2.addSecurityVulnerability(new SecurityVulnerability("cve", "CVE-1234-1234", 8.0f));

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(component1, component2));
    assertThat(policyAlerts, hasSize(1));
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, SecurityVulnerabilityStatusConditionType.ID, policyAlerts);
  }

  @Test
  public void testEvaluate_VulnerabilityConditions_Disjunction() {
    Constraint constraint = new Constraint("cid", "cname", LogicalOperator.OR);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7"));
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, "<=", "9"));
    constraint.addCondition(new Condition(SecurityVulnerabilityStatusConditionType.ID, "is not",
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE.getId()));

    Policy policy = new Policy("pid", "pname");
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setHash("12345678901234567890");
    SecurityVulnerability sv1 = new SecurityVulnerability("cve", "CVE-1234-1234", 8.0f,
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE);
    SecurityVulnerability sv2 = new SecurityVulnerability("cve", "CVE-1234-1234", 4.0f);
    component1.addSecurityVulnerability(sv1);
    component1.addSecurityVulnerability(sv2);

    Component component2 = ComponentFactory.forGav("g1", "a1", "v2", MatchState.EXACT);
    component2.setHash("12345678901234567891");
    SecurityVulnerability sv3 = new SecurityVulnerability("cve", "CVE-1234-1234", 8.0f);
    component2.addSecurityVulnerability(sv3);

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(component1, component2));
    assertThat(policyAlerts, hasSize(7));
    for (PolicyAlert policyAlert : policyAlerts) {
      assertFactCounts(1, 1, policyAlert);
    }
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, newConditionTriggerWithSeverity(0, sv1), policyAlerts);
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, newConditionTriggerWithSeverity(1, sv1), policyAlerts);
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, newConditionTriggerWithSeverity(1, sv2), policyAlerts);
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        SecurityVulnerabilityStatusConditionType.ID, newConditionTriggerWithStatus(2, sv2), policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, newConditionTriggerWithSeverity(0, sv3), policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, newConditionTriggerWithSeverity(1, sv3), policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        SecurityVulnerabilityStatusConditionType.ID, newConditionTriggerWithStatus(2, sv3), policyAlerts);
  }

  private ConditionTrigger newConditionTriggerWithSeverity(int conditionIndex,
                                                           SecurityVulnerability securityVulnerability)
  {
    return new ConditionTrigger(conditionIndex, new TriggerSecurityVulnerabilityWithSeverity(securityVulnerability));

  }

  private ConditionTrigger newConditionTriggerWithStatus(int conditionIndex,
                                                         SecurityVulnerability securityVulnerability)
  {
    return new ConditionTrigger(conditionIndex, new TriggerSecurityVulnerabilityWithStatus(securityVulnerability));

  }
}
