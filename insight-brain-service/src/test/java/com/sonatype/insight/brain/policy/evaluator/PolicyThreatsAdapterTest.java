/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class PolicyThreatsAdapterTest
{

  private PolicyThreatsAdapter policyThreatsAdapter = new PolicyThreatsAdapter();

  @Test
  public void testCreatePolicyThreats() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, false,
        Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);

    PolicyThreats threats = policyThreatsAdapter.createPolicyThreats(violations);

    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_LargestPolicyThreat() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation10 = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        Action.ID_FAIL);
    PolicyViolation mavenViolation1 = buildPolicyViolation("policy2", "hash1", 1, mavenIdentifier, false,
        Action.ID_FAIL);
    PolicyViolation nugetViolation10 = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, false,
        Action.ID_FAIL);
    PolicyViolation nugetViolation1 = buildPolicyViolation("policy2", "hash2", 1, nugetIdentifier, false,
        Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation10, mavenViolation1, nugetViolation10,
        nugetViolation1);

    PolicyThreats threats = policyThreatsAdapter.createPolicyThreats(violations);

    PolicyViolation largestMavenPolicyViolation = getLargestThreatViolation("hash1", violations);
    PolicyViolation largestNuGetPolicyViolation = getLargestThreatViolation("hash2", violations);

    // Make sure we have 2 components.
    Assert.assertThat(threats.aaData, hasSize(2));

    // Make sure the largest policy violations are set on the component.
    for (PolicyThreats.Component component : threats.aaData) {
      // hash1 == maven component.
      if (component.hash.equals("hash1")) {
        Assert.assertThat(component.policyId, is(largestMavenPolicyViolation.getPolicyId()));
        Assert.assertThat(component.policyName, is(largestMavenPolicyViolation.getPolicyName()));
        Assert.assertThat(component.policyThreatLevel, is(largestMavenPolicyViolation.getThreatLevel()));
      }
      else {
        Assert.assertThat(component.policyId, is(largestNuGetPolicyViolation.getPolicyId()));
        Assert.assertThat(component.policyName, is(largestNuGetPolicyViolation.getPolicyName()));
        Assert.assertThat(component.policyThreatLevel, is(largestNuGetPolicyViolation.getThreatLevel()));
      }
    }
  }

  @Test
  public void testCreatePolicyThreats_Waived() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, true, Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, true, Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);

    PolicyThreats threats = policyThreatsAdapter.createPolicyThreats(violations);

    // Make sure each component has a waived policy.
    for (PolicyThreats.Component component : threats.aaData) {
      Assert.assertThat(component.waivedViolations, hasSize(1));
    }
    
    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_AllWaivedButTopViolationExists() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, true, Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, true, Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);

    PolicyThreats threats = policyThreatsAdapter.createPolicyThreats(violations);

    // Make sure each component has a 'top violation' even though all violations are waived.
    for (PolicyThreats.Component component : threats.aaData) {
      Assert.assertThat(component.waivedViolations, hasSize(1));
      Assert.assertNull(component.policyId);
      Assert.assertThat(component.policyName, is("None"));
      Assert.assertThat(component.policyThreatLevel, is(0));
    }

    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_Actions() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        Action.ID_FAIL);
    mavenViolation.setNotifications(Lists.newArrayList("a", "b"));
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, false,
        Action.ID_FAIL);
    nugetViolation.setNotifications(Lists.newArrayList("a", "b"));

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);

    PolicyThreats threats = policyThreatsAdapter.createPolicyThreats(violations);

    // Make sure we have two components.
    Assert.assertThat(threats.aaData, hasSize(2));

    // Each component has a fail action and two notify actions.
    for (PolicyThreats.Component component : threats.aaData) {
      Assert.assertThat(component.activeViolations, hasSize(1));
      Assert.assertThat(component.activeViolations.get(0).actions, hasSize(3));
      List<PolicyThreats.PolicyAction> actions = component.activeViolations.get(0).actions;
      Assert.assertThat(actions.get(0).actionType, is(Action.ID_FAIL));
      Assert.assertThat(actions.get(0).actionSummary, is(ActionTypes.getById(Action.ID_FAIL).getSummary()));
      Assert.assertThat(actions.get(1).actionType, is(Action.ID_NOTIFY));
      Assert.assertThat(actions.get(1).actionSummary, is(ActionTypes.getById(Action.ID_NOTIFY).getSummary()));
      Assert.assertThat(actions.get(2).actionType, is(Action.ID_NOTIFY));
      Assert.assertThat(actions.get(2).actionSummary, is(ActionTypes.getById(Action.ID_NOTIFY).getSummary()));
    }
  }

  @Test
  public void testCreatePolicyThreats_NullPolicyViolations() {
    PolicyThreats threats = policyThreatsAdapter.createPolicyThreats(null);

    Assert.assertThat(threats.aaData, hasSize(0));
    Assert.assertThat(threats.version, is(2));
  }

  private PolicyViolation buildPolicyViolation(String policyId, String hash, int threatLevel,
      ComponentIdentifier componentIdentifier, boolean waived, String actionType)
  {
    PolicyEvaluation evaluation = new PolicyEvaluation("applicationId1", "stageId1", "scanId1");

    PolicyViolation violation = new PolicyViolation(evaluation, policyId, policyId, threatLevel,
        PolicyThreatCategory.OTHER, hash, componentIdentifier, buildConstraintFact(policyId), new ArrayList<String>());
    violation.setWaived(waived);
    violation.setActionTypeId(actionType);

    return violation;
  }

  private List<ConstraintFact> buildConstraintFact(String policyId) {
    ConstraintFact fact = new ConstraintFact("constraint-" + policyId, "constraint-" + policyId,
        "test-operator");
    ConditionFact condition = new ConditionFact(ConditionTypes.MatchStateConditionType.getId(),
        "Match state condition.", "Unknown match state.");
    fact.addConditionFact(condition);

    return Lists.newArrayList(fact);
  }

  private void assertPolicyThreats(PolicyThreats threats, List<PolicyViolation> violations) {
    for (PolicyThreats.Component component : threats.aaData) {
      assertPolicyThreatsComponent(component, violations);
      assertPolicyThreatsPolicyViolations(component.activeViolations, violations);
      assertPolicyThreatsPolicyViolations(component.waivedViolations, violations);
    }
  }

  private void assertPolicyThreatsComponent(PolicyThreats.Component component, List<PolicyViolation> violations) {
    PolicyViolation violation = getLargestThreatViolation(component.hash, violations);
    if (violation != null) {
      assertPolicyThreatsComponent(component, violation);
      return;
    }
    Assert.fail("Unable to find matching violation for policy threats component " + component.toString() + ".");
  }

  private PolicyViolation getLargestThreatViolation(String hash, List<PolicyViolation> violations) {
    PolicyViolation result = null;
    for (PolicyViolation violation : violations) {
      if (violation.getHash().equals(hash)) {
        if (result == null) {
          result = violation;
        }
        else if (violation.getThreatLevel() > result.getThreatLevel()) {
          result = violation;
        }
      }
    }

    return result;
  }

  private void assertPolicyThreatsComponent(PolicyThreats.Component component, PolicyViolation violation) {
    Assert.assertThat(component.hash, is(violation.getHash()));
    Assert.assertThat(component.componentIdentifier, is(violation.getComponentIdentifier()));
    if (!violation.isWaived()) {
      Assert.assertThat(component.policyId, is(violation.getPolicyId()));
      Assert.assertThat(component.policyName, is(violation.getPolicyName()));
      Assert.assertThat(component.policyThreatLevel, is(violation.getThreatLevel()));
    }
  }

  private void assertPolicyThreatsPolicyViolations(List<PolicyThreats.PolicyViolation> policyViolations,
      List<PolicyViolation> violations)
  {
    for (PolicyThreats.PolicyViolation policyViolation : policyViolations) {
      assertPolicyThreatsPolicyViolations(policyViolation, violations);
    }
  }

  private void assertPolicyThreatsPolicyViolations(PolicyThreats.PolicyViolation policyViolation,
      List<PolicyViolation> violations)
  {
    for (PolicyViolation violation : violations) {
      if (policyViolation.policyId.equals(violation.getPolicyId())) {
        assertPolicyThreatsPolicyViolations(policyViolation, violation);
        return;
      }
    }

    Assert.fail("Unable to find matching violation for policy threats violation " + policyViolation.toString() + ".");
  }

  private void assertPolicyThreatsPolicyViolations(PolicyThreats.PolicyViolation policyViolation,
      PolicyViolation violation)
  {
    Assert.assertThat(policyViolation.policyId, is(violation.getPolicyId()));
    Assert.assertThat(policyViolation.policyName, is(violation.getPolicyName()));

    for (PolicyThreats.PolicyAction action : policyViolation.actions) {
      Assert.assertThat(action.actionType, is(violation.getActionTypeId()));
      Assert.assertThat(action.actionSummary, is(ActionTypes.getById(violation.getActionTypeId()).getSummary()));
    }

    assertPolicyThreatsPolicyConstraints(policyViolation.constraints, violation.getConstraintFacts());
  }

  private void assertPolicyThreatsPolicyConstraints(List<PolicyThreats.PolicyConstraint> policyConstraints,
      List<ConstraintFact> facts)
  {
    for (PolicyThreats.PolicyConstraint policyConstraint : policyConstraints) {
      assertPolicyThreatsPolicyConstraints(policyConstraint, facts);
    }
  }

  private void assertPolicyThreatsPolicyConstraints(PolicyThreats.PolicyConstraint constraint,
      List<ConstraintFact> facts)
  {
    for (ConstraintFact fact : facts) {
      if (constraint.constraintId.equals(fact.getConstraintId())) {
        assertPolicyThreatsPolicyConstraints(constraint, fact);
        return;
      }
    }

    Assert.fail("Unable to find matching policy threats constraint fact " + constraint.toString() + ".");
  }

  private void assertPolicyThreatsPolicyConstraints(PolicyThreats.PolicyConstraint constraint, ConstraintFact fact) {
    Assert.assertThat(constraint.constraintId, is(fact.getConstraintId()));
    Assert.assertThat(constraint.constraintName, is(fact.getConstraintName()));
    Assert.assertThat(constraint.constraintOperator, is(fact.getOperatorName()));

    assertPolicyThreatsPolicyConditions(constraint.conditions, fact.getConditionFacts());
  }

  private void assertPolicyThreatsPolicyConditions(List<PolicyThreats.PolicyCondition> conditions,
      List<ConditionFact> conditionFacts)
  {
    for (PolicyThreats.PolicyCondition condition : conditions) {
      assertPolicyThreatsPolicyConditions(condition, conditionFacts);
    }
  }

  private void assertPolicyThreatsPolicyConditions(PolicyThreats.PolicyCondition condition,
      List<ConditionFact> conditionFacts)
  {
    for (ConditionFact fact : conditionFacts) {
      if (fact.getConditionTypeId().equals(condition.conditionType)
          && fact.getReason().equals(condition.conditionReason) && fact.getSummary().equals(condition.conditionSummary)) {
        return;
      }
    }

    Assert.fail("Unable to find matching policy threats condition fact " + condition.toString() + ".");
  }
}
