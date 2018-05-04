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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class RepositoryPolicyViolationTest
{
  private static final ComponentIdentifier MAVEN_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("groupId",
      "artifactId", "version");

  @Test
  public void testConstructorConstraintFacts() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);

    // Test construction of RepositoryPolicyViolation with constraint facts.
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation("repositoryId", "path", new Date(),
        "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER,
        constraintFacts);
    assertThat(policyViolation.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorConstraintFactsJson() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);

    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation("repositoryId", "path", new Date(),
        "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER,
        constraintFactsJson);
    assertThat(policyViolation.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson() throws Exception {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);
    policyViolation.setConstraintFactsJson(constraintFactsJson);
    assertThat(policyViolation.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson_Null() throws Exception {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation();

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
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation();

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
      new RepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /* threatLevel */,
          PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFactsJson);
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
      new RepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /* threatLevel */,
          PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFactsJson);
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
      new RepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /* threatLevel */,
          PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts);
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
      new RepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /* threatLevel */,
          PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts);
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
  public void testSetWaived() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation("repositoryId", "path", new Date(),
        "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER,
        constraintFacts);
    assertThat(policyViolation.isWaived(), is(false));

    policyViolation.setWaived(true);
    assertThat(policyViolation.isWaived(), is(true));

    try {
      policyViolation.setWaived(false);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException expected) {
      assertThat(expected.getMessage(), is("Cannot un-waive a policy violation."));
    }
  }
}
