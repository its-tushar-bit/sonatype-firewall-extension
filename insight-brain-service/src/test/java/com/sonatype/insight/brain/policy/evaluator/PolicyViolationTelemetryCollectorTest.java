/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.*;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_CHANGE_VERSION_POLICY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_LEGACY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationTelemetryCollectorTest
    extends AbstractComponentTest
{
  private static final String TEST_APP_ID = "testApp";

  private static final String TEST_STAGE = "testStage";

  private static final String REAL_APPLICATION_ID = "real_application_id";

  private static final PolicyEvaluation policyEvaluation =
      new PolicyEvaluation(TEST_APP_ID, TEST_STAGE, "scanId123", CurrentUser.SYSTEM, ScanTriggerType.CLI);

  private static final ComponentIdentifier commonsLang3 = ComponentIdentifier.createMavenCoordinates(
      "org.apache.commons", "commons-lang3", "3.8.1");

  private static final ComponentIdentifier urllib3 = ComponentIdentifier.createPypiCoordinates(
      "urllib3", "1.25.7", null, "py");

  private static final ComponentIdentifier lodashv3 = ComponentIdentifier.createNpmCoordinates("lodash", "3.0.4");

  private static final ComponentIdentifier lodashv4 = ComponentIdentifier.createNpmCoordinates("lodash", "4.17.15");

  private static final ComponentIdentifier lodashv5 = ComponentIdentifier.createNpmCoordinates("lodash", "5.1.0");

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private TelemetryUtils telemetryUtils;

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = true;
    final boolean isDirectDependency = false;
    final boolean isInnerSource = false;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(37);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    Component component = new Component();
    component.setComponentIdentifier(lodashv5);
    component.setInnerSourceData(null);
    component.setDirectDependency(isDirectDependency);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, lodashv4);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForFixedViolation(policyViolation, Collections.singletonList(component));
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(2);
    assertTelemetryAttributes(TIME_TO_REMEDIATE_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, isDirectDependency, isInnerSource, null,
        telemetryData.get(0));
    assertTelemetryAttributes(TIME_TO_CHANGE_VERSION_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, isDirectDependency, isInnerSource,
        "upgrade", telemetryData.get(1));
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByDowngrade() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = true;
    final boolean isDirectDependency = false;
    final boolean isInnerSource = false;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(37);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    Component component = new Component();
    component.setComponentIdentifier(lodashv4);
    component.setInnerSourceData(null);
    component.setDirectDependency(isDirectDependency);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, lodashv5);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForFixedViolation(policyViolation, Collections.singletonList(component));
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(2);
    assertTelemetryAttributes(TIME_TO_REMEDIATE_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, isDirectDependency, isInnerSource, null,
        telemetryData.get(0));
    assertTelemetryAttributes(TIME_TO_CHANGE_VERSION_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, isDirectDependency, isInnerSource,
        "downgrade", telemetryData.get(1));
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByRemoval() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = true;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(37);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, lodashv5);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForFixedViolation(policyViolation, Collections.emptyList());
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertTelemetryAttributes(TIME_TO_REMEDIATE_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, null, null, null, telemetryData.get(0));
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedWithoutVersionChange() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = true;
    final boolean isDirectDependency = false;
    final boolean isInnerSource = false;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(37);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    Component component = new Component();
    component.setComponentIdentifier(lodashv5);
    component.setInnerSourceData(null);
    component.setDirectDependency(isDirectDependency);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, lodashv5);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForFixedViolation(policyViolation, Collections.singletonList(component));
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertTelemetryAttributes(TIME_TO_REMEDIATE_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, isDirectDependency, isInnerSource, null,
        telemetryData.get(0));
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUndetermined() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = true;
    final boolean isInnerSource = true;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(37);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    Component component1 = new Component();
    component1.setComponentIdentifier(lodashv5);
    component1.setInnerSourceData(Collections.singleton(new InnerSourceData()));

    Component component2 = new Component();
    component2.setComponentIdentifier(lodashv3);
    component2.setInnerSourceData(Collections.singleton(new InnerSourceData()));

    List<Component> components = new ArrayList<>();
    components.add(component1);
    components.add(component2);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, lodashv4);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForFixedViolation(policyViolation, components);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertTelemetryAttributes(TIME_TO_REMEDIATE_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, null, isInnerSource, null,
        telemetryData.get(0));
  }

  @Test
  public void testAddTelemetryForUnwaivedViolation() {
    // setup : create an unwaived policy violation
    final int threatLevel = 2;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = true;
    final boolean isDirectDependency = true;
    final boolean isInnerSource = true;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(5);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    Component component = new Component();
    component.setComponentIdentifier(urllib3);
    component.setInnerSourceData(Collections.singleton(new InnerSourceData()));
    component.setDirectDependency(isDirectDependency);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, urllib3);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForUnwaivedViolation(policyViolation, component);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertTelemetryAttributes(TIME_TO_WAIVE_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), evalTime.getTime(), evalTime.getTime(), null, -1, isDirectDependency,
        isInnerSource, null, telemetryData.get(0));
  }

  @Test
  public void testAddTelemetryForWaivedViolation() {
    // setup : create a waived policy violation
    final int threatLevel = 7;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = false;
    final boolean isDirectDependency = true;
    final boolean isInnerSource = false;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(45);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    final String waiverExpirationInDays = "never";

    Component component = new Component();
    component.setComponentIdentifier(commonsLang3);
    component.setInnerSourceData(null);
    component.setDirectDependency(isDirectDependency);

    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, commonsLang3);
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyViolation.getApplicationId());
    policyWaiver.setCreateTime(new Date());
    policyViolation.setOpenTime(openTime);
    policyViolation.setPolicyWaiverId(policyWaiver.getId());
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForWaivedViolation(policyViolation, component);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    Map<String, Object> attributes = telemetryData.get(0).getAttributes();
    assertTelemetryAttributes(TIME_TO_WAIVE_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), evalTime.getTime(), evalTime.getTime(), null, 1, isDirectDependency,
        isInnerSource, null, telemetryData.get(0));
    assertThat(attributes.get(WAIVER_EXPIRATION)).isEqualTo(waiverExpirationInDays);
  }

  @Test
  public void testAddTelemetryForWaivedViolation_NoDependencyInfo() {
    // setup : create a waived policy violation
    Component component = new Component();
    component.setComponentIdentifier(commonsLang3);

    PolicyViolation policyViolation = createPolicyViolation(7, PolicyThreatCategory.SECURITY, commonsLang3);
    policyViolation.setOpenTime(new Date());
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyViolation.getApplicationId());
    policyViolation.setPolicyWaiverId(policyWaiver.getId());
    PolicyViolationTelemetryCollector telemetryCollector =
        new PolicyViolationTelemetryCollector(policyWaiverDAO, telemetryUtils, false);

    // when
    telemetryCollector.addTelemetryForWaivedViolation(policyViolation, component);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TIME_TO_WAIVE_POLICY_VIOLATION);
    Map<String, Object> attributes = telemetryData.get(0).getAttributes();
    assertThat(attributes.get(DIRECT_DEPENDENCY)).isNull();
    assertThat(attributes.get(INNERSOURCE_DEPENDENCY)).isEqualTo(false);
  }

  @Test
  public void testAddTelemetryForLegacyViolation() {
    // setup : create a legacy policy violation
    final int threatLevel = 7;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = false;
    final boolean isDirectDependency = true;
    final boolean isInnerSource = false;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(45);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);

    Component component = new Component();
    component.setComponentIdentifier(commonsLang3);
    component.setInnerSourceData(null);
    component.setDirectDependency(isDirectDependency);

    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, commonsLang3);
    policyViolation.setLegacyViolationTime(new Date());
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForLegacyViolation(policyViolation, component);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TIME_TO_LEGACY_VIOLATION);
    Map<String, Object> attributes = telemetryData.get(0).getAttributes();
    assertTelemetryAttributes(TIME_TO_LEGACY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, null, 1, isDirectDependency, isInnerSource, null,
        telemetryData.get(0));
    assertThat(attributes.get(LEGACY_VIOLATION_TIME)).isEqualTo(evalTime.getTime());
  }

  @Test
  public void testAddTelemetryForConditionTypeViolation() {
    // setup : create a condition type policy violation
    final int threatLevel = 7;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final boolean isScmEnabled = false;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(45);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, commonsLang3);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    final TelemetryPurpose telemetryPurpose = TelemetryPurpose.CONDITION_TYPE_VIOLATION;
    final String validConditionType = HygieneRatingConditionType.ID;

    // when
    telemetryCollector.addTelemetryForConditionTypeViolation(policyViolation, validConditionType);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(telemetryPurpose);
    Map<String, Object> attributes = telemetryData.get(0).getAttributes();
    assertThat(attributes.get(APPLICATION_ID)).isNotEqualTo(policyEvaluation.getApplicationId());
    assertThat(attributes.get(REAL_APPLICATION_ID)).isEqualTo(policyViolation.getApplicationId());
    assertThat(attributes.get(STAGE)).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(attributes.get(THREAT_LEVEL)).isEqualTo(threatLevel);
    assertThat(attributes.get(THREAT_CATEGORY)).isEqualTo(policyThreatCategory.getName());
    assertThat(attributes.get(IS_SCM_ENABLED)).isEqualTo(isScmEnabled);
    assertThat(attributes.get(COUNT)).isEqualTo(1);
    assertThat(attributes.get(OPEN_TIME)).isEqualTo(openTime.getTime());
    assertThat(attributes.get(TIME)).isEqualTo(expectedTTR);
    assertThat(attributes.get(FIX_TIME)).isNull();

    // Important check
    assertThat(attributes.get(CONDITION_TYPE)).isEqualTo(validConditionType);
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
    return 1000L * 60 * 60 * hours;
  }

  private void assertTelemetryAttributes(
      TelemetryPurpose telemetryPurpose,
      int threatLevel,
      PolicyThreatCategory policyThreatCategory,
      boolean isScmEnabled,
      long expectedTTR,
      long openTime,
      Long waiveTime,
      Long unwaiveTime,
      Long fixTime,
      Integer count,
      Boolean isDirectDependency,
      Boolean isInnersource,
      String fixByVersionChange,
      TelemetryData telemetryData)
  {
    assertThat(telemetryData.getPurpose()).isEqualTo(telemetryPurpose);
    Map<String, Object> attributes = telemetryData.getAttributes();
    assertThat(attributes.get(APPLICATION_ID)).isNotEqualTo(policyEvaluation.getApplicationId());
    assertThat(attributes.get(STAGE)).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(attributes.get(THREAT_LEVEL)).isEqualTo(threatLevel);
    assertThat(attributes.get(THREAT_CATEGORY)).isEqualTo(policyThreatCategory.getName());
    assertThat(attributes.get(IS_SCM_ENABLED)).isEqualTo(isScmEnabled);
    assertThat(attributes.get(OPEN_TIME)).isEqualTo(openTime);
    assertThat(attributes.get(TIME)).isEqualTo(expectedTTR);
    if (waiveTime == null) {
      assertThat(attributes.get(WAIVE_TIME)).isNull();
    }
    else {
      assertThat(attributes.get(WAIVE_TIME)).isEqualTo(waiveTime);
    }

    if (unwaiveTime == null) {
      assertThat(attributes.get(UNWAIVE_TIME)).isNull();
    }
    else {
      assertThat(attributes.get(UNWAIVE_TIME)).isEqualTo(unwaiveTime);
    }

    if (fixTime == null) {
      assertThat(attributes.get(FIX_TIME)).isNull();
    }
    else {
      assertThat(attributes.get(FIX_TIME)).isEqualTo(fixTime);
    }

    if (count == null) {
      assertThat(attributes.get(COUNT)).isNull();
    }
    else {
      assertThat(attributes.get(COUNT)).isEqualTo(count);
    }

    if (isDirectDependency == null) {
      assertThat(attributes.get(DIRECT_DEPENDENCY)).isNull();
    }
    else {
      assertThat(attributes.get(DIRECT_DEPENDENCY)).isEqualTo(isDirectDependency);
    }

    if (isInnersource == null) {
      assertThat(attributes.get(INNERSOURCE_DEPENDENCY)).isNull();
    }
    else {
      assertThat(attributes.get(INNERSOURCE_DEPENDENCY)).isEqualTo(isInnersource);
    }

    if (fixByVersionChange == null) {
      assertThat(attributes.containsKey(FIX_BY_VERSION_CHANGE)).isFalse();
    }
    else {
      assertThat(attributes.get(FIX_BY_VERSION_CHANGE)).isEqualTo(fixByVersionChange);
    }
  }
}
