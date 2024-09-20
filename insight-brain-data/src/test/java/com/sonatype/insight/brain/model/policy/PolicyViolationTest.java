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
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.util.HashUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyViolationTest
{
  private static final ComponentIdentifier MAVEN_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("groupId",
      "artifactId", "version");

  private PolicyEvaluation evaluation;

  @Mock
  private PolicyViolationConstraintFactsDAO constraintsDAO;

  @Before
  public void setUp() {
    PolicyViolationConstraintFactsDAOProvider.inject(constraintsDAO);
    evaluation = new PolicyEvaluation("app-id", "stage-type-id", "scan-id", "system", ScanTriggerType.CLI);
    evaluation.setTime(new Date(System.currentTimeMillis() - 12345));
  }

  @After
  public void tearDown() {
    PolicyViolationConstraintFactsDAOProvider.inject(null);
  }

  @Test
  public void testConstructor_InitializeFromEvaluation() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.getApplicationId()).isEqualTo(evaluation.getApplicationId());
    assertThat(policyViolation.getStageTypeId()).isEqualTo(evaluation.getStageTypeId());
    assertThat(policyViolation.getOpenTime()).isEqualTo(evaluation.getTime());
  }

  @Test
  public void testConstructorConstraintFacts() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);

    // Test construction of PolicyViolation with constraint facts.
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts, null);
    assertThat(policyViolation.getConstraintFactsJson()).isEqualTo(constraintFactsJson).doesNotContain("\n", "\r",
        "\\n", "\\r");
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorFilename_WithConstraintFacts() {
    String filename = "filename";
    // Violations must have constraint facts.
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts, filename);

    assertThat(policyViolation.getFilename()).isEqualTo(filename);
  }

  @Test
  public void testSetConstraintFactsJson() {
    PolicyViolation policyViolation = new PolicyViolation();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);

    String constraintsFactsHash = HashHelper.truncateHash(HashUtils.hash(constraintFactsJson, HashUtils.SHA1));
    when(constraintsDAO.getById(anyString()))
        .thenReturn(new PolicyViolationConstraintFacts(constraintsFactsHash, constraintFactsJson));

    policyViolation.setConstraintFactsId(constraintsFactsHash);
    assertThat(policyViolation.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson_Null() {
    PolicyViolation policyViolation = new PolicyViolation();

    assertThatThrownBy(() -> policyViolation.setConstraintFactsJson(null)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ConstraintFactsJson cannot be null or empty.");
  }

  @Test
  public void testSetConstraintFactsJson_Empty() {
    PolicyViolation policyViolation = new PolicyViolation();

    assertThatThrownBy(() -> policyViolation.setConstraintFactsJson(" ")).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ConstraintFactsJson cannot be null or empty.");
  }

  @Test
  public void testConstructorConstraintFacts_Null() {
    assertThatThrownBy(() -> {
      List<ConstraintFact> constraintFacts = null;
      new PolicyViolation(evaluation, "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE,
          "hash", MAVEN_IDENTIFIER, constraintFacts, "filename");
    }).isInstanceOf(IllegalArgumentException.class).hasMessage("ConstraintFacts cannot be null");
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

  @Test
  public void testSetWaiveTime() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.getWaiveTime()).isNull();

    Date now = new Date();
    policyViolation.setWaiveTime(now);
    assertThat(policyViolation.getWaiveTime()).isEqualTo(now);

    assertThatThrownBy(() -> policyViolation.setWaiveTime(null)).isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot un-waive a policy violation.");
  }

  @Test
  public void testSetFixTime() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.getFixTime()).isNull();

    Date now = new Date();
    policyViolation.setFixTime(now);
    assertThat(policyViolation.getFixTime()).isEqualTo(now);

    assertThatThrownBy(() -> policyViolation.setFixTime(null)).isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot un-fix a policy violation.");
  }

  @Test
  public void testIsActive_Fixed() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.isActive()).isTrue();

    policyViolation.setFixTime(new Date());
    assertThat(policyViolation.isActive()).isFalse();
  }

  @Test
  public void testIsActive_Waived() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.isActive()).isTrue();

    policyViolation.setWaiveTime(new Date());
    assertThat(policyViolation.isActive()).isFalse();
  }

  @Test
  public void testIsActive_LegacyViolation() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");
    assertThat(policyViolation.isActive()).isTrue();

    policyViolation.setLegacyViolationTime(new Date());
    assertThat(policyViolation.isActive()).isFalse();
  }

  @Test
  public void testGetFixOrWaiveTime_BothNull() {
    PolicyViolation violation = new PolicyViolation();

    assertThat(violation.getFixOrWaiveTime()).isNull();
  }

  @Test
  public void testGetFixOrWaiveTime_FixTimeNull() {
    Date date = new Date();
    PolicyViolation violation = new PolicyViolation();
    violation.setWaiveTime(date);

    assertThat(violation.getFixOrWaiveTime()).isEqualTo(date);
  }

  @Test
  public void testGetFixOrWaiveTime_WaiveTimeNull() {
    Date date = new Date();
    PolicyViolation violation = new PolicyViolation();
    violation.setFixTime(date);

    assertThat(violation.getFixOrWaiveTime()).isEqualTo(date);
  }

  @Test
  public void testGetFixOrWaiveTime_WaiveTimeGreater() {
    Date fixTime = new Date();
    Date waiveTime = new Date(fixTime.getTime() + 1);
    PolicyViolation violation = new PolicyViolation();
    violation.setFixTime(fixTime);
    violation.setWaiveTime(waiveTime);

    assertThat(violation.getFixOrWaiveTime()).isEqualTo(fixTime);
  }

  @Test
  public void testGetFixOrWaiveTime_FixTimeGreater() {
    Date fixTime = new Date();
    Date waiveTime = new Date(fixTime.getTime() - 1);
    PolicyViolation violation = new PolicyViolation();
    violation.setFixTime(fixTime);
    violation.setWaiveTime(waiveTime);

    assertThat(violation.getFixOrWaiveTime()).isEqualTo(waiveTime);
  }

  @Test
  public void testGetOwnerId() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");

    assertThat(policyViolation.getOwnerId()).isEqualTo(policyViolation.getApplicationId());
  }

  @Test
  public void testSetReachabilityStatus() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");

    policyViolation.setReachabilityStatus(ReachabilityStatus.REACHABLE);
    assertThat(policyViolation.getReachabilityStatus()).isEqualTo(ReachabilityStatus.REACHABLE);

    policyViolation.setReachabilityStatus(ReachabilityStatus.NON_REACHABLE);
    assertThat(policyViolation.getReachabilityStatus()).isEqualTo(ReachabilityStatus.NON_REACHABLE);
  }

  @Test
  public void testGetNullReachabilityStatus() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");

    assertThat(policyViolation.getReachabilityStatus()).isNull();
  }

  @Test
  public void testGetConstraintsJson_whereLegacy() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");

    assertThat(policyViolation.getConstraintFactsId()).isNotBlank();
    assertThat(policyViolation.getConstraintFactsJson()).isNotBlank();
    assertThat(policyViolation.getConstraintFacts()).isNotEmpty();

    // Constraints is set so should not have been loaded from the database
    verify(constraintsDAO, never()).getById(anyString());
  }

  @Test
  public void testGetConstraintFactsWithoutLoading() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");

    policyViolation.getConstraintFactsJson();

    verify(constraintsDAO, never()).getById(anyString());
  }

  @Test
  public void testGetConstraintFacts_lazyLoadsConstraintsFromDatabase() {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, "policyId", "policyName", 5,
        PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, createConstraintFacts(1), "filename");

    String id = "testGetConstraintFacts_lazyLoadsConstraintsFromDatabase";
    String json = "json";

    policyViolation.clearConstraintFactsJson();
    policyViolation.setConstraintFactsId(id);

    try {
      PolicyViolationConstraintFactsDAOProvider.inject(constraintsDAO);
      when(constraintsDAO.getById(id)).thenReturn(new PolicyViolationConstraintFacts(id, json));

      String jsonResult = policyViolation.getConstraintFactsJson();
      verify(constraintsDAO).getById(id);
      assertThat(jsonResult).isEqualTo(json);
    }
    finally {
      PolicyViolationConstraintFactsDAOProvider.inject(null);
    }
  }
}
