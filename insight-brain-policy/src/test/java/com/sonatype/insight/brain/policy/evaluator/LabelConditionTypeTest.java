/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerLabel;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LabelConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private static final String applicationPublicId = "LabelConditionTypeTest";

  private static String organizationId;

  private static String applicationId;

  @BeforeEach
  public void createApplication() {
    Organization organization = tempEntity.newOrganization("test-organization");
    organizationId = organization.getId();

    Application application = tempEntity.newApplication("test-application", applicationPublicId, organizationId);
    applicationId = application.getId();
  }

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", LabelConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateIs() {
    // Create some labels
    Label label1 = tempEntity.newLabel(applicationId, "Good", Color.dark_green);
    String labelId1 = label1.getId();
    Label label2 = tempEntity.newLabel(applicationId, "Bad", Color.dark_red);
    String labelId2 = label2.getId();

    // Create policy constraints
    Constraint constraint = createConstraint("is", labelId1);
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addLabelId(labelId1);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addLabelId(labelId2);
    components.add(component2);
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLabel(labelId1));

    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LabelConditionType.ID,
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

    assertThat(actualReason).isEqualTo("Found label 'Good'");
  }

  @Test
  public void testEvaluateIsNot() {
    // Create some labels
    Label label1 = tempEntity.newLabel(applicationId, "Good", Color.dark_green);
    String labelId1 = label1.getId();
    Label label2 = tempEntity.newLabel(applicationId, "Bad", Color.dark_red);
    String labelId2 = label2.getId();

    // Create policy constraints
    Constraint constraint = createConstraint("is not", labelId1);
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addLabelId(labelId1);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addLabelId(labelId2);
    components.add(component2);
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLabel(labelId1));

    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, LabelConditionType.ID,
        expectedConditionTrigger, policyAlerts);
    assertContainsPolicyAlert(component3, policy, constraint, FailActionType.ID, LabelConditionType.ID,
        expectedConditionTrigger, policyAlerts);

    String actualReason1 = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason1).isEqualTo("Did not find label 'Good'");

    String actualReason2 = policyAlerts.get(1)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason2).isEqualTo("Did not find label 'Good'");
  }

  @Test
  public void testOrganizationLabelIs() {
    // Create some labels
    Label label1 = tempEntity.newLabel(organizationId, "Good", Color.dark_green);
    String labelId1 = label1.getId();
    Label label2 = tempEntity.newLabel(organizationId, "Bad", Color.dark_red);
    String labelId2 = label2.getId();

    // Create policy constraints
    Constraint constraint = createConstraint("is", labelId1);
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.addLabelId(labelId1);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.addLabelId(labelId2);
    components.add(component2);
    Component component3 = ComponentFactory.forGav("g3", "a3", "v3", MatchState.EXACT);
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLabel(labelId1));

    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, LabelConditionType.ID,
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

    assertThat(actualReason).isEqualTo("Found label 'Good'");
  }

  @Test
  public void testValidateCondition_InvalidLabelId() {
    Condition condition = new Condition(LabelConditionType.ID, "is", "abc");
    assertThatThrownBy(() -> ConditionTypes.LabelConditionType.validateCondition(null, condition, applicationId))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("Invalid label id: abc");
  }

  @Test
  public void testEvaluateLabelNameEdgeCase() {
    Label label1 = tempEntity.newLabelWithInvalidLabelText(applicationId, "*/comment-end", Color.dark_green);
    String labelId1 = label1.getId();

    List<Constraint> constraints = new ArrayList<>();
    constraints.add(createConstraint("is", labelId1));

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testEvaluate_Is_OneComponentTwoLabels() {
    // Create some labels
    Label label1 = tempEntity.newLabel(applicationId, "Good", Color.dark_green);
    Label label2 = tempEntity.newLabel(applicationId, "Bad", Color.dark_red);

    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = createConstraint("is", label1.getId());
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component = ComponentFactory.forGav("g", "a", "v", MatchState.EXACT);
    component.addLabelId(label1.getId());
    component.addLabelId(label2.getId());
    List<Component> components = Collections.singletonList(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));

    ConditionTrigger expectedConditionTrigger = new ConditionTrigger(0, new TriggerLabel(label1.getId()));

    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, LabelConditionType.ID,
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

    assertThat(actualReason).isEqualTo("Found label 'Good'");
  }

  @Test
  public void testEvaluate_IsNot_OneComponentTwoLabels() {
    // Create some labels
    Label label1 = tempEntity.newLabel(applicationId, "Good", Color.dark_green);
    Label label2 = tempEntity.newLabel(applicationId, "Bad", Color.dark_red);

    // Create policy
    Policy policy = new Policy("PolicyId", "Policy Name");
    Constraint constraint = createConstraint("is not", label1.getId());
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    Component component = ComponentFactory.forGav("g", "a", "v", MatchState.EXACT);
    component.addLabelId(label1.getId());
    component.addLabelId(label2.getId());
    List<Component> components = Collections.singletonList(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).isEmpty();
  }
}
