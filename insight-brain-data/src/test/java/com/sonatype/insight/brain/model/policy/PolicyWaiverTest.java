/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PolicyWaiverTest
{
  private final String longHash = "123456789012345678901";

  /**
   * 20 characters as currently specified in HashHelper
   */
  private final String expectedTruncatedHash = "12345678901234567890";

  @Before
  public void preconditions() {
    assertTrue(longHash.length() > 20);
  }

  @Test
  public void testLongHashTruncatedWhenObjectCreated() {
    PolicyWaiver policyWaiver = new PolicyWaiver(longHash, null /* policyId */, null /* ownerId */, null /* comment */);
    assertEquals(expectedTruncatedHash, policyWaiver.getHash());
  }

  @Test
  public void testLongHashTruncatedWhenHashSet() {
    PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setHash(longHash);
    assertEquals(expectedTruncatedHash, policyWaiver.getHash());
  }

  @Test
  public void testSetConstraintFactsJson() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    policyWaiver.setConstraintFactsJson(constraintFactsJson);
    assertThat(policyWaiver.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson_Null() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFactsJson(null);

    assertThat(policyWaiver.getConstraintFactsJson(), is(nullValue()));
    assertThat(policyWaiver.getConstraintFacts(), is(nullValue()));
  }

  @Test
  public void testSetConstraintFactsJson_Empty() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFactsJson(" ");

    assertThat(policyWaiver.getConstraintFactsJson(), is(nullValue()));
    assertThat(policyWaiver.getConstraintFacts(), is(nullValue()));
  }

  @Test
  public void testSetConstraintFacts() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    policyWaiver.setConstraintFacts(constraintFacts);
    assertThat(policyWaiver.getConstraintFactsJson(), is(constraintFactsJson));
    assertThat(policyWaiver.getConstraintFactsJson(), not(containsString("\n")));
    assertThat(policyWaiver.getConstraintFactsJson(), not(containsString("\r")));
    assertThat(policyWaiver.getConstraintFactsJson(), not(containsString("\\n")));
    assertThat(policyWaiver.getConstraintFactsJson(), not(containsString("\\r")));
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFacts_Null() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFacts(null);

    assertThat(policyWaiver.getConstraintFactsJson(), is(nullValue()));
    assertThat(policyWaiver.getConstraintFacts(), is(nullValue()));
  }

  @Test
  public void testSetConstraintFacts_Empty() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFacts(Collections.emptyList());

    assertThat(policyWaiver.getConstraintFactsJson(), is(nullValue()));
    assertThat(policyWaiver.getConstraintFacts(), is(nullValue()));
  }

  @Test
  public void testConstructorConstraintFacts() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson(), is(constraintFactsJson));
    assertThat(policyWaiver.getConstraintFactsJson(), not(containsString("\n")));
    assertThat(policyWaiver.getConstraintFactsJson(), not(containsString("\r")));
    assertThat(policyWaiver.getConstraintFactsJson(), not(containsString("\\n")));
    assertThat(policyWaiver.getConstraintFactsJson(), not(containsString("\\r")));
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorConstraintFacts_Null() throws Exception {
    List<ConstraintFact> constraintFacts = null;

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson(), is(nullValue()));
    assertThat(policyWaiver.getConstraintFacts(), is(nullValue()));
  }

  @Test
  public void testConstructorConstraintFacts_Empty() throws Exception {
    List<ConstraintFact> constraintFacts = Collections.emptyList();

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson(), is(nullValue()));
    assertThat(policyWaiver.getConstraintFacts(), is(nullValue()));
  }

  private List<ConstraintFact> createConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
      ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
          0 /* conditionIndex */, "some summary", "some reason");
      conditionFact.setTriggerJson("some trigger");
      constraintFact.addConditionFact(conditionFact);
      constraintFacts.add(constraintFact);
    }
    return constraintFacts;
  }

  private void assertConstraintFacts(List<ConstraintFact> actual, List<ConstraintFact> expected) {
    assertThat(actual, hasSize(expected.size()));
    for (int constraintFactIndex = 0; constraintFactIndex < expected.size(); constraintFactIndex++) {
      ConstraintFact expectedConstraintFact = expected.get(constraintFactIndex);
      ConstraintFact actualConstraintFact = actual.get(constraintFactIndex);
      assertThat(actualConstraintFact.getConstraintId(), is(expectedConstraintFact.getConstraintId()));
      assertThat(actualConstraintFact.getConstraintName(), is(expectedConstraintFact.getConstraintName()));
      assertThat(actualConstraintFact.getOperatorName(), is(expectedConstraintFact.getOperatorName()));
      for (int conditionFactIndex = 0; conditionFactIndex < expectedConstraintFact.getConditionFacts()
          .size(); conditionFactIndex++) {
        ConditionFact expectedConditionFact = expectedConstraintFact.getConditionFacts().get(conditionFactIndex);
        ConditionFact actualConditionFact = actualConstraintFact.getConditionFacts().get(conditionFactIndex);
        assertThat(actualConditionFact.getConditionTypeId(), is(expectedConditionFact.getConditionTypeId()));
        assertThat(actualConditionFact.getConditionIndex(), is(expectedConditionFact.getConditionIndex()));
        assertThat(actualConditionFact.getSummary(), is(expectedConditionFact.getSummary()));
        assertThat(actualConditionFact.getReason(), is(expectedConditionFact.getReason()));
        assertThat(actualConditionFact.getTriggerJson(), is(expectedConditionFact.getTriggerJson()));
      }
    }
  }
}
