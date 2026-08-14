/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProxyRepositoryPolicyViolationTest
{
  private static final ComponentIdentifier MAVEN_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("groupId",
      "artifactId", "version");

  @Test
  public void testConstructor_ConstraintFacts() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);

    // Test construction of ProxyRepositoryPolicyViolation with constraint facts.
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(),
            "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER,
            constraintFacts);
    assertThat(policyViolation.getConstraintFactsJson()).isEqualTo(constraintFactsJson)
        .doesNotContain("\n", "\r",
            "\\n", "\\r");
    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFacts_Null() {
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation();

    assertThatThrownBy(() -> policyViolation.setConstraintFacts(null)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ConstraintFacts cannot be null or empty.");
  }

  @Test
  public void testSetConstraintFacts_Empty() {
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation();

    assertThatThrownBy(() -> policyViolation.setConstraintFacts(Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ConstraintFacts cannot be null or empty.");
  }

  @Test
  public void testConstructor_ConstraintFacts_Null() {
    assertThatThrownBy(() -> {
      List<ConstraintFact> constraintFacts = null;
      new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /*
                                                                                                          * threatLevel
                                                                                                          */,
          PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts);
    }).isInstanceOf(IllegalArgumentException.class).hasMessage("ConstraintFacts cannot be null or empty.");
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
          .size(); conditionFactIndex++)
      {
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
  public void testSetWaived() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(),
            "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER,
            constraintFacts);
    assertThat(policyViolation.isWaived()).isFalse();

    policyViolation.setWaived(true);
    assertThat(policyViolation.isWaived()).isTrue();

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> policyViolation.setWaived(false))
        .withMessage("Cannot un-waive a repository policy violation.");
  }

  @Test
  public void testSetPolicyWaiverDetails() {
    Date waiveTime = new Date();
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(),
            "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER,
            constraintFacts);
    assertThat(policyViolation.isWaived()).isFalse();

    policyViolation.setWaived(true);
    assertThat(policyViolation.isWaived()).isTrue();

    policyViolation.setPolicyWaiverId("policyWaiverId");
    assertThat(policyViolation.getPolicyWaiverId()).isEqualTo("policyWaiverId");

    policyViolation.setPolicyWaiverComment("waive comment");
    assertThat(policyViolation.getPolicyWaiverComment()).isEqualTo("waive comment");

    policyViolation.setWaiveTime(waiveTime);
    assertThat(policyViolation.getWaiveTime()).isEqualTo(waiveTime);
  }

  @Test
  public void testSetWaiveTime_IllegalStateException() {
    Date waiveTime = new Date();
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(),
            "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER,
            constraintFacts);
    policyViolation.setWaiveTime(waiveTime);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> policyViolation.setWaiveTime(null))
        .withMessage("Cannot un-waive a repository policy violation.");
  }

  @Test
  public void testGetOpenTime() {
    Date time = new Date();
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", time, "policyId", "policyName", 5 /* threatLevel */,
            PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts);

    assertThat(policyViolation.getTime()).isEqualTo(time);
    assertThat(policyViolation.getOpenTime()).isEqualTo(time);
  }

  @Test
  public void testGetStageTypeId() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /*
                                                                                                            * threatLevel
                                                                                                            */,
            PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts);

    assertThat(policyViolation.getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
  }

  @Test
  public void testGetOwnerId() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /*
                                                                                                            * threatLevel
                                                                                                            */,
            PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts);

    assertThat(policyViolation.getOwnerId()).isEqualTo(policyViolation.getRepositoryId());
  }

  @Test
  public void testGetConstraintFacts() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /*
                                                                                                            * threatLevel
                                                                                                            */,
            PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts);

    assertConstraintFacts(policyViolation.getConstraintFacts(), constraintFacts);

    policyViolation.clearConstraintFacts();

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> policyViolation.getConstraintFacts())
        .withMessageContaining("Constraint facts are not loaded yet for policyViolationId=");
  }

  @Test
  public void testGetConstraintFactsJson() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    ProxyRepositoryPolicyViolation policyViolation =
        new ProxyRepositoryPolicyViolation("repositoryId", "path", new Date(), "policyId", "policyName", 5 /*
                                                                                                            * threatLevel
                                                                                                            */,
            PolicyThreatCategory.LICENSE, "hash", MAVEN_IDENTIFIER, constraintFacts);

    assertThat(policyViolation.getConstraintFactsJson()).isEqualTo(constraintFactsJson);

    policyViolation.clearConstraintFacts();

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> policyViolation.getConstraintFactsJson())
        .withMessageContaining("Constraint facts are not loaded yet for policyViolationId=");
  }
}
