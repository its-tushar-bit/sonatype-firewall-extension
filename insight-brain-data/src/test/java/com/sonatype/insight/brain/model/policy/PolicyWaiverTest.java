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
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyWaiverTest
{
  @Test
  public void testLongHashTruncatedWhenObjectCreated() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(20);
    PolicyWaiver policyWaiver = new PolicyWaiver(longHash, null /* policyId */, null /* ownerId */, null /* comment */);
    assertThat(policyWaiver.getHash()).isEqualTo(longHash.substring(0, HashHelper.MAX_LENGTH));
  }

  @Test
  public void testLongHashTruncatedWhenHashSet() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(20);
    PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setHash(longHash);
    assertThat(policyWaiver.getHash()).isEqualTo(longHash.substring(0, HashHelper.MAX_LENGTH));
  }

  @Test
  public void testSetConstraintFactsJson() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    policyWaiver.setConstraintFactsJson(constraintFactsJson);
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson_Null() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFactsJson(null);

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFactsJson_Empty() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFactsJson(" ");

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFacts() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    policyWaiver.setConstraintFacts(constraintFacts);
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(constraintFactsJson).doesNotContain("\n", "\r", "\\n",
        "\\r");
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFacts_Null() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFacts(null);

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFacts_Empty() throws Exception {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFacts(Collections.emptyList());

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testConstructorConstraintFacts() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(constraintFactsJson).doesNotContain("\n", "\r", "\\n",
        "\\r");
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorConstraintFacts_Null() throws Exception {
    List<ConstraintFact> constraintFacts = null;

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testConstructorConstraintFacts_Empty() throws Exception {
    List<ConstraintFact> constraintFacts = Collections.emptyList();

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
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
    assertThat(actual).hasSameSizeAs(expected);
    for (int constraintFactIndex = 0; constraintFactIndex < expected.size(); constraintFactIndex++) {
      ConstraintFact expectedConstraintFact = expected.get(constraintFactIndex);
      ConstraintFact actualConstraintFact = actual.get(constraintFactIndex);
      assertThat(actualConstraintFact.getConstraintId()).isEqualTo(expectedConstraintFact.getConstraintId());
      assertThat(actualConstraintFact.getConstraintName()).isEqualTo(expectedConstraintFact.getConstraintName());
      assertThat(actualConstraintFact.getOperatorName()).isEqualTo(expectedConstraintFact.getOperatorName());
      for (int conditionFactIndex = 0; conditionFactIndex < expectedConstraintFact.getConditionFacts()
          .size(); conditionFactIndex++) {
        ConditionFact expectedConditionFact = expectedConstraintFact.getConditionFacts().get(conditionFactIndex);
        ConditionFact actualConditionFact = actualConstraintFact.getConditionFacts().get(conditionFactIndex);
        assertThat(actualConditionFact.getConditionTypeId()).isEqualTo(expectedConditionFact.getConditionTypeId());
        assertThat(actualConditionFact.getConditionIndex()).isEqualTo(expectedConditionFact.getConditionIndex());
        assertThat(actualConditionFact.getSummary()).isEqualTo(expectedConditionFact.getSummary());
        assertThat(actualConditionFact.getReason()).isEqualTo(expectedConditionFact.getReason());
        assertThat(actualConditionFact.getTriggerJson()).isEqualTo(expectedConditionFact.getTriggerJson());
      }
    }
  }
}
