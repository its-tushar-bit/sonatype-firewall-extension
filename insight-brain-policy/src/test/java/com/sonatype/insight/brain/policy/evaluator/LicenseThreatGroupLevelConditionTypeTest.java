/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
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
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerLicenseThreatGroupWithThreatLevel;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LicenseThreatGroupLevelConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private ComponentLoaderFactory componentLoaderFactory;

  private Organization org;

  private Application app;

  private LicenseThreatGroup licenseThreatGroup2;

  private LicenseThreatGroup licenseThreatGroup5;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();
    licenseThreatGroupLicenseDAO = daoFactory.createLicenseThreatGroupLicenseDAO();
    componentLoaderFactory = new ComponentLoaderFactory(
        daoFactory.createMultiLicenseDAO(),
        licenseThreatGroupDAO,
        licenseThreatGroupLicenseDAO,
        daoFactory.createLicenseOverrideDAO(),
        daoFactory.createSecurityVulnerabilityOverrideDAO(),
        daoFactory.createOwnerDAO(),
        daoFactory.createComponentLabelDAO(),
        daoFactory.createVulnerabilityCustomRemediationDAO(),
        daoFactory.createVulnerabilityCustomCweDAO(),
        daoFactory.createVulnerabilityCustomCvssVectorDAO(),
        daoFactory.createVulnerabilityCustomCvssSeverityDAO(),
        daoFactory.createVulnerabilityGroupDAO(),
        daoFactory.createVulnerabilityGroupVulnerabilityDAO());
    before();
  }

  public void before() {
    org = tempEntity.newOrganization("LicenseThreatGroupLevelConditionTypeTest");
    app = tempEntity.newApplication("test", "LicenseThreatGroupLevelConditionTypeTest_AppId", org.getId());

    licenseThreatGroup2 = new LicenseThreatGroup(app.getId(), "Level 2", 2);
    licenseThreatGroup2.setId("LTG2");
    licenseThreatGroupDAO.insert(licenseThreatGroup2);
    licenseThreatGroup5 = new LicenseThreatGroup(app.getId(), "Level 5", 5);
    licenseThreatGroup5.setId("LTG5");
    licenseThreatGroupDAO.insert(licenseThreatGroup5);
  }

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", LicenseThreatGroupLevelConditionType.ID, operator,
        value);
  }

  @Test
  public void testEvaluateLessOrEqual() {
    // Create policy constraints
    Constraint constraint = createConstraint("<=", "2");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addLicenseThreatGroup(licenseThreatGroup2);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addLicenseThreatGroup(licenseThreatGroup5);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroupWithThreatLevel(licenseThreatGroup2));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        LicenseThreatGroupLevelConditionType.ID, expectedConditionTrigger, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Found license threat group 'Level 2' with level <= 2 (level = 2)");
  }

  @Test
  public void testEvaluateGreaterOrEqual() {
    // Create policy constraints
    Constraint constraint = createConstraint(">=", "5");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addLicenseThreatGroup(licenseThreatGroup2);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addLicenseThreatGroup(licenseThreatGroup5);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroupWithThreatLevel(licenseThreatGroup5));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        LicenseThreatGroupLevelConditionType.ID, expectedConditionTrigger, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Found license threat group 'Level 5' with level >= 5 (level = 5)");
  }

  @Test
  public void testEvaluateRange() {
    // Create policy constraint
    Constraint constraint = new Constraint("Test-Contraint", "Range of Levels", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseThreatGroupLevelConditionType.ID, ">=", "3"));
    constraint.addCondition(new Condition(LicenseThreatGroupLevelConditionType.ID, "<=", "8"));

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addLicenseThreatGroup(licenseThreatGroup2);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addLicenseThreatGroup(licenseThreatGroup5);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroupWithThreatLevel(licenseThreatGroup5));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        LicenseThreatGroupLevelConditionType.ID, expectedConditionTrigger, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Found license threat group 'Level 5' with level >= 3 (level = 5)");
    actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(1)
        .getReason();
    assertThat(actualReason).isEqualTo("Found license threat group 'Level 5' with level <= 8 (level = 5)");
  }

  @Test
  public void testValidateCondition_InvalidLicenseThreatGroupLevel() {
    Condition condition = new Condition(LicenseThreatGroupLevelConditionType.ID, "<=", "abc");
    assertThatThrownBy(() -> new LicenseThreatGroupLevelConditionType().validateCondition(null, condition, app.getId()))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("Invalid license threat group level: abc");
  }

  @Test
  public void testEvaluate_LicenseThreatGroupFromOrganization() {
    LicenseThreatGroup orgLicenseThreatGroup = new LicenseThreatGroup(org.getId(),
        "testEvaluate-LicenseThreatGroupFromOrganization", 7);
    licenseThreatGroupDAO.insert(orgLicenseThreatGroup);
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense(org.getId(),
        orgLicenseThreatGroup.getId(), "Apache-2.0");
    licenseThreatGroupLicenseDAO.insert(licenseThreatGroupLicense);

    // Create policy constraints
    Constraint constraint = createConstraint(">=", "7");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(app);
    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    componentLoader.loadLicenseThreatGroups(component1);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroupWithThreatLevel(orgLicenseThreatGroup));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        LicenseThreatGroupLevelConditionType.ID, expectedConditionTrigger, policyAlerts);
    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo(
        "Found license threat group 'testEvaluate-LicenseThreatGroupFromOrganization' with level >= 7 (level = 7)");
  }

  @Test
  public void testEvaluate_OneComponentTwoLicenseThreatGroups() {
    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = createConstraint("<=", "9");
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component = ComponentFactory.forGav("g", "a", "v", MatchState.EXACT);
    component.addLicenseThreatGroup(licenseThreatGroup2);
    component.addLicenseThreatGroup(licenseThreatGroup5);
    List<Component> components = Collections.singletonList(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(2);

    // Verify the 1st policy violation
    assertFactCounts(1, 1, policyAlerts.get(0));
    ConditionTrigger expectedConditionTrigger1 = new ConditionTrigger(0,
        new TriggerLicenseThreatGroupWithThreatLevel(licenseThreatGroup2));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, LicenseThreatGroupLevelConditionType.ID,
        expectedConditionTrigger1, policyAlerts);
    String actualReason1 = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason1).isEqualTo("Found license threat group 'Level 2' with level <= 9 (level = 2)");

    // Verify the 2nd policy violation
    assertFactCounts(1, 1, policyAlerts.get(1));
    ConditionTrigger expectedConditionTrigger2 = new ConditionTrigger(0,
        new TriggerLicenseThreatGroupWithThreatLevel(licenseThreatGroup2));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, LicenseThreatGroupLevelConditionType.ID,
        expectedConditionTrigger2, policyAlerts);
    String actualReason2 = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason2).isEqualTo("Found license threat group 'Level 5' with level <= 9 (level = 5)");
  }
}
