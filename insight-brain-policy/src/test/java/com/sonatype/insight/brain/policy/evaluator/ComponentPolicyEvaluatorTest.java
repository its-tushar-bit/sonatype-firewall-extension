/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.repository.RepositoryType;
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
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
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
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.json.store.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class ComponentPolicyEvaluatorTest
    extends AbstractPolicyEvaluationTest
{
  private PolicyWaiverDAO policyWaiverDAO;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
  }

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

    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getComponentFacts()).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getDisplayName().toString())
        .isEqualTo("g : a : v");
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

    assertThat(policyAlerts).hasSize(2);
    assertThat(policyAlerts.get(0).getTrigger().getComponentFacts()).hasSize(1);
    assertThat(policyAlerts.get(1).getTrigger().getComponentFacts()).hasSize(1);

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint2, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);
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
    assertThat(policyAlerts).isEmpty();
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

    assertThat(policyAlerts).hasSize(1);
    List<ComponentFact> componentFacts = policyAlerts.get(0).getTrigger().getComponentFacts();
    assertThat(componentFacts).hasSize(1);
    assertThat(componentFacts.get(0).getConstraintFacts()).hasSize(1);

    assertContainsPolicyAlert(component1, policy, constraint2, FailActionType.ID, MatchStateConditionType.ID,
        policyAlerts);
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

    assertThat(policyAlerts).hasSize(1);
    List<ComponentFact> componentFacts = policyAlerts.get(0).getTrigger().getComponentFacts();
    assertThat(componentFacts).hasSize(1);
    assertThat(componentFacts.get(0).getConstraintFacts()).hasSize(1);

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID, MatchStateConditionType.ID,
        policyAlerts);
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

    assertThat(policyAlerts).isEmpty();

    // A component with Apache-2.0 license
    final Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    assertThat(policyAlerts).isEmpty();

    // A component with one security vulnerability and Apache-2.0 license
    final Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv2", 3F));
    component3.addDeclaredLicenseId("Apache-2.0");
    components.add(component3);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getComponentFacts()).hasSize(1);

    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);

    // Another component with one security vulnerability and Apache-2.0 license
    final Component component4 = ComponentFactory.forGav("g4", "a4", "v4", MatchState.EXACT);
    component4.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv4", 3F));
    component4.addDeclaredLicenseId("Apache-2.0");
    components.add(component4);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    assertThat(policyAlerts).hasSize(2);
    assertThat(policyAlerts.get(0).getTrigger().getComponentFacts()).hasSize(1);
    assertThat(policyAlerts.get(1).getTrigger().getComponentFacts()).hasSize(1);

    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(component4, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);
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

    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getComponentFacts()).hasSize(1);

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);

    // A component with Apache-2.0 license
    final Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    assertThat(policyAlerts).hasSize(2);
    assertThat(policyAlerts.get(0).getTrigger().getComponentFacts()).hasSize(1);
    assertThat(policyAlerts.get(1).getTrigger().getComponentFacts()).hasSize(1);

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);

    // A component with one security vulnerability and Apache-2.0 license
    final Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    component3.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv2", 3F));
    component3.addDeclaredLicenseId("Apache-2.0");
    components.add(component3);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    assertThat(policyAlerts).hasSize(4);
    for (PolicyAlert policyAlert : policyAlerts) {
      assertThat(policyAlert.getTrigger().getComponentFacts()).hasSize(1);
    }

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);

    // Another component with one security vulnerability and Apache-2.0 license
    final Component component4 = ComponentFactory.forGav("g4", "a4", "v4", MatchState.EXACT);
    component4.addSecurityVulnerability(new SecurityVulnerability("osvdb", "sv4", 3F));
    component4.addDeclaredLicenseId("Apache-2.0");
    components.add(component4);

    // Evaluate the policy
    policyAlerts = evaluate(stage, policy, components);

    assertThat(policyAlerts).hasSize(6);
    for (PolicyAlert policyAlert : policyAlerts) {
      assertThat(policyAlert.getTrigger().getComponentFacts()).hasSize(1);
    }

    assertContainsPolicyAlert(component1, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);
    assertContainsPolicyAlert(component4, policy, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component4, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);
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

    assertThat(policyAlerts).hasSize(1);
    actions = policyAlerts.get(0).getActions();
    assertThat(actions).hasSize(1);
    assertThat(actions.get(0).getActionTypeId()).isEqualTo(WarnActionType.ID);
    assertThat(actions.get(0).getTarget()).isNull();

    // Evaluate the policy when building
    policyAlerts = evaluate(new Stage(BuildStageType.ID), policy, components);

    assertThat(policyAlerts).hasSize(1);
    actions = policyAlerts.get(0).getActions();
    assertThat(actions).hasSize(1);
    assertThat(actions.get(0).getActionTypeId()).isEqualTo(FailActionType.ID);
    assertThat(actions.get(0).getTarget()).isNull();

    // Evaluate the policy when releasing
    policyAlerts = evaluate(new Stage(ReleaseStageType.ID), policy, components);

    assertThat(policyAlerts).hasSize(1);
    actions = policyAlerts.get(0).getActions();
    assertThat(actions).hasSize(1);
    assertThat(actions.get(0).getActionTypeId()).isEqualTo(NotifyActionType.ID);
    assertThat(actions.get(0).getTarget()).isEqualTo("manager@some.com");
  }

  @Test
  public void testEvaluate_SortedAlerts() {
    final List<Policy> policies = new ArrayList<>();

    // randomly generate a series of policies
    for (int i = 0; i <= 25 * Math.random(); i++) {
      Policy policy = randomPolicy();
      DroolsGenerator.generate(policy, labelDAO);
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
    final List<MatchFact> matchFacts = ComponentPolicyEvaluator.evaluateFacts(policies, components);
    List<PolicyFact> policyFacts = ComponentPolicyEvaluator.toPolicyFacts(policies, matchFacts);

    // Sort facts by policy then component then constraint then condition
    policyFacts.sort(ComponentPolicyEvaluator.POLICY_FACT_COMPARATOR);
    final List<PolicyFact> expectedPolicyFacts = new ArrayList<>(policyFacts);

    // Check sorting is consistent
    for (int i = 0; i < 100; i++) {
      Collections.shuffle(policyFacts);

      policyFacts.sort(ComponentPolicyEvaluator.POLICY_FACT_COMPARATOR);

      assertThat(policyFacts).isEqualTo(expectedPolicyFacts);
    }

    // Convert facts into alerts
    PolicyResults policyResults = componentPolicyEvaluator.toPolicyResults(null /* ownerId */, policies, policyFacts,
        new Stage(BuildStageType.ID), false /* forMonitoring */);
    final List<PolicyAlert> expectedAlerts = policyResults.getActiveAlerts();

    // Check alerts are consistent with the policy facts
    for (int i = 0; i < 100; i++) {
      Collections.shuffle(policyFacts);
      Collections.shuffle(policies);

      policyResults = componentPolicyEvaluator.toPolicyResults(null /* ownerId */, policies, policyFacts,
          new Stage(BuildStageType.ID), false /* forMonitoring */);
      final List<PolicyAlert> alerts = policyResults.getActiveAlerts();

      assertThat(alertsToString(alerts)).isEqualTo(alertsToString(expectedAlerts));
    }
  }

  @Test
  public void testEvaluate_OrgAndAppPolicies() {
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
    assertThat(policyAlerts).hasSize(3);
    assertContainsPolicyAlert(component3, policyParentOrg, constraintParentOrg, FailActionType.ID,
        LicenseConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component1, policyOrg, constraintOrg, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policyApp, constraintApp, FailActionType.ID, LicenseConditionType.ID,
        policyAlerts);
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
    tempEntity.newPolicy(policy1);
    List<Constraint> constraints2 = new ArrayList<>();
    Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraints2.add(constraint2);
    Policy policy2 = new Policy("PolicyId2", "Policy Name 2");
    policy2.setOwnerId(app.getId());
    policy2.setConstraints(constraints2);
    policy2.setAction(stage.getStageTypeId(), FailActionType.ID);
    tempEntity.newPolicy(policy2);

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
    assertThat(activePolicyAlerts).hasSize(4);
    assertContainsPolicyAlert(component1, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component1, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertThat(policyResults.getWaivedAlerts()).isEmpty();

    // Waive the alert for policy1 and component1 and re-evaluate
    PolicyAlert policyAlertToWaive = findPolicyAlert(activePolicyAlerts, component1, policy1);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", policy1.getId(), app.getId(),
        policyAlertToWaive.getTrigger().getComponentFacts().get(0).getConstraintFacts());
    policyResults = componentPolicyEvaluator.evaluate(app.getId(), stage, Arrays.asList(policy1, policy2), components);
    activePolicyAlerts = policyResults.getActiveAlerts();
    assertThat(activePolicyAlerts).hasSize(3);
    assertContainsPolicyAlert(component2, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component1, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);

    List<PolicyAlert> waivedPolicyAlerts = policyResults.getWaivedAlerts();
    assertThat(waivedPolicyAlerts).hasSize(1);
    assertContainsPolicyAlert(component1, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, waivedPolicyAlerts);
    assertThat(waivedPolicyAlerts.get(0).getTrigger().getComponentFacts()).hasSize(1);
    ComponentFact waivedComponentFact = waivedPolicyAlerts.get(0).getTrigger().getComponentFacts().get(0);
    assertThat(policyResults.getPolicyWaiver(waivedComponentFact).getId()).isEqualTo(policyWaiver.getId());
  }

  @Test
  public void testEvaluate_PolicyWaivedForContainerImage() {
    Stage stage = new Stage(ProxyStageType.ID);

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
    tempEntity.newPolicy(policy1);
    List<Constraint> constraints2 = new ArrayList<>();
    Constraint constraint2 = new Constraint("ConstraintId2", "Constraint Name 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraints2.add(constraint2);
    Policy policy2 = new Policy("PolicyId2", "Policy Name 2");
    policy2.setOwnerId(app.getId());
    policy2.setConstraints(constraints2);
    policy2.setAction(stage.getStageTypeId(), FailActionType.ID);
    tempEntity.newPolicy(policy2);

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
    PolicyResults policyResults =
        componentPolicyEvaluator.evaluate(app.getId(), stage, Arrays.asList(policy1, policy2), components);
    List<PolicyAlert> activePolicyAlerts = policyResults.getActiveAlerts();
    assertThat(activePolicyAlerts).hasSize(4);
    assertContainsPolicyAlert(component1, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component1, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertThat(policyResults.getWaivedAlerts()).isEmpty();

    // Waive the alert for policy1 and component1 and re-evaluate
    PolicyAlert policyAlertToWaive = findPolicyAlert(activePolicyAlerts, component1, policy1);
    PolicyWaiver policyWaiverForContainerImageComponent = tempEntity.newWaiver("hash1", policy1.getId(), app.getId(),
        policyAlertToWaive.getTrigger().getComponentFacts().get(0).getConstraintFacts());
    policyWaiverForContainerImageComponent.setForContainerImageComponent(true);
    policyWaiverForContainerImageComponent.setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);
    policyWaiverDAO.update(policyWaiverForContainerImageComponent);

    // The policy waiver for the container image should not be processed
    PolicyWaiver policyWaiverForContainerImage = tempEntity.newWaiver(null, policy1.getId(), app.getId(),
        policyAlertToWaive.getTrigger().getComponentFacts().get(0).getConstraintFacts());
    policyWaiverForContainerImage.setForContainerImage(true);
    policyWaiverForContainerImageComponent.setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);
    policyWaiverDAO.update(policyWaiverForContainerImage);

    policyResults = componentPolicyEvaluator.evaluate(app.getId(), stage, Arrays.asList(policy1, policy2), components);
    activePolicyAlerts = policyResults.getActiveAlerts();

    assertThat(activePolicyAlerts).hasSize(3);
    assertContainsPolicyAlert(component2, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component1, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);
    assertContainsPolicyAlert(component2, policy2, constraint2, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, activePolicyAlerts);

    List<PolicyAlert> waivedPolicyAlerts = policyResults.getWaivedAlerts();

    assertThat(waivedPolicyAlerts).hasSize(1);
    assertContainsPolicyAlert(component1, policy1, constraint1, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, waivedPolicyAlerts);
    assertThat(waivedPolicyAlerts.get(0).getTrigger().getComponentFacts()).hasSize(1);
    ComponentFact waivedComponentFact = waivedPolicyAlerts.get(0).getTrigger().getComponentFacts().get(0);
    assertThat(policyResults.getPolicyWaiver(waivedComponentFact).getId())
        .isEqualTo(policyWaiverForContainerImageComponent.getId());
  }

  private PolicyAlert findPolicyAlert(List<PolicyAlert> policyAlerts, Component component, Policy policy) {
    return policyAlerts.stream()
        .filter( //
            policyAlert -> policyAlert.getTrigger()
                .getComponentFacts()
                .get(0)
                .getComponentIdentifier()
                .equals(component.getComponentIdentifier()) && //
                policyAlert.getTrigger().getPolicyId().equals(policy.getId()))
        .findFirst()
        .get();
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
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        SecurityVulnerabilityStatusConditionType.ID, policyAlerts);
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
    assertThat(policyAlerts).hasSize(7);
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

  @Test
  public void testEvaluate_ConditionFacts_Conjunction() {
    Constraint constraint = new Constraint("cid", "CVSS >= 7 and <= 9", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7"));
    constraint.addCondition(new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityOverrideStatus.OPEN.getId()));

    Policy policy = new Policy("pid", "Security-High");
    policy.addConstraint(constraint);

    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component.setHash("12345678901234567890");
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve", "CVE-1234-1234", 8.0f,
        SecurityVulnerabilityOverrideStatus.OPEN);
    component.addSecurityVulnerability(securityVulnerability);

    List<PolicyAlert> policyAlerts = evaluate(policy, Collections.singletonList(component));
    assertThat(policyAlerts).hasSize(1);
    PolicyAlert policyAlert = policyAlerts.get(0);
    assertFactCounts(1, 1, policyAlert);
    List<ConditionFact> conditionFacts = policyAlert.getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts();
    assertThat(conditionFacts).hasSize(2);
    assertConditionFact(conditionFacts.get(0), 0, SecurityVulnerabilitySeverityConditionType.ID,
        "Found security vulnerability CVE-1234-1234 with severity >= 7 (severity = 8.0)",
        "Security Vulnerability Severity >= 7",
        newConditionTriggerWithSeverity(0, securityVulnerability),
        new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, "CVE-1234-1234"));
    assertConditionFact(conditionFacts.get(1), 1, SecurityVulnerabilityStatusConditionType.ID,
        "Found security vulnerability CVE-1234-1234 with status 'Open'", "Security Vulnerability Status is OPEN",
        newConditionTriggerWithStatus(1, securityVulnerability),
        new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, "CVE-1234-1234"));
  }

  @Test
  public void testEvaluate_ConditionFacts_Disjunction() {
    Constraint constraint = new Constraint("cid", "CVSS >= 7 and <= 9", LogicalOperator.OR);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7"));
    constraint.addCondition(new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityOverrideStatus.OPEN.getId()));

    Policy policy = new Policy("pid", "Security-High");
    policy.addConstraint(constraint);

    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component.setHash("12345678901234567890");
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve", "CVE-1234-1234", 8.0f,
        SecurityVulnerabilityOverrideStatus.OPEN);
    component.addSecurityVulnerability(securityVulnerability);

    List<PolicyAlert> policyAlerts = evaluate(policy, Collections.singletonList(component));
    assertThat(policyAlerts).hasSize(2);
    PolicyAlert policyAlert1 = policyAlerts.get(0);
    assertFactCounts(1, 1, policyAlert1);
    List<ConditionFact> conditionFacts1 = policyAlert1.getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts();
    assertThat(conditionFacts1).hasSize(1);
    assertConditionFact(conditionFacts1.get(0), 0, SecurityVulnerabilitySeverityConditionType.ID,
        "Found security vulnerability CVE-1234-1234 with severity >= 7 (severity = 8.0)",
        "Security Vulnerability Severity >= 7",
        newConditionTriggerWithSeverity(0, securityVulnerability),
        new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, "CVE-1234-1234"));

    PolicyAlert policyAlert2 = policyAlerts.get(1);
    assertFactCounts(1, 1, policyAlert2);
    List<ConditionFact> conditionFacts2 = policyAlert2.getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts();
    assertThat(conditionFacts2).hasSize(1);
    assertConditionFact(conditionFacts2.get(0), 1, SecurityVulnerabilityStatusConditionType.ID,
        "Found security vulnerability CVE-1234-1234 with status 'Open'", "Security Vulnerability Status is OPEN",
        newConditionTriggerWithStatus(1, securityVulnerability),
        new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, "CVE-1234-1234"));
  }

  @Test
  public void testEvaluate_PolicyWaiverIsExpired() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    DateTime now = DateTime.now();

    Component component1 = new Component(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    String hash1 = "hash1";
    component1.setHash(hash1);
    component1.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));
    Component component2 = new Component(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    String hash2 = "hash2";
    component2.setHash(hash2);
    component2.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));

    // expired waiver
    tempEntity.newWaiver(hash1, policy.getId(), app.getId(), null, "comment", now.toDate(), now.toDate());

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Lists.newArrayList(component1, component2), false);

    assertThat(policyResults.getWaivedAlerts()).isEmpty();
    assertThat(policyResults.getActiveAlerts()).hasSize(2);
    assertThat(policyResults.getActiveAlerts().get(0).getTrigger().getComponentFacts().get(0).getHash())
        .isEqualTo(hash1);
    assertThat(policyResults.getActiveAlerts().get(1).getTrigger().getComponentFacts().get(0).getHash())
        .isEqualTo(hash2);
  }

  @Test
  public void testEvaluate_PolicyWaiverIsExpiringInFuture() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    DateTime now = DateTime.now();

    Component component1 = new Component(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    String hash1 = "hash1";
    component1.setHash(hash1);
    component1.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));
    Component component2 = new Component(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    String hash2 = "hash2";
    component2.setHash(hash2);
    component2.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));

    PolicyWaiver policyWaiver = tempEntity.newWaiver(hash1, policy.getId(), app.getId(), null, "comment",
        now.toDate(), now.plusHours(1).toDate()); // expiring in future

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Lists.newArrayList(component1, component2), false);

    assertThat(policyResults.getWaivedAlerts()).hasSize(1);
    assertThat(policyResults.getWaivedAlerts().get(0).getTrigger().getComponentFacts().get(0).getHash())
        .isEqualTo(hash1);
    assertThat(policyResults
        .getPolicyWaiver(policyResults.getWaivedAlerts().get(0).getTrigger().getComponentFacts().get(0))
        .getId())
            .isEqualTo(policyWaiver.getId());
    assertThat(policyResults.getActiveAlerts()).hasSize(1);
    assertThat(policyResults.getActiveAlerts().get(0).getTrigger().getComponentFacts().get(0).getHash())
        .isEqualTo(hash2);
  }

  @Test
  public void testEvaluate_PolicyWaiverForSpecificComponent() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    Component component1 = new Component(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    String hash1 = "hash1";
    component1.setHash(hash1);
    component1.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));
    Component component2 = new Component(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    String hash2 = "hash2";
    component2.setHash(hash2);
    component2.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));

    PolicyWaiver policyWaiver = tempEntity.newWaiver(hash1, policy.getId(), app.getId());

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Lists.newArrayList(component1, component2), false);

    assertThat(policyResults.getWaivedAlerts()).hasSize(1);
    assertThat(policyResults.getWaivedAlerts().get(0).getTrigger().getComponentFacts().get(0).getHash())
        .isEqualTo(hash1);
    assertThat(policyResults
        .getPolicyWaiver(policyResults.getWaivedAlerts().get(0).getTrigger().getComponentFacts().get(0))
        .getId())
            .isEqualTo(policyWaiver.getId());
    assertThat(policyResults.getActiveAlerts()).hasSize(1);
    assertThat(policyResults.getActiveAlerts().get(0).getTrigger().getComponentFacts().get(0).getHash())
        .isEqualTo(hash2);
  }

  @Test
  public void testEvaluate_PolicyWaiverNotForSpecificComponent() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    Component component1 = new Component(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    String hash1 = "hash1";
    component1.setHash(hash1);
    component1.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));
    Component component2 = new Component(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    String hash2 = "hash2";
    component2.setHash(hash2);
    component2.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));

    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), app.getId());

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Lists.newArrayList(component1, component2), false);

    assertThat(policyResults.getWaivedAlerts()).hasSize(2);
    PolicyAlert waivedAlert1 = policyResults.getWaivedAlerts().get(0);
    assertThat(waivedAlert1.getTrigger().getComponentFacts().get(0).getHash()).isEqualTo(hash1);
    assertThat(policyResults.getPolicyWaiver(waivedAlert1.getTrigger().getComponentFacts().get(0)).getId())
        .isEqualTo(policyWaiver.getId());
    PolicyAlert waivedAlert2 = policyResults.getWaivedAlerts().get(1);
    assertThat(waivedAlert2.getTrigger().getComponentFacts().get(0).getHash()).isEqualTo(hash2);
    assertThat(policyResults.getPolicyWaiver(waivedAlert2.getTrigger().getComponentFacts().get(0)).getId())
        .isEqualTo(policyWaiver.getId());
    assertThat(policyResults.getActiveAlerts()).isEmpty();
  }

  @Test
  public void testEvaluate_PolicyWaiver_InheritedFromOrganization() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(org);

    Component component = new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    String hash = "hash";
    component.setHash(hash);
    component.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), org.getId());

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Collections.singletonList(component), false);

    assertThat(policyResults.getWaivedAlerts()).hasSize(1);
    PolicyAlert waivedAlert = policyResults.getWaivedAlerts().get(0);
    assertThat(waivedAlert.getTrigger().getComponentFacts().get(0).getHash()).isEqualTo(hash);
    assertThat(policyResults.getPolicyWaiver(waivedAlert.getTrigger().getComponentFacts().get(0)).getId())
        .isEqualTo(policyWaiver.getId());
    assertThat(policyResults.getActiveAlerts()).isEmpty();
  }

  @Test
  public void testEvaluate_PolicyWaiver_InheritedFromRootOrganization() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(org.getParentOrganizationId());

    Component component = new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    String hash = "hash";
    component.setHash(hash);
    component.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), org.getParentOrganizationId());

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Collections.singletonList(component), false);

    assertThat(policyResults.getWaivedAlerts()).hasSize(1);
    PolicyAlert waivedAlert = policyResults.getWaivedAlerts().get(0);
    assertThat(waivedAlert.getTrigger().getComponentFacts().get(0).getHash()).isEqualTo(hash);
    assertThat(policyResults.getPolicyWaiver(waivedAlert.getTrigger().getComponentFacts().get(0)).getId())
        .isEqualTo(policyWaiver.getId());
    assertThat(policyResults.getActiveAlerts()).isEmpty();
  }

  @Test
  public void testEvaluate_LegacyPolicyWaiver() {
    // Before Brain 1.53, policy waivers did not store the constraint facts from the policy alert that was waived.
    // Although there are other tests that use legacy waivers, I added this explicit test for legacy waivers, just in
    // case the other tests are updated to use new waivers.
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    Component component = new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    String hash = "hash";
    component.setHash(hash);
    component.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));

    PolicyWaiver policyWaiver = tempEntity.newWaiver(hash, policy.getId(), app.getId());

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Collections.singletonList(component), false);

    assertThat(policyResults.getWaivedAlerts()).hasSize(1);
    assertThat(policyResults.getWaivedAlerts().get(0).getTrigger().getComponentFacts().get(0).getHash())
        .isEqualTo(hash);
    assertThat(policyResults
        .getPolicyWaiver(policyResults.getWaivedAlerts().get(0).getTrigger().getComponentFacts().get(0))
        .getId())
            .isEqualTo(policyWaiver.getId());
  }

  @Test
  public void testEvaluate_PolicyWaiverWithConstraintFactsArePreferred() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    Component component = new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    String hash = "hash";
    component.setHash(hash);
    component.addSecurityVulnerability(new SecurityVulnerability("source", "refId", 5F));

    PolicyWaiver policyWaiverWithoutConstraintFacts = tempEntity.newWaiver(hash, policy.getId(), app.getId());

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Collections.singletonList(component), false);
    assertThat(policyResults.getWaivedAlerts()).hasSize(1);
    assertThat(policyResults
        .getPolicyWaiver(policyResults.getWaivedAlerts().get(0).getTrigger().getComponentFacts().get(0))
        .getId())
            .isEqualTo(policyWaiverWithoutConstraintFacts.getId());

    List<ConstraintFact> constraintFacts = policyResults.getWaivedAlerts()
        .get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts();
    PolicyWaiver policyWaiverWithConstraintFacts = tempEntity.newWaiver(hash, policy.getId(), app.getId(),
        constraintFacts);
    policyResults = componentPolicyEvaluator.evaluate(app.getId(), new Stage(BuildStageType.ID),
        Collections.singletonList(component), false);
    assertThat(policyResults.getWaivedAlerts()).hasSize(1);
    assertThat(policyResults
        .getPolicyWaiver(policyResults.getWaivedAlerts().get(0).getTrigger().getComponentFacts().get(0))
        .getId())
            .isEqualTo(policyWaiverWithConstraintFacts.getId());
  }

  private void assertConditionFact(
      ConditionFact actual,
      int expectedConditionIndex,
      String expectedConditionTypeId,
      String expectedReason,
      String expectedSummary,
      ConditionTrigger expectedConditionTrigger,
      TriggerReference expectedTriggerReference)
  {
    assertThat(actual.getConditionIndex()).isEqualTo(expectedConditionIndex);
    assertThat(actual.getConditionTypeId()).isEqualTo(expectedConditionTypeId);
    assertThat(actual.getReason()).isEqualTo(expectedReason);
    assertThat(actual.getSummary()).isEqualTo(expectedSummary);
    assertThat(actual.getTriggerJson()).isEqualTo(JsonUtils.writeUnformatted(expectedConditionTrigger))
        .doesNotContain("\n", "\r", "\\n", "\\r");

    if (expectedTriggerReference == null) {
      assertThat(actual.getReference()).isNull();
    }
    else {
      assertThat(actual.getReference().getType()).isEqualTo(expectedTriggerReference.getType());
      assertThat(actual.getReference().getValue()).isEqualTo(expectedTriggerReference.getValue());
    }
  }

  private ConditionTrigger newConditionTriggerWithSeverity(
      int conditionIndex,
      SecurityVulnerability securityVulnerability)
  {
    return new ConditionTrigger(conditionIndex, new TriggerSecurityVulnerabilityWithSeverity(securityVulnerability));

  }

  private ConditionTrigger newConditionTriggerWithStatus(
      int conditionIndex,
      SecurityVulnerability securityVulnerability)
  {
    return new ConditionTrigger(conditionIndex, new TriggerSecurityVulnerabilityWithStatus(securityVulnerability));
  }

  @Test
  public void testToPolicyResults_ActionsFromApplicationHierarchyWithoutRelatedRepository() {
    Organization organization = tempEntity.newOrganization("org-without-repo");
    Application app = tempEntity.newApplicationWithParent(organization);

    Policy policy = tempEntity.newPolicy(organization);
    policy.setAction(Stage.ID_PROXY, Action.ID_WARN);
    Constraint constraint = new Constraint("ConstraintId", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7"));
    policy.addConstraint(constraint);

    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    List<PolicyFact> policyFacts = Collections.singletonList(policyFact);

    PolicyResults results = componentPolicyEvaluator.toPolicyResults(app.getId(), Collections.singletonList(policy),
        policyFacts, new Stage(ProxyStageType.ID), false);

    assertThat(results.getActiveAlerts()).hasSize(1);
    PolicyAlert alert = results.getActiveAlerts().get(0);
    assertThat(alert.getActions()).hasSize(1);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_WARN);
  }

  @Test
  public void testToPolicyResults_ActionsFromRelatedRepositoryHierarchy() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository dockerProxyRepo =
        tempEntity.newRepository(repositoryManager, "docker-proxy-repo", RepositoryType.proxy, "docker");

    Organization orgWithRepo = tempEntity.newOrganization("org-with-docker-repo");
    orgWithRepo.setRelatedRepositoryId(dockerProxyRepo.getId());

    Application app = tempEntity.newApplicationWithParent(orgWithRepo);

    Policy repositoryPolicy = tempEntity.newPolicy(repositoryManager);
    repositoryPolicy.setAction(Stage.ID_PROXY, Action.ID_FAIL);
    Constraint constraint = new Constraint("ConstraintId", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7"));
    repositoryPolicy.addConstraint(constraint);

    PolicyFact policyFact =
        new PolicyFact(repositoryPolicy.getId(), repositoryPolicy.getName(), repositoryPolicy.getThreatLevel());
    List<PolicyFact> policyFacts = Collections.singletonList(policyFact);

    PolicyResults results =
        componentPolicyEvaluator.toPolicyResults(app.getId(), Collections.singletonList(repositoryPolicy),
            policyFacts, new Stage(ProxyStageType.ID), false);

    assertThat(results.getActiveAlerts()).hasSize(1);
    PolicyAlert alert = results.getActiveAlerts().get(0);
    assertThat(alert.getActions()).hasSize(1);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }
}
