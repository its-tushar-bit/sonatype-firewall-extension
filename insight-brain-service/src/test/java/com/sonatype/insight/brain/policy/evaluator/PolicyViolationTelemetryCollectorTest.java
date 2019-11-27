/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Test;

import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.*;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationTelemetryCollectorTest
{
  private static final String TEST_APP_ID = "testApp";

  private static final String TEST_STAGE = "testStage";

  private static final PolicyEvaluation policyEvaluation = new PolicyEvaluation(TEST_APP_ID, TEST_STAGE, "scanId123");

  private static final ComponentIdentifier commonsLang3 = ComponentIdentifier.createMavenCoordinates(
      "org.apache.commons", "commons-lang3", "3.8.1");

  private static final ComponentIdentifier urllib3 = ComponentIdentifier.createPypiCoordinates(
      "urllib3", "1.25.7", null, "py");

  private static final ComponentIdentifier lodash = ComponentIdentifier.createNpmCoordinates("lodash", "4.17.15");

  @Test
  public void testAddTelemetryForFixedViolation() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = true;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(37);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, lodash);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForFixedViolation(policyViolation);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TIME_TO_REMEDIATE_POLICY_VIOLATION);
    Map<String, Object> attributes = telemetryData.get(0).getAttributes();
    assertThat(attributes.get(APPLICATION_ID)).isNotEqualTo(policyEvaluation.getApplicationId());
    assertThat(attributes.get(STAGE)).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(attributes.get(THREAT_LEVEL)).isEqualTo(threatLevel);
    assertThat(attributes.get(THREAT_CATEGORY)).isEqualTo(policyThreatCategory.getName());
    assertThat(attributes.get(IS_SCM_ENABLED)).isEqualTo(isScmEnabled);
    assertThat(attributes.get(COUNT)).isEqualTo(1);
    assertThat(attributes.get(OPEN_TIME)).isEqualTo(openTime.getTime());
    assertThat(attributes.get(FIX_TIME)).isEqualTo(evalTime.getTime());
    assertThat(attributes.get(TIME)).isEqualTo(expectedTTR);
    assertThat(attributes.get(WAIVE_TIME)).isNull();
    assertThat(attributes.get(UNWAIVE_TIME)).isNull();
  }

  @Test
  public void testAddTelemetryForUnwaivedViolation() {
    // setup : create an unwaived policy violation
    final int threatLevel = 2;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = true;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(5);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, urllib3);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForUnwaivedViolation(policyViolation);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TIME_TO_WAIVE_POLICY_VIOLATION);
    Map<String, Object> attributes = telemetryData.get(0).getAttributes();
    assertThat(attributes.get(APPLICATION_ID)).isNotEqualTo(policyEvaluation.getApplicationId());
    assertThat(attributes.get(STAGE)).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(attributes.get(THREAT_LEVEL)).isEqualTo(threatLevel);
    assertThat(attributes.get(THREAT_CATEGORY)).isEqualTo(policyThreatCategory.getName());
    assertThat(attributes.get(IS_SCM_ENABLED)).isEqualTo(isScmEnabled);
    assertThat(attributes.get(COUNT)).isEqualTo(-1);
    assertThat(attributes.get(OPEN_TIME)).isEqualTo(openTime.getTime());
    assertThat(attributes.get(WAIVE_TIME)).isEqualTo(evalTime.getTime());
    assertThat(attributes.get(TIME)).isEqualTo(expectedTTR);
    assertThat(attributes.get(FIX_TIME)).isNull();
  }

  @Test
  public void testAddTelemetryForWaivedViolation() {
    // setup : create a waived policy violation
    final int threatLevel = 7;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = false;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(45);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, commonsLang3);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForWaivedViolation(policyViolation);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TIME_TO_WAIVE_POLICY_VIOLATION);
    Map<String, Object> attributes = telemetryData.get(0).getAttributes();
    assertThat(attributes.get(APPLICATION_ID)).isNotEqualTo(policyEvaluation.getApplicationId());
    assertThat(attributes.get(STAGE)).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(attributes.get(THREAT_LEVEL)).isEqualTo(threatLevel);
    assertThat(attributes.get(THREAT_CATEGORY)).isEqualTo(policyThreatCategory.getName());
    assertThat(attributes.get(IS_SCM_ENABLED)).isEqualTo(isScmEnabled);
    assertThat(attributes.get(COUNT)).isEqualTo(1);
    assertThat(attributes.get(OPEN_TIME)).isEqualTo(openTime.getTime());
    assertThat(attributes.get(WAIVE_TIME)).isEqualTo(evalTime.getTime());
    assertThat(attributes.get(TIME)).isEqualTo(expectedTTR);
    assertThat(attributes.get(FIX_TIME)).isNull();
  }

  private List<ConstraintFact> createConstraintFactsWithInjectedCondition(String conditionTypeId) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      ConstraintFact constraintFact = new ConstraintFact("constraintId" + i, "constraintName" + i, "operatorName" + i);
      constraintFacts.add(constraintFact);
      for (int j = 0; j < 10; j++) {
        String conditionType = (i == 5 && j == 5) ? conditionTypeId : SecurityVulnerabilitySeverityConditionType.ID;
        constraintFact.addConditionFact(new ConditionFact(conditionType, j, "summary", "reason"));
      }
    }
    return constraintFacts;
  }

  private PolicyViolation createPolicyViolation(
      int threatLevel,
      PolicyThreatCategory threatCategory,
      ComponentIdentifier componentIdentifier)
  {
    return new PolicyViolation(
        policyEvaluation,
        "somePolicyId",
        "somePolicyName",
        threatLevel,
        threatCategory,
        "hash" + 1000 * Math.random(),
        componentIdentifier,
        createConstraintFactsWithInjectedCondition(LicenseConditionType.ID),
        "/etc/policyEval123.zip"
    );
  }

  private long msForHours(int hours) {
    return 1000 * 60 * 60 * hours;
  }
}
