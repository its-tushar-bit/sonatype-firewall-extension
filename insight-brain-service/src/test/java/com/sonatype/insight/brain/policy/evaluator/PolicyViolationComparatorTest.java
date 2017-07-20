/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicyViolationComparatorTest
{
  private Comparator<PolicyViolationComparable> comparator = PolicyViolationComparator.COMPARATOR;

  private ComponentIdentifier componentA = ComponentIdentifier.createMavenCoordinates("A", "A", "A");
  private ComponentIdentifier componentB = ComponentIdentifier.createMavenCoordinates("B", "B", "B");

  @Test
  public void testCompare_SortOrder() {
    PolicyViolation v1 = buildPolicyViolation("2", "Policy1", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy2", 1, "hash", componentA);
    PolicyViolation v3 = buildPolicyViolation("1", "Policy1", 2, "hash", componentA);
    PolicyViolation v4 = buildPolicyViolation("1", "Policy1", 1, "hash2", componentA);
    PolicyViolation v5 = buildPolicyViolation("1", "Policy1", 1, "hash", componentB);

    List<PolicyViolation> sorted = Lists.newArrayList(v1, v2, v3, v4, v5);
    Collections.sort(sorted, comparator);

    // should sort in order of policy id, policy name, threat level, hash, component
    List<PolicyViolation> expected = Lists.newArrayList(v5, v4, v3, v2, v1);

    assertThat(sorted, is(expected));
  }

  @Test
  public void testCompare_PolicyIdLessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("2", "Policy", 1, "hash", componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_PolicyNameLessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy1", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy2", 1, "hash", componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_PolicyNameEqualsIgnoreCase() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy1", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "policy1", 1, "hash", componentA);

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_PolicyNameEqualsIgnoreWhiteSpace() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy1", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", " P o l i c y 1 ", 1, "hash", componentA);

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_ThreatLevelLessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 2, "hash", componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_HashLessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash1", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash2", componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_HashNullLessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, null, componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ComponentIdentifierLessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentB);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ComponentIdentifier1NullLessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", null);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_PolicyViolationEquals() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);

    compareAndAssert(v1, v2, 0);
  }

  private PolicyViolation buildPolicyViolation(String policyId,
                                               String policyName,
                                               int threatLevel,
                                               String hash,
                                               ComponentIdentifier componentIdentifier)
  {
    PolicyViolation violation = new PolicyViolation();
    violation.setPolicyName(policyName);
    violation.setPolicyId(policyId);
    violation.setThreatLevel(threatLevel);
    violation.setHash(hash);
    violation.setComponentIdentifier(componentIdentifier);
    return violation;
  }

  private void compareAndAssert(PolicyViolation v1, PolicyViolation v2, int expectedResult) {
    int result = comparator.compare(v1, v2);
    assertThat(result, is(expectedResult));

    result = comparator.compare(v2, v1);
    assertThat(result, is(-expectedResult));
  }

}
