/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DependencyTypeConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", DependencyTypeConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluateIs() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", DependencyType.DIRECT.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setDirectDependency(true);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(false);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was Direct");
  }

  @Test
  public void testEvaluateIsNot() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.TRANSITIVE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setDirectDependency(true);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(false);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was Direct, not Transitive");
  }

  @Test
  public void testEvaluateIsNot_NullValue() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.TRANSITIVE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setDirectDependency(true);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(null);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was Direct, not Transitive");
  }

  @Test
  public void testEvaluateIs_InnerSource() {
    Constraint constraint = createConstraint("is", DependencyType.INNER_SOURCE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    InnerSourceData innerSourceData = new InnerSourceData();
    innerSourceData.setInnerSource(true);
    component1.setInnerSourceData(innerSourceData);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(true);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was InnerSource");
  }

  @Test
  public void testEvaluateIsNot_InnerSource_Is_Transitive() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.INNER_SOURCE.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    InnerSourceData innerSourceData = new InnerSourceData();
    innerSourceData.setInnerSource(true);
    component1.setInnerSourceData(innerSourceData);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(false);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component2, "Dependency type was Transitive, not InnerSource");
  }

  @Test
  public void testEvaluateIsNot_InnerSource_Is_Direct() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.INNER_SOURCE.getId());
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    InnerSourceData innerSourceData = new InnerSourceData();
    innerSourceData.setInnerSource(true);
    component1.setInnerSourceData(innerSourceData);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(true);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component2, "Dependency type was Direct, not InnerSource");
  }

  private void assertPolicy(
      final Constraint constraint,
      final List<Component> components,
      final Component component,
      final String message)
  {
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, DependencyTypeConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo(message);
  }

  @Test
  public void testValidateCondition_InvalidValue() {
    Condition condition = new Condition(DependencyTypeConditionType.ID, "is", "abc");
    assertThatThrownBy(() -> {
      new DependencyTypeConditionType().validateCondition(null, condition, null /* applicationId */);
    }).isInstanceOf(InvalidConditionException.class).hasMessageEndingWith("Value not supported: abc");
  }
}
