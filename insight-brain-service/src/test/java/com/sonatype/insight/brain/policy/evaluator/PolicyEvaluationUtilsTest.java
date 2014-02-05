/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyEvaluationUtilsTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyEvaluationUtils evalUtils;

  private ComponentFact newComponentFact(String hash) {
    ComponentFact component = new ComponentFact("gid", "aid", "1.2.3", hash);
    ConstraintFact constraint = new ConstraintFact("constraint-id", "Constraint", "AND");
    constraint.addConditionFact(new ConditionFact("condition-type", "condition-summary", "condition-reason"));
    component.addConstraintFact(constraint);
    return component;
  }

  private PolicyAlert newAlert(String policyId, int threatLevel, ComponentFact... components) {
    PolicyFact fact = new PolicyFact(policyId, policyId + " Name", threatLevel);
    for (ComponentFact component : components) {
      fact.addComponentFact(component);
    }
    PolicyAlert alert = new PolicyAlert(fact, Arrays.asList(new Action(Action.ID_FAIL)));
    return alert;
  }

  private void assertComponent(PolicyThreats.Component component, ComponentFact fact) {
    assertThat(component.hash, is(fact.getHash()));
    assertThat(component.groupId, is(fact.getGroupId()));
    assertThat(component.artifactId, is(fact.getArtifactId()));
    assertThat(component.version, is(fact.getVersion()));
  }

  private void assertTopViolation(PolicyThreats.Component component, PolicyAlert alert) {
    assertThat(component.policyId, is(alert.getTrigger().getPolicyId()));
    assertThat(component.policyName, is(alert.getTrigger().getPolicyName()));
    assertThat(component.policyThreatLevel, is(alert.getTrigger().getThreatLevel()));
  }

  private void assertPolicy(PolicyThreats.PolicyViolation violation, PolicyAlert alert) {
    assertThat(violation.policyId, is(alert.getTrigger().getPolicyId()));
    assertThat(violation.policyName, is(alert.getTrigger().getPolicyName()));
    assertThat(violation.policyThreatLevel, is(alert.getTrigger().getThreatLevel()));
  }

  private void assertViolations(List<PolicyThreats.PolicyViolation> violations, PolicyAlert... alerts) {
    assertThat(violations, is(notNullValue()));
    assertThat(violations, hasSize(alerts.length));
    for (int i = 0; i < alerts.length; i++) {
      assertPolicy(violations.get(i), alerts[i]);
    }
  }

  @Test
  public void testToPolicyThreats_Basics() {
    ComponentFact component1 = newComponentFact("1234567890");
    PolicyAlert alert1 = newAlert("policy-1", 7, component1);
    PolicyResults results = new PolicyResults();
    results.setActiveAlerts(Arrays.asList(alert1));

    PolicyThreats threats = evalUtils.toPolicyThreats(results);
    assertThat(threats, is(notNullValue()));
    assertThat(threats.version, is(1));
    assertThat(threats.aaData, is(notNullValue()));
    assertThat(threats.aaData, hasSize(1));
    PolicyThreats.Component component = threats.aaData.get(0);
    assertComponent(component, component1);
    assertTopViolation(component, alert1);
    assertViolations(component.activeViolations, alert1);
    PolicyThreats.PolicyViolation violation = component.activeViolations.get(0);
    assertThat(violation.actions, is(notNullValue()));
    assertThat(violation.actions, hasSize(1));
    PolicyThreats.PolicyAction action = violation.actions.get(0);
    assertThat(action.actionType, is(Action.ID_FAIL));
    assertThat(action.actionSummary, is(new FailActionType().getSummary()));
    assertThat(violation.constraints, is(notNullValue()));
    assertThat(violation.constraints, hasSize(1));
    PolicyThreats.PolicyConstraint constraint = violation.constraints.get(0);
    assertThat(constraint.constraintId, is("constraint-id"));
    assertThat(constraint.constraintName, is("Constraint"));
    assertThat(constraint.constraintOperator, is("AND"));
    assertThat(constraint.conditions, is(notNullValue()));
    assertThat(constraint.conditions, hasSize(1));
    PolicyThreats.PolicyCondition condition = constraint.conditions.get(0);
    assertThat(condition.conditionType, is("condition-type"));
    assertThat(condition.conditionSummary, is("condition-summary"));
    assertThat(condition.conditionReason, is("condition-reason"));
    assertViolations(component.waivedViolations);
  }

  @Test
  public void testToPolicyThreats_ActiveVsWaivedViolations() {
    ComponentFact component1 = newComponentFact("1234567890");
    PolicyAlert alert1 = newAlert("policy-1", 7, component1);
    PolicyAlert alert2 = newAlert("policy-2", 9, component1);
    PolicyResults results = new PolicyResults();
    results.setActiveAlerts(Arrays.asList(alert1));
    results.setWaivedAlerts(Arrays.asList(alert2));

    PolicyThreats threats = evalUtils.toPolicyThreats(results);
    assertThat(threats, is(notNullValue()));
    assertThat(threats.aaData, is(notNullValue()));
    assertThat(threats.aaData, hasSize(1));
    PolicyThreats.Component component = threats.aaData.get(0);
    assertComponent(component, component1);
    assertTopViolation(component, alert1);
    assertViolations(component.activeViolations, alert1);
    assertViolations(component.waivedViolations, alert2);
  }

  @Test
  public void testToPolicyThreats_NeedToHaveTopViolationEvenWhenAllWaivedForBackwardCompat() {
    ComponentFact component1 = newComponentFact("1234567890");
    PolicyAlert alert1 = newAlert("policy-1", 7, component1);
    PolicyResults results = new PolicyResults();
    results.setWaivedAlerts(Arrays.asList(alert1));

    PolicyThreats threats = evalUtils.toPolicyThreats(results);
    assertThat(threats, is(notNullValue()));
    assertThat(threats.aaData, is(notNullValue()));
    assertThat(threats.aaData, hasSize(1));
    PolicyThreats.Component component = threats.aaData.get(0);
    assertComponent(component, component1);
    assertThat(component.policyId, is(nullValue()));
    assertThat(component.policyName, is("None"));
    assertThat(component.policyThreatLevel, is(0));
    assertViolations(component.activeViolations);
    assertViolations(component.waivedViolations, alert1);
  }
}
