/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentFormatConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ComponentFormat;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComponentFormatConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", ComponentFormatConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluate_OperatorIs() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", "maven");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = new Component(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    component1.setMatchState(MatchState.EXACT);
    components.add(component1);
    Component component2 = new Component(ComponentIdentifier.createNpmCoordinates("p1", "v1"));
    component2.setMatchState(MatchState.EXACT);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, ComponentFormatConditionType.ID,
        policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Component format is 'maven'");
  }

  @Test
  public void testEvaluate_OperatorIsNot() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", "maven");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = new Component(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    component1.setMatchState(MatchState.EXACT);
    components.add(component1);
    Component component2 = new Component(ComponentIdentifier.createNpmCoordinates("p1", "v1"));
    component2.setMatchState(MatchState.EXACT);
    components.add(component2);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, ComponentFormatConditionType.ID,
        policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Component format is 'npm', not 'maven'");
  }

  @Test
  public void testValidateCondition_ValidValues() {
    for (String format : ComponentFormat.getAllAsStrings()) {
      // Should not throw exceptions
      Condition conditionIs = new Condition(ComponentFormatConditionType.ID, "is", format);
      ConditionTypes.ComponentFormatConditionType.validateCondition(null /* tx */, conditionIs, null /* ownerId */);

      Condition conditionIsNot = new Condition(ComponentFormatConditionType.ID, "is not", format);
      ConditionTypes.ComponentFormatConditionType.validateCondition(null /* tx */, conditionIsNot, null /* ownerId */);
    }
  }

  @Test
  public void testValidateCondition_InvalidValue() {
    Condition conditionIs = new Condition(ComponentFormatConditionType.ID, "is", "noSuchFormat");
    assertThatThrownBy(
        () -> new ComponentFormatConditionType().validateCondition(null /* tx */, conditionIs, null /* ownerId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Unsupported component format: 'noSuchFormat'");

    Condition conditionIsNot = new Condition(ComponentFormatConditionType.ID, "is not", "noSuchFormat");
    assertThatThrownBy(
        () -> new ComponentFormatConditionType().validateCondition(null /* tx */, conditionIsNot, null /* ownerId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Unsupported component format: 'noSuchFormat'");
  }

  @Test
  public void testValidateCondition_NullValue() {
    Condition condition = new Condition(ComponentFormatConditionType.ID, "is", null /* value */);
    assertThatThrownBy(
        () -> new ComponentFormatConditionType().validateCondition(null /* tx */, condition, null /* ownerId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Component format is required");
  }

  @Test
  public void testValidateCondition_EmptyStringValue() {
    Condition condition = new Condition(ComponentFormatConditionType.ID, "is", "" /* value */);
    assertThatThrownBy(
        () -> new ComponentFormatConditionType().validateCondition(null /* tx */, condition, null /* ownerId */))
            .isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Component format is required");
  }

  @Test
  public void testValidateCondition_SpaceValue() {
    Condition condition = new Condition(ComponentFormatConditionType.ID, "is", " " /* value */);
    assertThatThrownBy(() -> new ComponentFormatConditionType().validateCondition(null /* tx */, condition,
        null /* ownerId */)).isInstanceOf(InvalidConditionException.class)
            .hasMessageEndingWith("Component format is required");
  }

  @Test
  public void testEvaluate_UnknownComponent_WithoutComponentIdentifier() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", "maven");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = new Component();
    component.setMatchState(MatchState.UNKNOWN);
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(0);
  }

  @Test
  public void testEvaluate_UnknownComponent_WithComponentIdentifier() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", "maven");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component = new Component(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    component.setMatchState(MatchState.UNKNOWN);
    components.add(component);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);

    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, ComponentFormatConditionType.ID,
        policyAlerts);

    String actualReason = policyAlerts.get(0)
        .getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason();
    assertThat(actualReason).isEqualTo("Component format is 'maven'");
  }
}
