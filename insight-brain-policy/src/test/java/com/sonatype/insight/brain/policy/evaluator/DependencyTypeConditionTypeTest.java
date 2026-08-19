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

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.TriggerSecurityVulnerabilityWithSeverity;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DependencyTypeConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value, String constraintId) {
    return createConstraint(constraintId, "Constraint Name 1", DependencyTypeConditionType.ID, operator, value);
  }

  private Constraint createConstraint(String operator, String value) {
    return createConstraint(operator, value, "ConstraintId");
  }

  @Test
  public void testEvaluateIs_Direct() {
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
  public void testEvaluateIsNot_Direct() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.DIRECT.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setDirectDependency(false);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(true);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was not Direct");
  }

  @Test
  public void testEvaluateIs_Transitive() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", DependencyType.TRANSITIVE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setDirectDependency(false);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(true);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was Transitive");
  }

  @Test
  public void testEvaluateIsNot_Transitive() {
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
    assertPolicy(constraint, components, component1, "Dependency type was not Transitive");
  }

  @Test
  public void testEvaluateNullValue_Transitive() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.TRANSITIVE.getId(), "1");
    Constraint constraint1 = createConstraint("is", DependencyType.TRANSITIVE.getId(), "2");

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);

    // Evaluate the policy
    assertNoPolicy(Arrays.asList(constraint, constraint1), components);
  }

  @Test
  public void testEvaluate_NullValue_Direct() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.DIRECT.getId(), "1");
    Constraint constraint1 = createConstraint("is", DependencyType.DIRECT.getId(), "2");

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);

    // Evaluate the policy
    assertNoPolicy(Arrays.asList(constraint, constraint1), components);
  }

  @Test
  public void testEvaluate_NullValue_InnerSource() {
    // "is innersource" should not match when isInnerSource is null (component is not innersource)
    Constraint constraint = createConstraint("is", DependencyType.INNER_SOURCE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);

    assertNoPolicy(Collections.singletonList(constraint), components);
  }

  @Test
  public void testEvaluateIsNot_InnerSourceValue() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.INNER_SOURCE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setDirectDependency(true);
    components.add(component1);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was not InnerSource");
  }

  @Test
  public void testEvaluateIs_InnerSourceValue() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", DependencyType.INNER_SOURCE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setInnerSource(false);
    components.add(component1);

    // Evaluate the policy
    assertNoPolicy(Collections.singletonList(constraint), components);
  }

  @Test
  public void testEvaluateIsNot_InnerSourceValueCondition() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.INNER_SOURCE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setInnerSource(false);
    components.add(component1);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was not InnerSource");
  }

  @Test
  public void testEvaluateIsNot_DirectCondition() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.DIRECT.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setDirectDependency(false);
    components.add(component1);

    // Evaluate the policy
    assertPolicy(constraint, components, component1, "Dependency type was not Direct");
  }

  @Test
  public void testEvaluateIs_InnerSource() {
    Constraint constraint = createConstraint("is", DependencyType.INNER_SOURCE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setInnerSource(true);
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

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    component1.setInnerSource(true);
    components.add(component1);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(false);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component2, "Dependency type was not InnerSource");
  }

  @Test
  public void testEvaluateIs_InnerSource_Is_Transitive() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", DependencyType.INNER_SOURCE.getId(), "1");
    Constraint constraint1 = createConstraint("is", DependencyType.TRANSITIVE.getId(), "2");

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);

    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(false);
    component2.setInnerSource(true);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(Arrays.asList(constraint, constraint1), components, component2, 2, "Dependency type was InnerSource",
        "Dependency type was Transitive");
  }

  @Test
  public void testEvaluateIs_InnerSource_Is_Direct() {
    // Create policy constraints
    Constraint constraint = createConstraint("is", DependencyType.INNER_SOURCE.getId(), "1");
    Constraint constraint1 = createConstraint("is", DependencyType.DIRECT.getId(), "2");

    Component component = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component.setDirectDependency(true);
    component.setInnerSource(true);

    // Evaluate the policy
    assertPolicy(Arrays.asList(constraint, constraint1), Collections.singletonList(component), component, 2,
        "Dependency type was InnerSource", "Dependency type was Direct");
  }

  @Test
  public void testEvaluateIsNot_InnerSource_Is_Direct() {
    // Create policy constraints
    Constraint constraint = createConstraint("is not", DependencyType.INNER_SOURCE.getId());

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);
    component1.setInnerSource(true);
    Component component2 = ComponentFactory.forGav("g2", "a2", "v2", MatchState.EXACT);
    component2.setDirectDependency(true);
    component2.setInnerSource(false);
    components.add(component2);

    // Evaluate the policy
    assertPolicy(constraint, components, component2, "Dependency type was not InnerSource");
  }

  // CLM-38699: DependencyType "is not innersource" should match components where isInnerSource is null
  // (null means not innersource, since innersource requires the flag to be explicitly set to true)
  @Test
  public void testEvaluateIsNot_InnerSource_WhenBothDependencyFieldsNull() {
    Constraint constraint = createConstraint("is not", DependencyType.INNER_SOURCE.getId());

    List<Component> components = new ArrayList<>();
    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    // Neither isInnerSource nor directDependency is set - typical for Python packages
    components.add(component);

    assertPolicy(constraint, components, component, "Dependency type was not InnerSource");
  }

  // CLM-38699: Reproduce the exact scenario - AND constraint with DependencyType + SecurityVulnerabilitySeverity
  @Test
  public void testEvaluateIsNot_InnerSource_CombinedWithSecurityVulnerabilitySeverity() {
    Condition secVulnCondition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "9");
    Condition depTypeCondition =
        new Condition(DependencyTypeConditionType.ID, "is not", DependencyType.INNER_SOURCE.getId());
    Constraint constraint = new Constraint("ConstraintId1", "Constraint Name 1", LogicalOperator.AND);
    constraint.addCondition(secVulnCondition);
    constraint.addCondition(depTypeCondition);

    SecurityVulnerability securityVulnerability = new SecurityVulnerability("source", "CVE-2024-0001", 9F);
    Component component = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    // No isInnerSource or directDependency - typical for Python packages
    component.addSecurityVulnerability(securityVulnerability);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<PolicyAlert> policyAlerts = evaluate(policy, Collections.singletonList(component));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID,
        SecurityVulnerabilitySeverityConditionType.ID,
        new ConditionTrigger(0, new TriggerSecurityVulnerabilityWithSeverity(securityVulnerability)),
        policyAlerts);
  }

  private void assertPolicy(
      final Constraint constraint,
      final List<Component> components,
      final Component component,
      final String... messages)
  {
    assertPolicy(Collections.singletonList(constraint), components, component, 1, messages);
  }

  private void assertPolicy(
      final List<Constraint> constraints,
      final List<Component> components,
      final Component component,
      final int policyAlertsSize,
      final String... messages)
  {
    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(policyAlertsSize);
    assertFactCounts(1, 1, policyAlerts.get(0));

    for (Constraint constraint : constraints) {
      assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, DependencyTypeConditionType.ID,
          policyAlerts);
    }

    List<String> reasons = new ArrayList<>();
    for (PolicyAlert policyAlert : policyAlerts) {
      reasons.add(
          policyAlert.getTrigger()
              .getComponentFacts()
              .get(0)
              .getConstraintFacts()
              .get(0)
              .getConditionFacts()
              .get(0)
              .getReason());
    }
    assertThat(reasons).containsExactlyInAnyOrder(messages);
  }

  private void assertNoPolicy(
      final List<Constraint> constraints,
      final List<Component> components)
  {
    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testValidateCondition_InvalidValue() {
    Condition condition = new Condition(DependencyTypeConditionType.ID, "is", "abc");
    DependencyTypeConditionType dependencyTypeConditionType = new DependencyTypeConditionType();
    assertThatThrownBy(() -> dependencyTypeConditionType.validateCondition(null, condition, null /* applicationId */))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("Value not supported: abc");
  }
}
