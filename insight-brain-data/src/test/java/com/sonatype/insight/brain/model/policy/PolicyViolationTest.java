/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.base.Joiner;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PolicyViolationTest
{
  private PolicyEvaluation evaluation = new PolicyEvaluation();

  private static final ComponentIdentifier MAVEN_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("groupId",
      "artifactId", "version");

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

  @Test
  public void testConstructorFilename_Null() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, JsonUtils.format(createConstraintFacts(2)),
        null);

    assertNull(policyViolation.getFilename());
  }

  @Test
  public void testConstructorFilename_Blank() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, JsonUtils.format(createConstraintFacts(2)),
        " ");

    assertNull(policyViolation.getFilename());
  }

  private List<ConstraintFact> createConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      constraintFacts.add(new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and"));
    }
    return constraintFacts;
  }

  private String createNotificationsString(int count) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (builder.length() > 0) {
        builder.append(PolicyViolation.NOTIFICATIONS_DELIMITER_CHAR);
      }
      builder.append("DonaldDuck").append(i).append("@example.com");
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

  @Test
  public void testSetNotifications() throws Exception {
    String notificationsString = createNotificationsString(2);
    List<String> notifications = Arrays
        .asList(notificationsString.split(PolicyViolation.NOTIFICATIONS_DELIMITER_REGEX));
    // Violations must have constraint facts.
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts, null /* filename */);
    policyViolation.setNotifications(notifications);

    assertThat(policyViolation.getNotifications(), is(notifications));
    assertThat(policyViolation.getNotificationsString(), is(notificationsString));
  }

  @Test
  public void testSetNotifications_Empty() throws Exception {
    List<String> notifications = new ArrayList<>();
    // Violations must have constraint facts.
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts, null /* filename */);
    policyViolation.setNotifications(notifications);

    assertThat(policyViolation.getNotifications(), hasSize(0));
    assertThat(policyViolation.getNotificationsString(), is(nullValue()));
  }

  @Test
  public void testSetNotifications_Null() throws Exception {
    List<String> notifications = null;
    // Violations must have constraint facts.
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts, null /* filename */);
    policyViolation.setNotifications(notifications);

    assertThat(policyViolation.getNotifications(), hasSize(0));
    assertThat(policyViolation.getNotificationsString(), is(nullValue()));
  }

  @Test
  public void testSetWaived() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts, null /* filename */);
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

  @Test
  public void testGetFilename_ReturnsNullGivenNullFilename() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPathnames((String) null);
    assertThat(policyViolation.getFilename(), is(nullValue()));
  }

  @Test
  public void testGetFilename_ReturnsNullGivenEmptyFilename() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPathnames("");
    assertThat(policyViolation.getFilename(), is(nullValue()));
  }

  @Test
  public void testGetFilename_ReturnsFilenameGivenFilename() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPathnames("foo");
    assertThat(policyViolation.getFilename(), is(equalTo("foo")));
  }

  @Test
  public void testGetFilename_ReturnsNullGivenNullPathnames() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPathnames((List<String>) null);
    assertThat(policyViolation.getFilename(), is(nullValue()));
  }

  @Test
  public void testGetFilename_ReturnsNullGivenEmptyPathnames() throws Exception {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPathnames(Collections.<String>emptyList());
    assertThat(policyViolation.getFilename(), is(nullValue()));
  }

  @Test
  public void testGetFilename_ReturnsCorrectFirstPathFilename() throws Exception {
    assertFilename("bar", "", "bar");
    assertFilename("foo", "foo", "bar");
    assertFilename("foo.jar", "foo.jar", "bar");
    assertFilename("foo.jar", "some/path/foo.jar", "bar");
    assertFilename("foo.jar", "/some/path/foo.jar", "bar");
    assertFilename("foo with spaces.jar", "foo with spaces.jar", "bar");
    assertFilename("junit:junit:jar:4.9", "dependency:/com.sonatype.test:clm:war:0.1/junit:junit:jar:4.9");
    String sc = "!£$%^&*()-_+=/`.<>?@'~#{}[];:<>,|\\";
    assertFilename("g2:a2:jar:0.2", String.format("%sdependency%s:/g1:%sa1%s:%s0.1/g2:a2:jar:0.2", sc, sc, sc, sc, sc));
  }

  private void assertFilename(String expectedFilename, String... pathnames) {
    assertFilename(expectedFilename, Arrays.asList(pathnames));
  }

  private void assertFilename(String expectedFilename, List<String> pathnames) {
    PolicyViolation policyViolation = new PolicyViolation();
    // Check filename is correct given unmigrated pathnames String
    policyViolation.setPathnames(Joiner.on(PolicyViolation.PATHNAMES_DELIMITER_CHAR).skipNulls().join(pathnames));
    assertThat(policyViolation.getFilename(), is(equalTo(expectedFilename)));
    // Check filename is correct when set via pathnames list
    policyViolation.setPathnames(pathnames);
    assertThat(policyViolation.getFilename(), is(equalTo(expectedFilename)));
  }
}
