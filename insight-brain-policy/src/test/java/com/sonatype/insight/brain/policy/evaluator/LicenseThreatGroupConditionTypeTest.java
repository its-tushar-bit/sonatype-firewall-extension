/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.ConditionFact;
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
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerLicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LicenseThreatGroupConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  @Inject
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Inject
  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  @Inject
  private ComponentLoaderFactory componentLoaderFactory;

  private Organization org;

  private Application app;

  private LicenseThreatGroupLicense gpl20LicenseThreatGroupLicense;

  private ComponentLoader componentLoader;

  @Before
  public void before() {
    org = tempEntity.newOrganization("LicenseThreatGroupConditionTypeTest");
    app = tempEntity.newApplication("test", "LicenseThreatGroupConditionTypeTest_AppId", org.getId());
    componentLoader = componentLoaderFactory.createComponentLoader(app);

    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId(), "Copyleft", 8);
    gpl20LicenseThreatGroupLicense = tempEntity.newLicenseThreatGroupLicense(app.getId(), licenseThreatGroup.getId(),
        "GPL-2.0");

    licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId(), "Liberal", 2);
    tempEntity.newLicenseThreatGroupLicense(app.getId(), licenseThreatGroup.getId(), "Apache-2.0");
  }

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", LicenseThreatGroupConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateIs_Declared() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(
            0);

    // Create policy constraints
    Constraint constraint = createConstraint("is", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

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
        new TriggerLicenseThreatGroup(licenseThreatGroup.getId()));

    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();

    assertThat(actualReason).isEqualTo("Found licenses in the 'Copyleft' license threat group ('GPL-2.0')");
  }

  @Test
  public void testEvaluateIsNot_Declared() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(
            0);

    // Create policy constraints
    Constraint constraint = createConstraint("is not", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

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
        new TriggerLicenseThreatGroup(licenseThreatGroup.getId()));

    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();

    assertThat(actualReason).isEqualTo("Did not find a license in the 'Copyleft' license threat group");
  }

  @Test
  public void testEvaluateIs_Observed() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(
            0);

    // Create policy constraints
    Constraint constraint = createConstraint("is", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addObservedLicenseId("Apache-2.0");
    componentLoader.loadLicenseThreatGroups(component1);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addObservedLicenseId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroup(licenseThreatGroup.getId()));

    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();

    assertThat(actualReason).isEqualTo("Found licenses in the 'Copyleft' license threat group ('GPL-2.0')");
  }

  @Test
  public void testEvaluateIsNot_Observed() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(
            0);

    // Create policy constraints
    Constraint constraint = createConstraint("is not", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addObservedLicenseId("Apache-2.0");
    componentLoader.loadLicenseThreatGroups(component1);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addObservedLicenseId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroup(licenseThreatGroup.getId()));

    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();

    assertThat(actualReason).isEqualTo("Did not find a license in the 'Copyleft' license threat group");
  }

  @Test
  public void testEvaluateIs_Overridden() {
    LicenseThreatGroup licenseThreatGroup =
        licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0").get(0);

    // Create policy constraints
    Constraint constraint = createConstraint("is", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    component1.addObservedLicenseId("Apache-2.0");
    component1.addLicenseOverrideId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component1);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    component2.addObservedLicenseId("GPL-2.0");
    component2.addLicenseOverrideId("Apache-2.0");
    componentLoader.loadLicenseThreatGroups(component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroup(licenseThreatGroup.getId()));

    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();

    assertThat(actualReason).isEqualTo("Found licenses in the 'Copyleft' license threat group ('GPL-2.0')");
  }

  @Test
  public void testEvaluateIsNot_Overridden() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(
            0);

    // Create policy constraints
    Constraint constraint = createConstraint("is not", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    component1.addObservedLicenseId("Apache-2.0");
    component1.addLicenseOverrideId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component1);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    component2.addObservedLicenseId("GPL-2.0");
    component2.addLicenseOverrideId("Apache-2.0");
    componentLoader.loadLicenseThreatGroups(component2);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroup(licenseThreatGroup.getId()));

    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();

    assertThat(actualReason).isEqualTo("Did not find a license in the 'Copyleft' license threat group");
  }

  @Test
  public void testValidateCondition_InvalidLicenseThreatGroupId() {
    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is", "abc");
    assertThatThrownBy(
        () -> ConditionTypes.LicenseThreatGroupConditionType.validateCondition(null, condition, app.getId()))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Invalid license threat group id: abc");
  }

  @Test
  public void testValidateCondition_Unassigned() {
    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is",
        LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID);
    ConditionTypes.LicenseThreatGroupConditionType.validateCondition(null, condition, app.getId());
  }

  @Test
  public void testEvaluate_LicenseThreatGroupFromOrganization() {
    LicenseThreatGroup orgLicenseThreatGroup = tempEntity.newLicenseThreatGroup(org.getId(),
        "testEvaluate-LicenseThreatGroupFromOrganization", 5);
    tempEntity.newLicenseThreatGroupLicense(org.getId(), orgLicenseThreatGroup.getId(), "Apache-2.0");

    // Create policy constraints
    Constraint constraint = createConstraint("is", orgLicenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

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
        new TriggerLicenseThreatGroup(orgLicenseThreatGroup.getId()));

    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();

    assertThat(actualReason).isEqualTo("Found licenses in the 'testEvaluate-LicenseThreatGroupFromOrganization' "
        + "license threat group ('Apache-2.0')");
  }

  @Test
  public void testEvaluateIs_Unassigned() {
    // Un-assign the GPL-2.0 license
    licenseThreatGroupLicenseDAO.delete(gpl20LicenseThreatGroupLicense);

    // Create policy constraints
    Constraint constraint = createConstraint("is", LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID);
    List<Constraint> constraints = Lists.newArrayList(constraint);
    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    componentLoader.loadLicenseThreatGroups(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component2);
    List<Component> components = Lists.newArrayList(component1, component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroup(LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID));

    List<ConditionFact> conditionFacts = assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID,
        LicenseThreatGroupConditionType.ID, expectedConditionTrigger, policyAlerts);
    assertThat(conditionFacts).hasSize(1);
    assertThat(conditionFacts.get(0).getReason())
        .isEqualTo("Found licenses that are not assigned to any license threat group ('GPL-2.0')");
    assertThat(conditionFacts.get(0).getSummary()).isEqualTo("License Threat Group is '[unassigned]'");
  }

  @Test
  public void testEvaluateIsNot_Unassigned() {
    // Un-assign the GPL-2.0 license
    licenseThreatGroupLicenseDAO.delete(gpl20LicenseThreatGroupLicense);

    // Create policy constraints
    Constraint constraint = createConstraint("is not", LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID);
    List<Constraint> constraints = Lists.newArrayList(constraint);
    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("Apache-2.0");
    componentLoader.loadLicenseThreatGroups(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component2);
    List<Component> components = Lists.newArrayList(component1, component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroup(LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID));

    List<ConditionFact> conditionFacts = assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID,
        LicenseThreatGroupConditionType.ID, expectedConditionTrigger, policyAlerts);
    assertThat(conditionFacts).hasSize(1);
    assertThat(conditionFacts.get(0).getReason())
        .isEqualTo("Did not find a license that is not assigned to any license threat group");
    assertThat(conditionFacts.get(0).getSummary()).isEqualTo("License Threat Group is not '[unassigned]'");
  }

  @Test
  public void testExplainCondition_MissingLTG() {
    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is", "id-of-missing-ltg");
    LicenseThreatGroupConditionType conditionType = ConditionTypes.LicenseThreatGroupConditionType;
    assertThat(conditionType.explainCondition(condition)).isEqualTo(conditionType.getName() + " is '[deleted]'");
  }

  @Test
  public void testGenerateDroolsConditionValue_MissingLTG() {
    Condition condition = new Condition(LicenseThreatGroupConditionType.ID, "is", "id-of-missing-ltg");
    LicenseThreatGroupConditionType conditionType = ConditionTypes.LicenseThreatGroupConditionType;
    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      assertThat(conditionType.generateDroolsConditionCode(tx, condition)).isNotNull();
    }
  }

  @Test
  public void testEvaluate_Is_OneComponentTwoLicenseThreatGroups() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    Constraint constraint = createConstraint("is", licenseThreatGroup.getId());
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    // A component with two licenses in two license threat groups, one LTG being the one in the policy condition.
    // There should be a policy violation.
    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component.addDeclaredLicenseId("Apache-2.0");
    component.addDeclaredLicenseId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component);
    List<Component> components = Collections.singletonList(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0,
        new TriggerLicenseThreatGroup(licenseThreatGroup.getId()));

    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();

    assertThat(actualReason).isEqualTo("Found licenses in the 'Copyleft' license threat group ('GPL-2.0')");
  }

  @Test
  public void testEvaluate_IsNot_OneComponentTwoLicenseThreatGroups() {
    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(app.getId(), "GPL-2.0")
        .get(0);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    Constraint constraint = createConstraint("is not", licenseThreatGroup.getId());
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    // A component with two licenses in two license threat groups, one LTG being the one in the policy condition.
    // There should be no policy violation, because the component has a license that is not in the policy condition LTG.
    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component.addDeclaredLicenseId("Apache-2.0");
    component.addDeclaredLicenseId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component);
    List<Component> components = Collections.singletonList(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testEvaluateIs_TwoLicenses() {
    LicenseThreatGroup licenseThreatGroup =
        tempEntity.newLicenseThreatGroup(app.getOrganizationId(), "TestLTG", 5, "Apache-2.0", "GPL-2.0");

    // Create policy constraints
    Constraint constraint = createConstraint("is", licenseThreatGroup.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component.addDeclaredLicenseId("Apache-2.0");
    component.addDeclaredLicenseId("GPL-2.0");
    componentLoader.loadLicenseThreatGroups(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, Collections.singletonList(component));

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger =
        new ConditionTrigger(0, new TriggerLicenseThreatGroup(licenseThreatGroup.getId()));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, LicenseThreatGroupConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason)
        .isEqualTo("Found licenses in the 'TestLTG' license threat group ('Apache-2.0', 'GPL-2.0')");
  }
}
