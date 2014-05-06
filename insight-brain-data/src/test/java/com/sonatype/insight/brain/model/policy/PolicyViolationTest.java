/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PolicyViolationTest
{
  @Test
  public void testConstructorConstraintFacts() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);

    // Test construction of PolicyViolation with constraint facts.
    PolicyViolation policyViolation = new PolicyViolation("policyEvaluationId", "policyId", "policyName",
        5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version", constraintFacts,
        null);
    assertThat(policyViolation.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorPathnames() throws Exception {
    String pathnamesString = createPathnamesString(2);
    List<String> pathnames = Arrays.asList(pathnamesString.split(PolicyViolation.PATHNAMES_DELIMITER_REGEX));
    // Violations must have constraint facts.
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);

    PolicyViolation policyViolation = new PolicyViolation("policyEvaluationId", "policyId", "policyName",
        5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version", constraintFacts,
        pathnames);

    assertThat(policyViolation.getPathnames(), is(pathnames));
    assertThat(policyViolation.getPathnamesString(), is(pathnamesString));
  }

  @Test
  public void testConstructorConstraintFactsJson() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.format(constraintFacts);

    PolicyViolation policyViolation = new PolicyViolation("policyEvaluationId", "policyId", "policyName",
        5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version",
        constraintFactsJson, null);
    assertThat(policyViolation.getConstraintFactsJson(), is(constraintFactsJson));
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorPathnamesString() throws Exception {
    String pathnamesString = createPathnamesString(2);
    List<String> pathnames = Arrays.asList(pathnamesString.split(PolicyViolation.PATHNAMES_DELIMITER_REGEX));
    // Violations must have constraint facts.
    String constraintFactsJson = JsonUtils.format(createConstraintFacts(1));

    PolicyViolation policyViolation = new PolicyViolation("policyEvaluationId", "policyId", "policyName",
        5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version",
        constraintFactsJson, pathnamesString);

    assertThat(policyViolation.getPathnames(), is(pathnames));
    assertThat(policyViolation.getPathnamesString(), is(pathnamesString));
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
    }
  }

  @Test
  public void testConstructorConstraintFactsJson_Null() throws Exception {
    try {
      String constraintFactsJson = null;
      new PolicyViolation("policyEvaluationId", "policyId", "policyName", 5 /* threatLevel */,
          PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version", constraintFactsJson,
          createPathnamesString(1));
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testConstructorConstraintFactsJson_Empty() throws Exception {
    try {
      String constraintFactsJson = " ";
      new PolicyViolation("policyEvaluationId", "policyId", "policyName", 5 /* threatLevel */,
          PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version", constraintFactsJson,
          createPathnamesString(1));
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testConstructorConstraintFacts_Null() throws Exception {
    try {
      List<String> pathnames = Arrays.asList(createPathnamesString(1));
      List<ConstraintFact> constraintFacts = null;
      new PolicyViolation("policyEvaluationId", "policyId", "policyName", 5 /* threatLevel */,
          PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version", constraintFacts, pathnames);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testConstructorConstraintFacts_Empty() throws Exception {
    try {
      List<String> pathnames = Arrays.asList(createPathnamesString(1));
      List<ConstraintFact> constraintFacts = new ArrayList<>();
      new PolicyViolation("policyEvaluationId", "policyId", "policyName", 5 /* threatLevel */,
          PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version", constraintFacts, pathnames);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testConstructorPathnamesString_Null() throws Exception {
    String pathnamesString = null;
    PolicyViolation policyViolation = new PolicyViolation("policyEvaluationId", "policyId", "policyName",
        5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version",
        JsonUtils.format(createConstraintFacts(2)), pathnamesString);

    assertNull(policyViolation.getPathnames());
  }

  @Test
  public void testConstructorPathnamesString_Empty() throws Exception {
    String pathnamesString = " ";
    PolicyViolation policyViolation = new PolicyViolation("policyEvaluationId", "policyId", "policyName",
        5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version",
        JsonUtils.format(createConstraintFacts(2)), pathnamesString);

    assertNull(policyViolation.getPathnames());
    assertNull(policyViolation.getPathnamesString());
  }

  @Test
  public void testConstructorPathnames_Null() throws Exception {
    List<String> pathnames = null;
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    PolicyViolation policyViolation = new PolicyViolation("policyEvaluationId", "policyId", "policyName",
        5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version", constraintFacts,
        pathnames);

    assertNull(policyViolation.getPathnames());
    assertNull(policyViolation.getPathnamesString());
  }

  @Test
  public void testConstructorPathnames_Empty() throws Exception {
    List<String> pathnames = new ArrayList<>();
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    PolicyViolation policyViolation = new PolicyViolation("policyEvaluationId", "policyId", "policyName",
        5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", "groupId", "artifactId", "version", constraintFacts,
        pathnames);

    assertNull(policyViolation.getPathnames());
    assertNull(policyViolation.getPathnamesString());
  }

  private List<ConstraintFact> createConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      constraintFacts.add(new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and"));
    }
    return constraintFacts;
  }

  private String createPathnamesString(int count) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (builder.length() > 0) {
        builder.append(PolicyViolation.PATHNAMES_DELIMITER_CHAR);
      }
      builder.append("/test/path/" + i + ".suffix");
    }
    return builder.toString();
  }

  private void assertConstraintFacts(List<ConstraintFact> actual, List<ConstraintFact> expected) {
    assertThat(actual, hasSize(expected.size()));
    for (int i = 0; i < expected.size(); i++) {
      ConstraintFact expectedConstraintFact = expected.get(i);
      ConstraintFact actualConstraintFact = actual.get(i);
      assertThat(actualConstraintFact.getConstraintId(), is(expectedConstraintFact.getConstraintId()));
      assertThat(actualConstraintFact.getConstraintName(), is(expectedConstraintFact.getConstraintName()));
      assertThat(actualConstraintFact.getOperatorName(), is(expectedConstraintFact.getOperatorName()));
    }
  }
}
