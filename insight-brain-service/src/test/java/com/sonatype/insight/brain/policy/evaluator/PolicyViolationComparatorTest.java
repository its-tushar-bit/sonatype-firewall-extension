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
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

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
  public void testCompare_PolicyViolation_SortOrder() {
    PolicyViolation v1 = buildPolicyViolation("2", "Policy1", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy1", 2, "hash", componentA);
    PolicyViolation v3 = buildPolicyViolation("1", "Policy1", 1, "hash2", componentA);
    PolicyViolation v4 = buildPolicyViolation("1", "Policy1", 1, "hash", componentB);

    List<PolicyViolation> sorted = Lists.newArrayList(v1, v2, v3, v4);
    Collections.sort(sorted, comparator);

    // should sort in order of policy id, threat level, hash, component
    List<PolicyViolation> expected = Lists.newArrayList(v4, v3, v2, v1);

    assertThat(sorted, is(expected));
  }

  @Test
  public void testCompare_ConstraintFacts_SortOrder() {
    ConstraintFact constraintFact11 = buildConstraintFact("testConstraintId2", "Test Constraint Name2",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    ConstraintFact constraintFact12 = buildConstraintFact("testConstraintId3", "Test Constraint Name2",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy1", 1, "hash1", componentB,
        Lists.newArrayList(constraintFact11, constraintFact12));

    // Less constraints
    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId2", "Test Constraint Name2",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy1", 1, "hash1", componentB,
        Lists.newArrayList(constraintFact2));

    // Different constraint id
    ConstraintFact constraintFact3 = buildConstraintFact("testConstraintId1", "Test Constraint Name2",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v3 = buildPolicyViolation("1", "Policy1", 1, "hash1", componentB,
        Lists.newArrayList(constraintFact3));

    // Different constraint name
    ConstraintFact constraintFact4 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v4 = buildPolicyViolation("1", "Policy1", 1, "hash1", componentB,
        Lists.newArrayList(constraintFact4));

    List<PolicyViolation> sorted = Lists.newArrayList(v1, v2, v3, v4);
    Collections.sort(sorted, comparator);

    List<PolicyViolation> expected = Lists.newArrayList(v4, v3, v2, v1);

    assertThat(sorted, is(expected));
  }

  @Test
  public void testCompare_PolicyIdLessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("2", "Policy", 1, "hash", componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_PolicyNameIgnored() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy1", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy2", 1, "hash", componentA);

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

  @Test
  public void testCompare_ConstraintFactsSizes_LessThanGreaterThan() {
    ConstraintFact constraintFact11 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact11));

    ConstraintFact constraintFact21 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    ConstraintFact constraintFact22 = buildConstraintFact("testConstraintId2", "Test Constraint Name2",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact21, constraintFact22));

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ConstraintFactsAreSortedById() {
    ConstraintFact constraintFact11 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    ConstraintFact constraintFact12 = buildConstraintFact("testConstraintId2", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact11, constraintFact12));

    ConstraintFact constraintFact21 = buildConstraintFact("testConstraintId2", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    ConstraintFact constraintFact22 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact21, constraintFact22));

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_ConstraintFactsIds_LessThanGreaterThan() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId2", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ConstraintFactsNames_LessThanGreaterThan() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name2",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ConstraintFactsNames_EqualsIgnoreCase() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId", "Test Constraint Name",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId", "test constraint name",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_ConstraintFactsNames_EqualsIgnoreWhiteSpace() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId", "Test Constraint Name",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId", "T e s t ConstraintName",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_ConditionFactsSizes_LessThanGreaterThan() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"),
        new ConditionFact(AgeInDaysConditionType.ID, 1 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ConditionFactsIndexes_LessThanGreaterThan() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 1 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ConditionFactsConditionTypes_LessThanGreaterThan() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(LicenseConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ConditionFactsAreSortedByIndex() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"),
        new ConditionFact(AgeInDaysConditionType.ID, 1 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 1 /* conditionIndex */, "test summary", "test reason"),
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact2));

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_ConditionFactsTriggers_ConditionTypeDoesNotStoreTriggerData() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact2));

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_ConditionFactsTriggers_ConditionTypeStoresTriggerData() {
    ConditionFact conditionFact1 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact1.setTriggerJson(
        "{\"conditionIndex\" : 0, \"trigger\" : {\"refId\" : \"CVE-2013-0001\",\"statusId\" : \"OPEN\"}}");
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact1);
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact1));

    ConditionFact conditionFact2 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact2.setTriggerJson(
        "{\"conditionIndex\" : 0, \"trigger\" : {\"refId\" : \"CVE-2013-0002\",\"statusId\" : \"OPEN\"}}");
    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact2);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact2));

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ConditionFactsTriggers_ConditionTypeStoresTriggerData_LegacyPolicyViolationWithoutTriggerData() {
    // Legacy policy violation without trigger data
    ConditionFact conditionFact1 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact1);
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact1));

    // New policy violation with trigger data
    ConditionFact conditionFact2 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact2.setTriggerJson("trigger");
    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact2);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact2));

    compareAndAssert(v1, v2, 0);
  }

  @Test
  // Before Brain 1.53, constraint facts were serialized as formatted json and condition fact triggers were formatted
  // twice, which caused line separators to be encoded in json.
  public void testCompare_ConditionFactsTriggers_LegacyTriggerContainsEncodedLineSeparators() {
    // New policy violation with condition trigger serialized as unformatted json.
    ConditionFact conditionFact1 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact1
        .setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2013-0329\",\"statusId\":\"OPEN\"}}");
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact1);
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact1));

    // Legacy policy violation with Windows line separators.
    ConditionFact conditionFact2 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact2.setTriggerJson(
        "{\r\n  \"conditionIndex\" : 0,\r\n  \"trigger\" : {\r\n    \"refId\" : \"CVE-2013-0329\",\r\n    \"statusId\" : \"OPEN\"\r\n  }\r\n}");
    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact2);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact2));
    compareAndAssert(v1, v2, 0);

    // Legacy policy violation with unix line separators.
    conditionFact2 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0 /* conditionIndex */,
        "test summary", "test reason");
    conditionFact2.setTriggerJson(
        "{\n  \"conditionIndex\" : 0,\n  \"trigger\" : {\n    \"refId\" : \"CVE-2013-0329\",\n    \"statusId\" : \"OPEN\"\n  }\n}");
    constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact2);
    v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA, Lists.newArrayList(constraintFact2));
    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_ConditionFactsTriggers_TriggerAttributeOrderDoesNotMatter() {
    ConditionFact conditionFact1 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact1
        .setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2013-0329\",\"statusId\":\"OPEN\"}}");
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact1);
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact1));

    ConditionFact conditionFact2 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact2
        .setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"statusId\":\"OPEN\",\"refId\":\"CVE-2013-0329\"}}");
    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact2);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact2));

    compareAndAssert(v1, v2, 0);
  }

  private PolicyViolation buildPolicyViolation(String policyId,
                                               String policyName,
                                               int threatLevel,
                                               String hash,
                                               ComponentIdentifier componentIdentifier)
  {
    ConstraintFact constraintFact = buildConstraintFact("testConstraintId", "Test Constraint Name",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact);

    return buildPolicyViolation(policyId, policyName, threatLevel, hash, componentIdentifier, constraintFacts);
  }

  private PolicyViolation buildPolicyViolation(String policyId,
                                               String policyName,
                                               int threatLevel,
                                               String hash,
                                               ComponentIdentifier componentIdentifier,
                                               List<ConstraintFact> constraintFacts)
  {
    PolicyViolation violation = new PolicyViolation();
    violation.setPolicyName(policyName);
    violation.setPolicyId(policyId);
    violation.setThreatLevel(threatLevel);
    violation.setHash(hash);
    violation.setComponentIdentifier(componentIdentifier);
    violation.setConstraintFacts(constraintFacts);
    return violation;
  }

  private ConstraintFact buildConstraintFact(String constraintId,
                                             String constraintName,
                                             ConditionFact... conditionFacts)
  {
    ConstraintFact constraintFact = new ConstraintFact(constraintId, constraintName, LogicalOperator.AND.toString());
    for (ConditionFact conditionFact : conditionFacts) {
      constraintFact.addConditionFact(conditionFact);
    }

    return constraintFact;
  }

  private void compareAndAssert(PolicyViolation v1, PolicyViolation v2, int expectedResult) {
    int result = comparator.compare(v1, v2);
    result = (int) Math.signum(result);
    assertThat(result, is(expectedResult));

    result = comparator.compare(v2, v1);
    result = (int) Math.signum(result);
    assertThat(result, is(-expectedResult));
  }

}
