/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class PolicyViolationDigesterTest
{
  @Test
  public void testDigest_Nothing() {
    final List<PolicyViolation> oldViolations = Collections.emptyList();
    final List<PolicyViolation> newViolations = Collections.emptyList();

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results, notNullValue());
    assertThat(results.getAppeared(), empty());
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_UnknownPolicyViolation() {
    final List<PolicyViolation> oldViolations = Collections.emptyList();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), contains(newViolations.get(0)));
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_NoChange() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results, notNullValue());
    assertThat(results.getAppeared(), empty());
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_ClearedPolicyViolation() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = Collections.emptyList();

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), empty());
    assertThat(results.getCleared(), contains(oldViolations.get(0)));
  }

  @Test
  public void testDigest_UnknownPolicyViolationBefore() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.add(0, newPolicyViolation("policy_1", "Policy 1", 0));

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), contains(newViolations.get(0)));
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_UnknownPolicyViolationAfter() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.add(newPolicyViolation("policy_8", "Policy 8", 0));

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), contains(newViolations.get(1)));
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_UnknownPolicyViolationBeforeAndAfter() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.add(0, newPolicyViolation("policy_1", "Policy 1", 0));
    newViolations.add(newPolicyViolation("policy_8", "Policy 8", 0));

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), contains(newViolations.get(0), newViolations.get(2)));
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_UnknownPolicyViolationBeforeAndAfterClearedPolicyViolation() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = new ArrayList<PolicyViolation>();

    newViolations.add(newPolicyViolation("policy_1", "Policy 1", 0));
    newViolations.add(newPolicyViolation("policy_8", "Policy 8", 0));

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), contains(newViolations.get(0), newViolations.get(1)));
    assertThat(results.getCleared(), contains(oldViolations.get(0)));
  }

  @Test
  public void testDigest_UnknownComponentFactBefore() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();
    PolicyViolation newPolicyViolation = defaultPolicyViolation();
    newPolicyViolation.setHash("1H");
    newViolations.add(0, newPolicyViolation);

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), hasSize(1));
    assertThat(results.getAppeared().get(0).getHash(), is("1H"));
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_UnknownComponentFactAfter() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();
    PolicyViolation newPolicyViolation = defaultPolicyViolation();
    newPolicyViolation.setHash("H1");
    newViolations.add(newPolicyViolation);

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), hasSize(1));
    assertThat(results.getAppeared().get(0).getHash(), is("H1"));
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_DifferentConstraintFacts() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    final ConstraintFact newFact = constraintFact("constraint_1", "Constraint 1", "AND");
    newViolations.get(0).setConstraintFacts(Collections.singletonList(newFact));

    final PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), hasSize(1));
    assertThat(results.getAppeared().get(0).getConstraintFacts().get(0).getConstraintId(), is("constraint_1"));
    assertThat(results.getCleared(), hasSize(1));
    assertThat(results.getCleared().get(0).getConstraintFacts().get(0).getConstraintId(), is("constraint_4"));
  }

  @Test
  public void testDigest_PolicyNameChange() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.get(0).setPolicyName("Policy 4~");

    PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), hasSize(1));
    assertThat(results.getAppeared().get(0).getPolicyName(), is("Policy 4~"));
    assertThat(results.getCleared(), hasSize(1));
    assertThat(results.getCleared().get(0).getPolicyName(), is("Policy 4"));
  }

  @Test
  public void testDigest_PolicyNameCaseChange() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    // Policy name case changes are ignored
    newViolations.get(0).setPolicyName("policy 4");

    PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), empty());
    assertThat(results.getCleared(), empty());
  }

  @Test
  public void testDigest_PolicyThreatLevelChange() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.get(0).setThreatLevel(10);

    PolicyViolationDiff results = PolicyViolationDigester.digestPolicyViolations(newViolations, oldViolations);

    assertThat(results.getAppeared(), hasSize(1));
    assertThat(results.getAppeared().get(0).getThreatLevel(), is(10));
    assertThat(results.getCleared(), hasSize(1));
    assertThat(results.getCleared().get(0).getThreatLevel(), is(0));
  }

  private static List<PolicyViolation> defaultPolicyViolations() {
    final List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(defaultPolicyViolation());
    return policyViolations;
  }

  private static PolicyViolation defaultPolicyViolation() {
    final ConditionFact conditionFact = conditionFact(MatchStateConditionType.ID, "is", "exact");
    final ConstraintFact constraintFact = constraintFact("constraint_4", "Constraint 4", "OR");
    constraintFact.addConditionFact(conditionFact);
    
    PolicyViolation policyViolation = new PolicyViolation(null, "policy_4", "Policy 4", 0, PolicyThreatCategory.OTHER,
        "H", "G", "A", "V", Collections.singletonList(constraintFact));

    return policyViolation;
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

  private static PolicyViolation newPolicyViolation(String policyId, String policyName, int threatLevel) {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPolicyId(policyId);
    policyViolation.setPolicyName(policyName);
    policyViolation.setThreatLevel(threatLevel);
    return policyViolation;
  }
}
