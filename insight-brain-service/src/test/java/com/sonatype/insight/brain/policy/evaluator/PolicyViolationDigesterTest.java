/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationDigesterTest
    extends AbstractComponentTest
{
  @Test
  public void testDigest_Nothing() {
    final List<PolicyViolation> oldViolations = Collections.emptyList();
    final List<PolicyViolation> newViolations = Collections.emptyList();

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results).isNotNull();
    assertThat(results.getAppeared()).isEmpty();
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).isEmpty();
  }

  @Test
  public void testDigest_UnknownPolicyViolation() {
    final List<PolicyViolation> oldViolations = Collections.emptyList();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).containsExactly(newViolations.get(0));
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).isEmpty();
  }

  @Test
  public void testDigest_NoChange() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results).isNotNull();
    assertThat(results.getAppeared()).isEmpty();
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).hasSize(1);
    assertThat(results.getSame().get(oldViolations.get(0))).isEqualTo(newViolations.get(0));
  }

  @Test
  public void testDigest_ClearedPolicyViolation() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = Collections.emptyList();

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).isEmpty();
    assertThat(results.getCleared()).containsExactly(oldViolations.get(0));
    assertThat(results.getSame()).isEmpty();
  }

  @Test
  public void testDigest_UnknownPolicyViolationBefore() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.add(0, newPolicyViolation("policy_1", "Policy 1", 0));

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).containsExactly(newViolations.get(0));
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).hasSize(1);
    assertThat(results.getSame().get(oldViolations.get(0))).isEqualTo(newViolations.get(1));
  }

  @Test
  public void testDigest_UnknownPolicyViolationAfter() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.add(newPolicyViolation("policy_8", "Policy 8", 0));

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).containsExactly(newViolations.get(1));
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).hasSize(1);
    assertThat(results.getSame().get(oldViolations.get(0))).isEqualTo(newViolations.get(0));
  }

  @Test
  public void testDigest_UnknownPolicyViolationBeforeAndAfter() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.add(0, newPolicyViolation("policy_1", "Policy 1", 0));
    newViolations.add(newPolicyViolation("policy_8", "Policy 8", 0));

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).containsExactly(newViolations.get(0), newViolations.get(2));
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).hasSize(1);
    assertThat(results.getSame().get(oldViolations.get(0))).isEqualTo(newViolations.get(1));
  }

  @Test
  public void testDigest_UnknownPolicyViolationBeforeAndAfterClearedPolicyViolation() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = new ArrayList<>();

    newViolations.add(newPolicyViolation("policy_1", "Policy 1", 0));
    newViolations.add(newPolicyViolation("policy_8", "Policy 8", 0));

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).containsExactly(newViolations.get(0), newViolations.get(1));
    assertThat(results.getCleared()).containsExactly(oldViolations.get(0));
    assertThat(results.getSame()).isEmpty();
  }

  @Test
  public void testDigest_UnknownComponentFactBefore() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();
    PolicyViolation newPolicyViolation = defaultPolicyViolation();
    newPolicyViolation.setHash("1H");
    newViolations.add(0, newPolicyViolation);

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).hasSize(1);
    assertThat(results.getAppeared().get(0).getHash()).isEqualTo("1H");
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).hasSize(1);
    assertThat(results.getSame().get(oldViolations.get(0))).isEqualTo(newViolations.get(1));
  }

  @Test
  public void testDigest_UnknownComponentFactAfter() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();
    PolicyViolation newPolicyViolation = defaultPolicyViolation();
    newPolicyViolation.setHash("H1");
    newViolations.add(newPolicyViolation);

    final PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).hasSize(1);
    assertThat(results.getAppeared().get(0).getHash()).isEqualTo("H1");
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).hasSize(1);
    assertThat(results.getSame().get(oldViolations.get(0))).isEqualTo(newViolations.get(0));
  }

  @Test
  public void testDigest_PolicyNameChange() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.get(0).setPolicyName("Policy 4~");

    PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results).isNotNull();
    assertThat(results.getAppeared()).isEmpty();
    assertThat(results.getCleared()).isEmpty();
    assertThat(results.getSame()).hasSize(1);
    assertThat(results.getSame().get(oldViolations.get(0))).isEqualTo(newViolations.get(0));
  }

  @Test
  public void testDigest_PolicyThreatLevelChange() {
    final List<PolicyViolation> oldViolations = defaultPolicyViolations();
    final List<PolicyViolation> newViolations = defaultPolicyViolations();

    newViolations.get(0).setThreatLevel(10);

    PolicyViolationDiff<PolicyViolation> results = PolicyViolationDigester.digestPolicyViolations(oldViolations,
        newViolations);

    assertThat(results.getAppeared()).hasSize(1);
    assertThat(results.getAppeared().get(0).getThreatLevel()).isEqualTo(10);
    assertThat(results.getCleared()).hasSize(1);
    assertThat(results.getCleared().get(0).getThreatLevel()).isEqualTo(0);
    assertThat(results.getSame()).isEmpty();
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

    PolicyEvaluation evaluation = new PolicyEvaluation();
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policy_4", "Policy 4", 0,
        PolicyThreatCategory.OTHER, "H", ComponentIdentifier.createMavenCoordinates("G", "A", "V"),
        Collections.singletonList(constraintFact), "filename");

    return policyViolation;
  }

  private static ConstraintFact constraintFact(final String id, final String name, final String operator) {
    return new ConstraintFact(id, name, operator);
  }

  private static ConditionFact conditionFact(final String conditionTypeId, final String operator, final String value) {
    final Condition condition = new Condition(conditionTypeId, operator, value);
    return ComponentPolicyEvaluator.createConditionFact(condition,
        new MatchFact(ComponentFactory.forGav("G", "A", "V", MatchState.EXACT), null /* policyId */,
            null /* constraintId */, Collections.emptyList() /* conditionTriggers */));
  }

  private static PolicyViolation newPolicyViolation(String policyId, String policyName, int threatLevel) {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPolicyId(policyId);
    policyViolation.setPolicyName(policyName);
    policyViolation.setThreatLevel(threatLevel);
    return policyViolation;
  }
}
