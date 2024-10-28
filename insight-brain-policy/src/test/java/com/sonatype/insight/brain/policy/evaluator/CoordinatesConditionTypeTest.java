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

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CoordinatesConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", CoordinatesConditionType.ID, operator, value);
  }

  @Test
  public void testEvaluate_Maven_MatchExact() {
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_MAVEN,
        "Coordinates were g2 : a2 : v2 (match g2 : a2 : * : * : v2)");
  }

  @Test
  public void testEvaluate_Aname_MatchExact() {
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_ANAME, "Coordinates were g2 (a2) v2 (match g2 (a2) v2)");
  }

  @Test
  public void testEvaluate_Pypi_MatchExact() {
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_PYPI,
        "Coordinates were g2 (v2) a2 (.e2) (match g2 (v2) a2 (.*))");
  }

  @Test
  public void testEvaluate_npm_MatchExact() {
    testEvaluate_MatchExact(ComponentIdentifier.FORMAT_NPM,
        "Coordinates were g2 : a2 (match g2 : a2)");
  }

  private void testEvaluate_MatchExact(String format, String expectedMessage) {
    // Create policy constraints
    Constraint constraint = createConstraint("match", format + ":g2:a2:v2");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forCoordinates(format, "g1", "a1", "v1", "e1");
    components.add(component1);
    Component component2 = ComponentFactory.forCoordinates(format, "g2", "a2", "v2", "e2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo(expectedMessage);
  }

  @Test
  public void testEvaluate_Maven_MatchGavecNotGavce() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + ":g:a:v:e:c");

    Component componentGavec = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");
    Component componentGavce = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "c", "e");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGavec, componentGavce));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : c : v (match g : a : e : c : v)");
  }

  @Test
  public void testEvaluate_Maven_MatchGaveNotGavc() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + ":g:a:v:e:");

    Component componentGave = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavc = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "e");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavc));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : v (match g : a : e : v)");
  }

  @Test
  public void testEvaluate_Maven_MatchGavAnyExtensionAnyClassifier() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + ":g:a:v");

    Component componentGav3 = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v");
    Component componentGav5 = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "");
    Component componentGave = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavc = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "", "c");
    Component componentGavec = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy,
        Arrays.asList(componentGav3, componentGav5, componentGave, componentGavc, componentGavec));
    assertThat(policyAlerts).hasSize(5);
    for (PolicyAlert policyAlert : policyAlerts) {
      assertFactCounts(1, 1, policyAlert);
    }
    assertContainsPolicyAlert(componentGav3, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGav5, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavc, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a :  : c : v (match g : a : * : * : v)");
    actualReason = policyAlerts.get(1).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : c : v (match g : a : * : * : v)");
    actualReason = policyAlerts.get(2).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : v (match g : a : * : * : v)");
    actualReason = policyAlerts.get(3).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : v (match g : a : * : * : v)");
    actualReason = policyAlerts.get(4).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : v (match g : a : * : * : v)");
  }

  @Test
  public void testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates() throws Exception {
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven:g", "(match g : * : * : * : *)");
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven::a", "(match * : a : * : * : *)");
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven:::v", "(match * : * : * : * : v)");
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven:g:a", "(match g : a : * : * : *)");
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven:g::v", "(match g : * : * : * : v)");
    testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates("maven::a:v", "(match * : a : * : * : v)");
  }

  private void testEvaluate_Maven_LegacyConditionsWithEmptyGavCoordinates(
      final String coordinatesValue,
      final String expectedConditionMessage)
  {
    Policy policy = createPolicy(coordinatesValue);

    Component componentGav = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v");
    Component componentGave = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGav, componentGave, componentGavec));
    assertThat(policyAlerts).hasSize(3);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertFactCounts(1, 1, policyAlerts.get(2));
    assertContainsPolicyAlert(componentGav, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : c : v " + expectedConditionMessage);
    actualReason = policyAlerts.get(1).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : v " + expectedConditionMessage);
    actualReason = policyAlerts.get(2).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : v " + expectedConditionMessage);
  }

  @Test
  public void testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates() throws Exception {
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name:n", "(match n (*) *)");
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name::q", "(match * (q) *)");
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name::q:v", "(match * (q) v)");
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name:n:q", "(match n (q) *)");
    testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates("a-name:n:q:v", "(match n (q) v)");
  }

  private void testEvaluate_Aname_LegacyConditionsWithEmptyCoordinates(
      final String coordinatesValue,
      final String expectedConditionMessage)
  {
    Policy policy = createPolicy(coordinatesValue);

    Component componentNqv = ComponentFactory.forCoordinates(ComponentIdentifier.FORMAT_ANAME, "n", "q", "v");

    List<PolicyAlert> policyAlerts = evaluate(policy, Collections.singletonList(componentNqv));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentNqv, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were n (q) v " + expectedConditionMessage);
  }

  @Test
  public void testEvaluate_Maven_EmptyClassifierCoordinate_Matches_EmptyClassifierValue() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + ":g:a:v:e:");

    Component componentGave = ComponentFactory.forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec =
        ComponentFactory.forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavec));
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : v (match g : a : e : v)");
  }

  @Test
  public void testEvaluate_Maven_WildcardClassifierCoordinate_Matches_AnyClassifierValue() {
    Policy policy = createPolicy(ComponentIdentifier.FORMAT_MAVEN + ":g:a:v:e:*");

    Component componentGave = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "");
    Component componentGavec = ComponentFactory
        .forCoordinates(ComponentIdentifier.FORMAT_MAVEN, "g", "a", "v", "e", "c");

    List<PolicyAlert> policyAlerts = evaluate(policy, Arrays.asList(componentGave, componentGavec));
    assertThat(policyAlerts).hasSize(2);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertFactCounts(1, 1, policyAlerts.get(1));
    assertContainsPolicyAlert(componentGave, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    assertContainsPolicyAlert(componentGavec, policy, policy.getConstraints().get(0), FailActionType.ID,
        CoordinatesConditionType.ID, policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : c : v (match g : a : e : * : v)");
    actualReason = policyAlerts.get(1).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g : a : e : v (match g : a : e : * : v)");
  }

  private Policy createPolicy(final String constraintValue) {
    final Policy policy = new Policy("policyId", "policyName");
    policy.setConstraints(Collections.singletonList(createConstraint("match", constraintValue)));
    policy.setAction(BuildStageType.ID, FailActionType.ID);
    return policy;
  }

  @Test
  public void testEvaluate_Maven_MatchGavWithSpaces() {
    // Create policy constraints
    Constraint constraint = createConstraint("match", "maven:g1 : a1 : v1");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", "a1", "v1", MatchState.EXACT);
    components.add(component1);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g1 : a1 : v1 (match g1 : a1 : * : * : v1)");
  }

  @Test
  public void testEvaluate_Maven_MatchWildcard() {
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_MAVEN,
        "Coordinates were g2 : a2 : v2 (match g2 : a* : * : * : v2)");
  }

  @Test
  public void testEvaluate_Aname_MatchWildcard() {
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_ANAME, "Coordinates were g2 (a2) v2 (match g2 (a*) v2)");
  }

  @Test
  public void testEvaluate_Pypi_MatchWildcard() {
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_PYPI,
        "Coordinates were g2 (v2) a2 (.e2) (match g2 (v2) a* (.*))");
  }

  @Test
  public void testEvaluate_npm_MatchWildcard() {
    testEvaluate_MatchWildcard(ComponentIdentifier.FORMAT_NPM,
        "Coordinates were g2 : a2 (match g2 : a*)");
  }

  private void testEvaluate_MatchWildcard(String format, String expectedMessage) {
    // Create policy constraints
    Constraint constraint = createConstraint("match", format + ":g2:a*:v2");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forCoordinates(format, "g1", "a1", "v1", "e1");
    components.add(component1);
    Component component2 = ComponentFactory.forCoordinates(format, "g2", "a2", "v2", "e2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component2, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo(expectedMessage);
  }

  @Test
  public void testEvaluate_Maven_DoNotMatchExact() {
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_MAVEN,
        "Coordinates were g1 : a1 : v1 (do not match g2 : a2 : * : * : v2)");
  }

  @Test
  public void testEvaluate_Aname_DoNotMatchExact() {
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_ANAME,
        "Coordinates were g1 (a1) v1 (do not match g2 (a2) v2)");
  }

  @Test
  public void testEvaluate_Pypi_DoNotMatchExact() {
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_ANAME,
        "Coordinates were g1 (a1) v1 (do not match g2 (a2) v2)");
  }

  @Test
  public void testEvaluate_npm_DoNotMatchExact() {
    testEvaluate_DoNotMatchExact(ComponentIdentifier.FORMAT_NPM,
        "Coordinates were g1 : a1 (do not match g2 : a2)");
  }

  private void testEvaluate_DoNotMatchExact(String format, String expectedMessage) {
    // Create policy constraints
    Constraint constraint = createConstraint("do not match", format + ":g2:a2:v2");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forCoordinates(format, "g1", "a1", "v1", "e1");
    components.add(component1);
    Component component2 = ComponentFactory.forCoordinates(format, "g2", "a2", "v2", "e2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo(expectedMessage);
  }

  @Test
  public void testEvaluate_Maven_DoNotMatchWildcard() {
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_MAVEN,
        "Coordinates were g1 : a1 : v1 (do not match g2 : a* : * : * : v2)");
  }

  @Test
  public void testEvaluate_Aname_DoNotMatchWildcard() {
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_ANAME,
        "Coordinates were g1 (a1) v1 (do not match g2 (a*) v2)");
  }

  @Test
  public void testEvaluate_Pypi_DoNotMatchWildcard() {
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_PYPI,
        "Coordinates were g1 (v1) a1 (.e1) (do not match g2 (v2) a* (.*))");
  }

  @Test
  public void testEvaluate_npm_DoNotMatchWildcard() {
    testEvaluate_DoNotMatchWildcard(ComponentIdentifier.FORMAT_NPM,
        "Coordinates were g1 : a1 (do not match g2 : a*)");
  }

  private void testEvaluate_DoNotMatchWildcard(String format, String expectedMessage) {
    // Create policy constraints
    Constraint constraint = createConstraint("do not match", format + ":g2:a*:v2");
    List<Constraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    // Create policy
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forCoordinates(format, "g1", "a1", "v1", "e1");
    components.add(component1);
    Component component2 = ComponentFactory.forCoordinates(format, "g2", "a2", "v2", "e2");
    components.add(component2);
    Component component3 = new Component();
    components.add(component3);

    // Evaluate the policy
    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo(expectedMessage);
  }

  @Test
  public void testEvaluate_MatchPyPiCoordinatesIgnoreCase() {
    Constraint constraint = createConstraint("match", ComponentIdentifier.FORMAT_PYPI + ":PyYaMl:1:*:*");
    List<Constraint> constraints = Collections.singletonList(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component =
        ComponentFactory.forCoordinates(ComponentIdentifier.FORMAT_PYPI, "pyyaml", "1", "*", "*");
    components.add(component);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were pyyaml (*) 1 (.*) (match PyYaMl (*) 1 (.*))");
  }

  @Test
  public void testEvaluate_MatchNpmCoordinatesCaseSensitive() {
    Constraint constraint = createConstraint("do not match", ComponentIdentifier.FORMAT_NPM + ":jQuery:1");
    List<Constraint> constraints = Collections.singletonList(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component =
        ComponentFactory.forCoordinates(ComponentIdentifier.FORMAT_NPM, "jquery", "1");
    components.add(component);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were jquery : 1 (do not match jQuery : 1)");
  }

  @Test
  public void testEvaluate_MatchPyPiCoordinates_OptionalCoordinates() {
    Constraint constraint = createConstraint("match", ComponentIdentifier.FORMAT_PYPI + ":PyYaMl:1::");
    List<Constraint> constraints = Collections.singletonList(constraint);

    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    policy.setConstraints(constraints);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component =
        ComponentFactory.forCoordinates(ComponentIdentifier.FORMAT_PYPI, "pyyaml", "1", "", "");
    components.add(component);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were pyyaml 1 (match PyYaMl 1)");
  }

  @Test
  public void testEvaluate_EscapeUnsafeCharacter() {
    String artifactId = "\\\"\r\n\t'";
    Policy policy = new Policy("PolicyId1", "Policy Name 1");
    Constraint constraint = createConstraint("match", "maven:g1:" + artifactId);
    policy.addConstraint(constraint);
    policy.setAction(BuildStageType.ID, FailActionType.ID);

    List<Component> components = new ArrayList<>();
    Component component1 = ComponentFactory.forGav("g1", artifactId, "v1", MatchState.EXACT);
    components.add(component1);

    List<PolicyAlert> policyAlerts = evaluate(policy, components);
    assertThat(policyAlerts).hasSize(1);
    assertFactCounts(1, 1, policyAlerts.get(0));
    assertContainsPolicyAlert(component1, policy, constraint, FailActionType.ID, CoordinatesConditionType.ID,
        policyAlerts);
    String actualReason = policyAlerts.get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason();
    assertThat(actualReason).isEqualTo("Coordinates were g1 : \\\"\r\n" + "\t' : v1 (match g1 : \\\"\r\n" +
        "\t' : * : * : *)");
  }

  @Test
  public void testValidateCondition_NullCoordinates() {
    Condition condition = new Condition(CoordinatesConditionType.ID, "match", null);
    assertThatThrownBy(
        () -> new CoordinatesConditionType().validateCondition(null, condition, null /* applicationId */))
        .isInstanceOf(InvalidConditionException.class).hasMessageEndingWith("Missing coordinates");
  }

  @Test
  public void testValidateCondition_EmptyCoordinates() {
    Condition condition = new Condition(CoordinatesConditionType.ID, "match", " ");
    assertThatThrownBy(
        () -> new CoordinatesConditionType().validateCondition(null, condition, null /* applicationId */))
        .isInstanceOf(InvalidConditionException.class).hasMessageEndingWith("Missing coordinates");
  }

  @Test
  public void testValidateCondition_UnsupportedCoordinateFormat() {
    Condition condition = new Condition(CoordinatesConditionType.ID, "match", "nuget");
    assertThatThrownBy(
        () -> new CoordinatesConditionType().validateCondition(null, condition, null /* applicationId */))
        .isInstanceOf(InvalidConditionException.class)
        .hasMessageEndingWith("Unsupported component identifier format for coordinates policy condition: 'nuget'");
  }

  @Test
  public void testConvertIfNeeded_UnsupportedCoordinateFormat_DoesNotThrowNullPointerException() {
    new Condition(CoordinatesConditionType.ID, "match", "nuget::").getValue();
    new Condition(CoordinatesConditionType.ID, "match", "unknown:").getValue();
  }

  @Test
  public void testConvertIfNeeded() {
    assertConvertIfNeeded("maven:g", "maven:g:*:*:*:*");
    assertConvertIfNeeded("maven::a", "maven:*:a:*:*:*");
    assertConvertIfNeeded("maven:::v", "maven:*:*:v:*:*");
    assertConvertIfNeeded("maven:g:a", "maven:g:a:*:*:*");
    assertConvertIfNeeded("maven:g::v", "maven:g:*:v:*:*");
    assertConvertIfNeeded("maven::a:v", "maven:*:a:v:*:*");
    assertConvertIfNeeded("maven:g:a:v", "maven:g:a:v:*:*");
    assertConvertIfNeeded("maven:g:a:v:e:", "maven:g:a:v:e:");
    assertConvertIfNeeded("maven:g:a:v:e:c", "maven:g:a:v:e:c");

    assertConvertIfNeeded("a-name:n", "a-name:n:*:*");
    assertConvertIfNeeded("a-name::q", "a-name:*:q:*");
    assertConvertIfNeeded("a-name:::v", "a-name:*::v");
    assertConvertIfNeeded("a-name:n:q", "a-name:n:q:*");
    assertConvertIfNeeded("a-name:n::v", "a-name:n::v");
    assertConvertIfNeeded("a-name:n:q:v", "a-name:n:q:v");

    assertConvertIfNeeded("pypi:n", "pypi:n:*:*:*");
    assertConvertIfNeeded("pypi::v", "pypi:*:v:*:*");
    assertConvertIfNeeded("pypi:::q", "pypi:*:*:q:*");
    assertConvertIfNeeded("pypi::::e", "pypi:*:*::e");
    assertConvertIfNeeded("pypi:n:v", "pypi:n:v:*:*");
    assertConvertIfNeeded("pypi::v:q", "pypi:*:v:q:*");
    assertConvertIfNeeded("pypi:n:v:q", "pypi:n:v:q:*");
    assertConvertIfNeeded("pypi:n:v::e", "pypi:n:v::e");
    assertConvertIfNeeded("pypi:n:v:q:e", "pypi:n:v:q:e");

    assertConvertIfNeeded("npm:n::", "npm:n:*");
  }

  private void assertConvertIfNeeded(final String value, final String expectedConvertedValue) {
    assertThat(createCoordinateCondition(value).getValue()).isEqualTo(expectedConvertedValue);
  }

  private Condition createCoordinateCondition(final String value) {
    return new Condition(CoordinatesConditionType.ID, "match", value);
  }
}
