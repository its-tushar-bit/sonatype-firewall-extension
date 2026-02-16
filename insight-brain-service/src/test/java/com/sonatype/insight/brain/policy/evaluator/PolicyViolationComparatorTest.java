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

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationComparatorTest
{
  private final Comparator<PolicyViolationComparable> comparator = PolicyViolationComparator.COMPARATOR;

  private final ComponentIdentifier componentA = ComponentIdentifier.createMavenCoordinates("A", "A", "A");

  private final ComponentIdentifier componentB = ComponentIdentifier.createMavenCoordinates("B", "B", "B");

  @Test
  public void testCompare_PolicyViolation_SortOrder() {
    PolicyViolation v1 = buildPolicyViolation("2", "Policy1", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy1", 2, "hash", componentA);
    PolicyViolation v3 = buildPolicyViolation("1", "Policy1", 1, "hash2", componentA);
    PolicyViolation v4 = buildPolicyViolation("1", "Policy1", 1, "hash", componentB);

    List<PolicyViolation> sorted = Lists.newArrayList(v1, v2, v3, v4);
    sorted.sort(comparator);

    // should sort in order of policy id, threat level, hash, component
    assertThat(sorted).containsExactly(v4, v3, v2, v1);
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

    List<PolicyViolation> sorted = Lists.newArrayList(v1, v2, v3);
    sorted.sort(comparator);

    assertThat(sorted).containsExactly(v3, v2, v1);
  }

  @Test
  public void testCompare_PolicyId_LessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("2", "Policy", 1, "hash", componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_PolicyNameIsIgnored() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy1", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy2", 1, "hash", componentA);

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_ThreatLevel_LessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 2, "hash", componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_Hash_LessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash1", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash2", componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_HashNull_LessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, null, componentA);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ComponentIdentifier_LessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentB);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_ComponentIdentifier1Null_LessThanGreaterThan() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", null);

    compareAndAssert(v1, v2, -1);
  }

  @Test
  public void testCompare_PolicyViolation_Equal() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);

    compareAndAssert(v1, v2, 0);
  }

  @Test
  public void testCompare_PolicyViolation_EqualConstraintFactsIds() {
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA);

    // Ensure the constraint facts are not equal
    ConstraintFact constraintFact11 = buildConstraintFact("testConstraintId2", "Test Constraint Name2",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    ConstraintFact constraintFact12 = buildConstraintFact("testConstraintId3", "Test Constraint Name2",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v2 = buildPolicyViolation("1", "Policy1", 1, "hash", componentA,
        Lists.newArrayList(constraintFact11, constraintFact12));

    v1.setConstraintFactsId("id");
    v2.setConstraintFactsId("id");

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
  public void testCompare_ConstraintFactsNameAreIgnored() {
    ConstraintFact constraintFact1 = buildConstraintFact("testConstraintId1", "Test Constraint Name1",
        new ConditionFact(AgeInDaysConditionType.ID, 0 /* conditionIndex */, "test summary", "test reason"));
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name2",
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
  @SuppressWarnings("checkstyle:LineLength")
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
  // twice, which caused line separators to be encoded in json. Also, the condition trigger json contained white spaces.
  public void testCompare_ConditionFactsTriggers_LegacyTriggerContainsEncodedLineSeparatorsAndWhitespace() {
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
    conditionFact2.setTriggerJson("{\r\n  \"conditionIndex\" : 0,\r\n  \"trigger\" : {\r\n    "
        + "\"refId\" : \"CVE-2013-0329\",\r\n    \"statusId\" : \"OPEN\"\r\n  }\r\n}");
    ConstraintFact constraintFact2 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact2);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact2));
    compareAndAssert(v1, v2, 0);

    // Legacy policy violation with unix line separators.
    ConditionFact conditionFact3 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact3.setTriggerJson("{\n  \"conditionIndex\" : 0,\n  \"trigger\" : {\n    "
        + "\"refId\" : \"CVE-2013-0329\",\n    \"statusId\" : \"OPEN\"\n  }\n}");
    ConstraintFact constraintFact3 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact3);
    PolicyViolation v3 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact3));
    compareAndAssert(v1, v3, 0);

    // Violation formatted like v1, but with slightly different data.  Since the other three violations are all equal,
    // they should all compare to this one the same way. If a naive lexical comparison were done, v4 would come out
    // above v2 and v3 because '"' is higher than newline characters.
    ConditionFact conditionFact4 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact4
        .setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2013-0328\",\"statusId\":\"OPEN\"}}");
    ConstraintFact constraintFact4 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact4);
    PolicyViolation v4 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact4));
    compareAndAssert(v1, v4, 1);
    compareAndAssert(v2, v4, 1);
    compareAndAssert(v3, v4, 1);
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

    // this ConditionFact is not equal to the other two, and needs to compare similarly to each of them.  If a naive
    // lexical comparison were done it will compare as greater than v1 and less than v2
    ConditionFact conditionFact3 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "test summary", "test reason");
    conditionFact3
        .setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2013-0330\",\"statusId\":\"OPEN\"}}");
    ConstraintFact constraintFact3 = buildConstraintFact("testConstraintId1", "Test Constraint Name1", conditionFact3);
    PolicyViolation v3 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Lists.newArrayList(constraintFact3));

    compareAndAssert(v1, v2, 0);
    compareAndAssert(v1, v3, -1);
    compareAndAssert(v2, v3, -1);
  }

  /**
   * Test for CLM-38434: Verify that policy violations with the same coordinate pattern but different condition indexes
   * are considered equal after triggerJson comparison was added.
   * <p>
   * This simulates the scenario where: 1. A policy has multiple coordinate conditions (e.g., a:a:1.*,
   * com.thoughtworks.xstream:xstream:1.*, z:z:1.*) 2. A component matches one of them
   * (com.thoughtworks.xstream:xstream:1.2) at index 1 3. The policy is modified and a condition before it is deleted
   * (e.g., a:a:1.* is removed) 4. The same component is re-evaluated and now matches at index 0 (because a:a:1.* was
   * removed) 5. The violations should still be considered equal because the coordinate pattern is the same
   */
  @Test
  public void testCompare_CoordinateConditions_SamePatternDifferentIndex_AreEqual() {
    // Violation 1: Coordinates condition at index 1 with trigger data
    String coordinatePattern = "maven:com.thoughtworks.xstream:xstream:1.*:*:*";
    String triggerJson1 = "{\"conditionIndex\":1,\"trigger\":{\"pattern\":\"" + coordinatePattern + "\"}}";
    ConditionFact conditionFact1 = new ConditionFact(
        "Coordinates",
        1, /* conditionIndex */
        "Coordinates match " + coordinatePattern,
        "Coordinates were com.thoughtworks.xstream : xstream : 1.2 (match " + coordinatePattern + ")"
    );
    conditionFact1.setTriggerJson(triggerJson1);
    ConstraintFact constraintFact1 = buildConstraintFact("constraintId", "Constraint Name", conditionFact1);
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    // Violation 2: Same coordinates condition at index 0 (after earlier conditions were deleted) with trigger data
    String triggerJson2 = "{\"conditionIndex\":0,\"trigger\":{\"pattern\":\"" + coordinatePattern + "\"}}";
    ConditionFact conditionFact2 = new ConditionFact(
        "Coordinates",
        0, /* conditionIndex - different! */
        "Coordinates match " + coordinatePattern,
        "Coordinates were com.thoughtworks.xstream : xstream : 1.2 (match " + coordinatePattern + ")"
    );
    conditionFact2.setTriggerJson(triggerJson2);
    ConstraintFact constraintFact2 = buildConstraintFact("constraintId", "Constraint Name", conditionFact2);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    // These should be equal because the coordinate pattern is the same, even though the index differs
    compareAndAssert(v1, v2, 0);
  }

  /**
   * Test that when coordinate patterns are different, violations are correctly identified as different, regardless of
   * index.
   */
  @Test
  public void testCompare_CoordinateConditions_DifferentPattern_AreDifferent() {
    // Violation 1: First coordinate pattern
    String coordinatePattern1 = "maven:com.thoughtworks.xstream:xstream:1.*:*:*";
    String triggerJson1 = "{\"conditionIndex\":0,\"trigger\":{\"pattern\":\"" + coordinatePattern1 + "\"}}";
    ConditionFact conditionFact1 = new ConditionFact(
        "Coordinates",
        0,
        "Coordinates match " + coordinatePattern1,
        "Coordinates were com.thoughtworks.xstream : xstream : 1.2 (match " + coordinatePattern1 + ")"
    );
    conditionFact1.setTriggerJson(triggerJson1);
    ConstraintFact constraintFact1 = buildConstraintFact("constraintId", "Constraint Name", conditionFact1);
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    // Violation 2: Different coordinate pattern
    String coordinatePattern2 = "maven:com.thoughtworks.xstream:xstream:2.*:*:*";
    String triggerJson2 = "{\"conditionIndex\":0,\"trigger\":{\"pattern\":\"" + coordinatePattern2 + "\"}}";
    ConditionFact conditionFact2 = new ConditionFact(
        "Coordinates",
        0,
        "Coordinates match " + coordinatePattern2,
        "Coordinates were com.thoughtworks.xstream : xstream : 2.0 (match " + coordinatePattern2 + ")"
    );
    conditionFact2.setTriggerJson(triggerJson2);
    ConstraintFact constraintFact2 = buildConstraintFact("constraintId", "Constraint Name", conditionFact2);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    // These should be different because the coordinate patterns are different
    int result = comparator.compare(v1, v2);
    assertThat(result).isNotEqualTo(0);
  }

  /**
   * Test backward compatibility: violations without triggerJson (legacy) should still fall back to index comparison.
   */
  @Test
  public void testCompare_CoordinateConditions_LegacyWithoutTriggerJson_UseIndexComparison() {
    // Violation 1: Coordinates condition at index 0, no trigger data (legacy)
    ConditionFact conditionFact1 = new ConditionFact(
        "Coordinates",
        0,
        "Coordinates match maven:com.thoughtworks.xstream:xstream:1.*:*:*",
        "Coordinates were com.thoughtworks.xstream : xstream : 1.2"
    );
    // Note: no setTriggerJson() call - simulates legacy violation
    ConstraintFact constraintFact1 = buildConstraintFact("constraintId", "Constraint Name", conditionFact1);
    PolicyViolation v1 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact1));

    // Violation 2: Coordinates condition at index 1, no trigger data (legacy)
    ConditionFact conditionFact2 = new ConditionFact(
        "Coordinates",
        1, /* different index */
        "Coordinates match maven:com.thoughtworks.xstream:xstream:1.*:*:*",
        "Coordinates were com.thoughtworks.xstream : xstream : 1.2"
    );
    // Note: no setTriggerJson() call - simulates legacy violation
    ConstraintFact constraintFact2 = buildConstraintFact("constraintId", "Constraint Name", conditionFact2);
    PolicyViolation v2 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact2));

    // Violation 3: Coordinates condition at index 0 again, no trigger data (legacy)
    ConditionFact conditionFact3 = new ConditionFact(
        "Coordinates",
        0, /* same index */
        "Coordinates match maven:com.thoughtworks.xstream:xstream:1.*:*:*",
        "Coordinates were com.thoughtworks.xstream : xstream : 1.2"
    );
    // Note: no setTriggerJson() call - simulates legacy violation
    ConstraintFact constraintFact3 = buildConstraintFact("constraintId", "Constraint Name", conditionFact3);
    PolicyViolation v3 = buildPolicyViolation("1", "Policy", 1, "hash", componentA,
        Collections.singletonList(constraintFact3));

    // Legacy violations without triggerJson should be compared by index, so these are different
    assertThat(comparator.compare(v1, v2)).isNotEqualTo(0);
    // and these are the same
    assertThat(comparator.compare(v1, v3)).isEqualTo(0);
  }

  private PolicyViolation buildPolicyViolation(
      String policyId,
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

  private PolicyViolation buildPolicyViolation(
      String policyId,
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

  private ConstraintFact buildConstraintFact(
      String constraintId,
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
    assertThat(result).isEqualTo(expectedResult);

    result = comparator.compare(v2, v1);
    result = (int) Math.signum(result);
    assertThat(result).isEqualTo(-expectedResult);
  }
}
