/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;

public class PolicyAlertDigesterTest
{
  @Test
  public void testDigest_Nothing() {
    final List<PolicyAlert> oldAlerts = Collections.emptyList();
    final List<PolicyAlert> newAlerts = Collections.emptyList();

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results, nullValue());
  }

  @Test
  public void testDigest_UnknownPolicyAlert() {
    final List<PolicyAlert> oldAlerts = Collections.emptyList();
    final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(newAlerts.get(0)));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_NoChange() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results, nullValue());
  }

  @Test
  public void testDigest_ClearedPolicyAlert() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final List<PolicyAlert> newAlerts = Collections.emptyList();

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], empty());
    assertThat(results[1], contains(oldAlerts.get(0)));
  }

  @Test
  public void testDigest_UnknownPolicyAlertBefore() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

    newAlerts.add(0, new PolicyAlert(policyFact("policy_1", "Policy 1", 0), Collections.<Action> emptyList()));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(newAlerts.get(0)));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_UnknownPolicyAlertAfter() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

    newAlerts.add(new PolicyAlert(policyFact("policy_8", "Policy 8", 0), Collections.<Action> emptyList()));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(newAlerts.get(1)));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_UnknownPolicyAlertBeforeAndAfter() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

    newAlerts.add(0, new PolicyAlert(policyFact("policy_1", "Policy 1", 0), Collections.<Action> emptyList()));
    newAlerts.add(new PolicyAlert(policyFact("policy_8", "Policy 8", 0), Collections.<Action> emptyList()));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(newAlerts.get(0), newAlerts.get(2)));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_UnknownPolicyAlertBeforeAndAfterClearedPolicyAlert() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final List<PolicyAlert> newAlerts = new ArrayList<PolicyAlert>();

    newAlerts.add(new PolicyAlert(policyFact("policy_1", "Policy 1", 0), Collections.<Action> emptyList()));
    newAlerts.add(new PolicyAlert(policyFact("policy_8", "Policy 8", 0), Collections.<Action> emptyList()));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(newAlerts.get(0), newAlerts.get(1)));
    assertThat(results[1], contains(oldAlerts.get(0)));
  }

  @Test
  public void testDigest_UnknownComponentFactBefore() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final PolicyFact trigger = oldAlerts.get(0).getTrigger();

    final ComponentFact oldFact = trigger.getComponentFacts().get(0);
    final ComponentFact newFact = componentFact("1G", "A", "V", "1H");

    final List<PolicyAlert> newAlerts = Arrays.asList(oldAlerts.get(0).with(trigger.with(newFact, oldFact)));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts(), contains(newFact));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_UnknownComponentFactAfter() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    PolicyFact trigger = oldAlerts.get(0).getTrigger();

    final ComponentFact oldFact = trigger.getComponentFacts().get(0);
    final ComponentFact newFact = componentFact("G1", "A", "V", "H1");

    final List<PolicyAlert> newAlerts = Arrays.asList(oldAlerts.get(0).with(trigger.with(oldFact, newFact)));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts(), contains(newFact));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_UnknownConstraintFactBefore() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final PolicyFact trigger = oldAlerts.get(0).getTrigger();
    final ComponentFact component = trigger.getComponentFacts().get(0);

    final ConstraintFact oldFact = component.getConstraintFacts().get(0);
    final ConstraintFact newFact = constraintFact("constraint_1", "Constraint 1", "AND");

    final List<PolicyAlert> newAlerts = Arrays.asList(oldAlerts.get(0).with(
        trigger.with(component.with(newFact, oldFact))));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts(), contains(newFact));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_UnknownConstraintFactAfter() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final PolicyFact trigger = oldAlerts.get(0).getTrigger();
    final ComponentFact component = trigger.getComponentFacts().get(0);

    final ConstraintFact oldFact = component.getConstraintFacts().get(0);
    final ConstraintFact newFact = constraintFact("constraint_8", "Constraint 8", "AND");

    final List<PolicyAlert> newAlerts = Arrays.asList(oldAlerts.get(0).with(
        trigger.with(component.with(oldFact, newFact))));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts(), contains(newFact));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_UnknownConditionFactBefore() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final PolicyFact trigger = oldAlerts.get(0).getTrigger();
    final ComponentFact component = trigger.getComponentFacts().get(0);
    final ConstraintFact constraint = component.getConstraintFacts().get(0);

    final ConditionFact oldFact = constraint.getConditionFacts().get(0);
    final ConditionFact newFact = conditionFact(CoordinatesConditionType.ID, "match", "*");

    final List<PolicyAlert> newAlerts = Arrays.asList(oldAlerts.get(0).with(
        trigger.with(component.with(constraint.with(newFact, oldFact)))));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts(), contains(newFact));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_UnknownConditionFactAfter() {
    final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
    final PolicyFact trigger = oldAlerts.get(0).getTrigger();
    final ComponentFact component = trigger.getComponentFacts().get(0);
    final ConstraintFact constraint = component.getConstraintFacts().get(0);

    final ConditionFact oldFact = constraint.getConditionFacts().get(0);
    final ConditionFact newFact = conditionFact(SecurityVulnerabilityConditionType.ID, "present");

    final List<PolicyAlert> newAlerts = Arrays.asList(oldAlerts.get(0).with(
        trigger.with(component.with(constraint.with(oldFact, newFact)))));

    final List<PolicyAlert>[] results = PolicyAlertDigester.digestPolicyAlerts(newAlerts, oldAlerts);

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts(), contains(newFact));
    assertThat(results[1], empty());
  }

  @Test
  public void testDigest_PolicyNameChange() {
    final PolicyAlert oldAlert = defaultPolicyAlert();
    final PolicyFact trigger = oldAlert.getTrigger();

    PolicyAlert newAlert;
    List<PolicyAlert>[] results;

    newAlert = oldAlert.with(policyFact("policy_4", "Policy 4", 0).with(trigger.getComponentFacts()));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));
    assertThat(results, nullValue());

    newAlert = oldAlert.with(policyFact("policy_4", "Policy 4~", 0).with(trigger.getComponentFacts()));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));
    assertThat(results[0], contains(newAlert));
  }

  @Test
  public void testDigest_PolicyThreatLevelChange() {
    final PolicyAlert oldAlert = defaultPolicyAlert();
    final PolicyFact trigger = oldAlert.getTrigger();

    PolicyAlert newAlert;
    List<PolicyAlert>[] results;

    newAlert = oldAlert.with(policyFact("policy_4", "Policy 4", 0).with(trigger.getComponentFacts()));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));
    assertThat(results, nullValue());

    newAlert = oldAlert.with(policyFact("policy_4", "Policy 4", 1).with(trigger.getComponentFacts()));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));
    assertThat(results[0], contains(newAlert));
  }

  @Test
  public void testDigest_ConstraintNameChange() {
    final PolicyAlert oldAlert = defaultPolicyAlert();
    final PolicyFact trigger = oldAlert.getTrigger();

    final ComponentFact componentFact = trigger.getComponentFacts().get(0);
    final ConstraintFact constraintFact = componentFact.getConstraintFacts().get(0);
    final ConditionFact conditionFact = constraintFact.getConditionFacts().get(0);

    PolicyAlert newAlert;
    List<PolicyAlert>[] results;

    final ConstraintFact sameConstraintFact = constraintFact("constraint_4", "Constraint 4", "OR").with(conditionFact);

    newAlert = oldAlert.with(trigger.with(componentFact.with(sameConstraintFact)));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));

    assertThat(results, nullValue());

    final ConstraintFact newConstraintFact = constraintFact("constraint_4", "Constraint 4~", "OR").with(conditionFact);

    newAlert = oldAlert.with(trigger.with(componentFact.with(newConstraintFact)));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts(),
        contains(newConstraintFact));
    assertThat(results[1].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts(), contains(constraintFact));
  }

  @Test
  public void testDigest_ConstraintOperatorChange() {
    final PolicyAlert oldAlert = defaultPolicyAlert();
    final PolicyFact trigger = oldAlert.getTrigger();

    final ComponentFact componentFact = trigger.getComponentFacts().get(0);
    final ConstraintFact constraintFact = componentFact.getConstraintFacts().get(0);
    final ConditionFact conditionFact = constraintFact.getConditionFacts().get(0);

    PolicyAlert newAlert;
    List<PolicyAlert>[] results;

    final ConstraintFact sameConstraintFact = constraintFact("constraint_4", "Constraint 4", "OR").with(conditionFact);

    newAlert = oldAlert.with(trigger.with(componentFact.with(sameConstraintFact)));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));

    assertThat(results, nullValue());

    final ConstraintFact newConstraintFact = constraintFact("constraint_4", "Constraint 4", "AND").with(conditionFact);

    newAlert = oldAlert.with(trigger.with(componentFact.with(newConstraintFact)));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts(),
        contains(newConstraintFact));
    assertThat(results[1].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts(), contains(constraintFact));
  }

  @Test
  public void testDigest_ConditionValueChange() {
    final PolicyAlert oldAlert = defaultPolicyAlert();
    final PolicyFact trigger = oldAlert.getTrigger();

    final ComponentFact componentFact = trigger.getComponentFacts().get(0);
    final ConstraintFact constraintFact = componentFact.getConstraintFacts().get(0);
    final ConditionFact conditionFact = constraintFact.getConditionFacts().get(0);

    PolicyAlert newAlert;
    List<PolicyAlert>[] results;

    final ConditionFact sameConditionFact = conditionFact(MatchStateConditionType.ID, "is", "exact");

    newAlert = oldAlert.with(trigger.with(componentFact.with(constraintFact.with(sameConditionFact))));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));

    assertThat(results, nullValue());

    final ConditionFact newConditionFact = conditionFact(MatchStateConditionType.ID, "is", "similar");

    newAlert = oldAlert.with(trigger.with(componentFact.with(constraintFact.with(newConditionFact))));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts(), contains(newConditionFact));
    assertThat(results[1].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts(), contains(conditionFact));
  }

  @Test
  public void testDigest_ConditionOperatorChange() {
    final PolicyAlert oldAlert = defaultPolicyAlert();
    final PolicyFact trigger = oldAlert.getTrigger();

    final ComponentFact componentFact = trigger.getComponentFacts().get(0);
    final ConstraintFact constraintFact = componentFact.getConstraintFacts().get(0);
    final ConditionFact conditionFact = constraintFact.getConditionFacts().get(0);

    PolicyAlert newAlert;
    List<PolicyAlert>[] results;

    final ConditionFact sameConditionFact = conditionFact(MatchStateConditionType.ID, "is", "exact");

    newAlert = oldAlert.with(trigger.with(componentFact.with(constraintFact.with(sameConditionFact))));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));

    assertThat(results, nullValue());

    final ConditionFact newConditionFact = conditionFact(MatchStateConditionType.ID, "is not", "exact");

    newAlert = oldAlert.with(trigger.with(componentFact.with(constraintFact.with(newConditionFact))));
    results = PolicyAlertDigester.digestPolicyAlerts(Arrays.asList(newAlert), Arrays.asList(oldAlert));

    assertThat(results[0], contains(any(PolicyAlert.class)));
    assertThat(results[0].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts(), contains(newConditionFact));
    assertThat(results[1].get(0).getTrigger().getComponentFacts().get(0).getConstraintFacts().get(0)
        .getConditionFacts(), contains(conditionFact));
  }

  private static List<PolicyAlert> defaultPolicyAlerts() {
    final List<PolicyAlert> policyAlerts = new ArrayList<PolicyAlert>();
    policyAlerts.add(defaultPolicyAlert());
    return policyAlerts;
  }

  private static PolicyAlert defaultPolicyAlert() {
    return new PolicyAlert(defaultPolicyFact(), Collections.<Action> emptyList());
  }

  private static PolicyFact defaultPolicyFact() {
    final ConditionFact conditionFact = conditionFact(MatchStateConditionType.ID, "is", "exact");
    final ConstraintFact constraintFact = constraintFact("constraint_4", "Constraint 4", "OR");
    constraintFact.addConditionFact(conditionFact);
    final ComponentFact componentFact = componentFact("G", "A", "V", "H");
    componentFact.addConstraintFact(constraintFact);
    final PolicyFact policyFact = policyFact("policy_4", "Policy 4", 0);
    policyFact.addComponentFact(componentFact);
    return policyFact;
  }

  private static PolicyFact policyFact(final String id, final String name, final int threatLevel) {
    return new PolicyFact(id, name, threatLevel);
  }

  private static ComponentFact componentFact(final String groupId, final String artifactId, final String version,
      final String hash)
  {
    return new ComponentFact(groupId, artifactId, version, hash);
  }

  private static ConstraintFact constraintFact(final String id, final String name, final String operator) {
    return new ConstraintFact(id, name, operator);
  }

  private static ConditionFact conditionFact(final String conditionTypeId, final String operator, final String value) {
    final Condition condition = new Condition();
    condition.setConditionTypeId(conditionTypeId);
    condition.setOperator(operator);
    condition.setValue(value);
    return PolicyEvaluator.createConditionFact(condition, new Component("G", "A", "V", MatchState.EXACT));
  }

  private static ConditionFact conditionFact(final String conditionTypeId, final String operator) {
    final Condition condition = new Condition();
    condition.setConditionTypeId(conditionTypeId);
    condition.setOperator(operator);
    return PolicyEvaluator.createConditionFact(condition, new Component("G", "A", "V", MatchState.EXACT));
  }
}
