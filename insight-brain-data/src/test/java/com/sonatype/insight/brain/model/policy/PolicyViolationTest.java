/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class PolicyViolationTest
{
  private static final ComponentIdentifier MAVEN_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("groupId",
      "artifactId", "version");

  private PolicyEvaluation evaluation;

  @Before
  public void setUp() {
    evaluation = new PolicyEvaluation("app-id", "stage-type-id", "scan-id");
    evaluation.setTime(new Date(System.currentTimeMillis() - 12345));
  }

  @Test
  public void testConstructor_InitializeFromEvaluation() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.getApplicationId(), is(evaluation.getApplicationId()));
    assertThat(policyViolation.getStageTypeId(), is(evaluation.getStageTypeId()));
    assertThat(policyViolation.getOpenTime(), is(evaluation.getTime()));
  }

  @Test
  public void testConstructorConstraintFacts() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);

    // Test construction of PolicyViolation with constraint facts.
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts, null);
    assertThat(policyViolation.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorFilename_WithConstraintFacts() throws Exception {
    String filename = "filename";
    // Violations must have constraint facts.
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts, filename);

    assertThat(policyViolation.getFilename(), is(filename));
  }

  @Test
  public void testConstructorConstraintFactsJson() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFactsJson, null);
    assertThat(policyViolation.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorFilename_WithConstraintFactsJson() throws Exception {
    String filename = "filename";
    // Violations must have constraint facts.
    String constraintFactsJson = JsonUtils.format(createConstraintFacts(1));

    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFactsJson, filename);

    assertThat(policyViolation.getFilename(), is(filename));
  }

  @Test
  public void testSetConstraintFactsJson() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);
    policyViolation.setConstraintFactsJson(constraintFactsJson);
    assertThat(policyViolation.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson_Null() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation();

    try {
      policyViolation.setConstraintFactsJson(null);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage(), is("ConstraintFactsJson cannot be null or empty."));
    }
  }

  @Test
  public void testSetConstraintFactsJson_Empty() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation();

    try {
      policyViolation.setConstraintFactsJson(" ");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage(), is("ConstraintFactsJson cannot be null or empty."));
    }
  }

  @Test
  public void testConstructorConstraintFactsJson_Null() throws Exception {
    try {
      String constraintFactsJson = null;
      new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE,
          "hash", MAVEN_IDENTIFIER, constraintFactsJson, "filename");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage(), is("ConstraintFactsJson cannot be null or empty."));
    }
  }

  @Test
  public void testConstructorConstraintFactsJson_Empty() throws Exception {
    try {
      String constraintFactsJson = " ";
      new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE,
          "hash", MAVEN_IDENTIFIER, constraintFactsJson, "filename");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage(), is("ConstraintFactsJson cannot be null or empty."));
    }
  }

  @Test
  public void testConstructorConstraintFacts_Null() throws Exception {
    try {
      List<ConstraintFact> constraintFacts = null;
      new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE,
          "hash", MAVEN_IDENTIFIER, constraintFacts, "filename");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage(), is("ConstraintFacts cannot be null or empty."));
    }
  }

  @Test
  public void testConstructorConstraintFacts_Empty() throws Exception {
    try {
      List<ConstraintFact> constraintFacts = new ArrayList<>();
      new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE,
          "hash", MAVEN_IDENTIFIER, constraintFacts, "filename");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage(), is("ConstraintFacts cannot be null or empty."));
    }
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

  @Test
  public void testSetWaiveTime() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.getWaiveTime(), is(nullValue()));

    Date now = new Date();
    policyViolation.setWaiveTime(now);
    assertThat(policyViolation.getWaiveTime(), is(now));

    try {
      policyViolation.setWaiveTime(null);
      fail("Expected exception");
    }
    catch (IllegalStateException expected) {
      assertThat(expected.getMessage(), is("Cannot un-waive a policy violation."));
    }
  }

  @Test
  public void testSetFixTime() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.getFixTime(), is(nullValue()));

    Date now = new Date();
    policyViolation.setFixTime(now);
    assertThat(policyViolation.getFixTime(), is(now));

    try {
      policyViolation.setFixTime(null);
      fail("Expected exception");
    }
    catch (IllegalStateException expected) {
      assertThat(expected.getMessage(), is("Cannot un-fix a policy violation."));
    }
  }

  @Test
  public void testIsActive_Fixed() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.isActive(), is(true));

    policyViolation.setFixTime(new Date());
    assertThat(policyViolation.isActive(), is(false));
  }

  @Test
  public void testIsActive_Waived() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.isActive(), is(true));

    policyViolation.setWaiveTime(new Date());
    assertThat(policyViolation.isActive(), is(false));
  }

  @Test
  public void testIsActive_Grandfathered() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.isActive(), is(true));

    policyViolation.setGrandfatherTime(new Date());
    assertThat(policyViolation.isActive(), is(false));
  }
}
