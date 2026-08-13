/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerLicense;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LicenseConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private LicenseDAO licenseDAO;

  private MultiLicenseDAO multiLicenseDAO;

  private LicenseDataUpdater savedLicenseDataUpdater;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    licenseDAO = daoFactory.createLicenseDAO();
    multiLicenseDAO = daoFactory.createMultiLicenseDAO();
    before();
  }

  public void before() {
    savedLicenseDataUpdater = LicenseDataUpdater.getUpdater();
    LicenseDataUpdater.setUpdater(
        new LicenseDataUpdater(licenseDAO, multiLicenseDAO)
        {
          @Override
          public void doUpdate() {
          }
        });
  }

  @AfterEach
  public void after() {
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
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("UNSPECIFIED");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("AFL-1.2");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense(License.UNSPECIFIED_ID));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LicenseConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Found 'Not Provided' license");
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
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addDeclaredLicenseId("UNSPECIFIED");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addDeclaredLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense(License.UNSPECIFIED_ID));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, LicenseConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Did not find 'Not Provided' license");
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
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addObservedLicenseId("UNSPECIFIED");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addObservedLicenseId("AFL-1.2");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense(License.UNSPECIFIED_ID));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LicenseConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Found 'Not Provided' license");
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
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addObservedLicenseId("UNSPECIFIED");
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addObservedLicenseId("Apache-2.0");
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense(License.UNSPECIFIED_ID));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, LicenseConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Did not find 'Not Provided' license");
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
    policy.setAction(BuildStageType.ID, FailActionType.ID);

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

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense(License.UNSPECIFIED_ID));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, LicenseConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Found 'Not Provided' license");
  }

  @Test
  public void testEvaluateIs_Overridden_Multiple_LicenseIds() {
    // Create policy constraints
    List<Constraint> constraints = new ArrayList<>();
    Constraint constraint1 = createConstraint("constraintId1", "constraintName1", LicenseConditionType.ID, "is",
        "Apache-2.0");
    constraints.add(constraint1);

    // Create policy
    Policy policy = new Policy("policyId1", "policyName1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component.addDeclaredLicenseId("UNSPECIFIED");
    component.addObservedLicenseId("UNSPECIFIED");
    component.addLicenseOverrideId("Apache-2.0");
    component.addLicenseOverrideId("AFL-1.2");
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense("Apache-2.0"));
    assertContainsPolicyAlert(component, policy, constraint1, FailActionType.ID, LicenseConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Found 'Apache-2.0' license");

    constraints = new ArrayList<>();
    Constraint constraint2 =
        createConstraint("constraintId2", "constraintName2", LicenseConditionType.ID, "is", "AFL-1.2");
    constraints.add(constraint2);

    policy.setConstraints(constraints);

    // Evaluate the policy
    policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense("AFL-1.2"));
    assertContainsPolicyAlert(component, policy, constraint2, FailActionType.ID, LicenseConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Found 'AFL-1.2' license");
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
    policy.setAction(BuildStageType.ID, FailActionType.ID);

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

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense(License.UNSPECIFIED_ID));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LicenseConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Did not find 'Not Provided' license");
  }

  @Test
  public void testValidateCondition_InvalidLicenseId() {
    Condition condition = new Condition(LicenseConditionType.ID, "is", "abc");
    assertThatThrownBy(
        () -> ConditionTypes.LicenseConditionType.validateCondition(null, condition, null /* applicationId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Invalid license id: abc");
  }

  @Test
  public void testEvaluate_Is_OneComponentTwoLicenses() {
    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = createConstraint("is", "Apache-2.0");
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component = ComponentFactory.forGav("g", "a", "v", MatchState.EXACT);
    component.addDeclaredLicenseId("Apache-2.0");
    component.addDeclaredLicenseId("GPL-2.0");
    List<Component> components = Collections.singletonList(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLicense("Apache-2.0"));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, LicenseConditionType.ID,
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
    assertThat(actualReason).isEqualTo("Found 'Apache-2.0' license");
  }

  @Test
  public void testEvaluate_IsNot_OneComponentTwoLicenses() {
    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = createConstraint("is not", "Apache-2.0");
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component = ComponentFactory.forGav("g", "a", "v", MatchState.EXACT);
    component.addDeclaredLicenseId("Apache-2.0");
    component.addDeclaredLicenseId("GPL-2.0");
    List<Component> components = Collections.singletonList(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).isEmpty();
  }
}
