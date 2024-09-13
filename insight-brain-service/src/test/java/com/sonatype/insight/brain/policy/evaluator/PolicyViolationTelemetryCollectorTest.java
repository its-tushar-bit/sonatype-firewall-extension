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
import com.sonatype.clm.dto.model.policy.TriggerReference;
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
    final String policyName = "myPolicy";
    final String violationId = "123";
    final String ecosystem = "npm";
    final String cveNumber = "c-1-v-2-e";
    final double cvssScore = 0;
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
    PolicyViolation policyViolation =
        createPolicyViolation(threatLevel, policyThreatCategory, lodashv4, policyName, violationId,
            cveNumber, cvssScore, 2);
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
        telemetryData.get(0), policyViolation.getPolicyWaiverId(), ecosystem, policyName, violationId,
        cveNumber, cvssScore);
    assertTelemetryAttributes(TIME_TO_CHANGE_VERSION_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, isDirectDependency, isInnerSource,
        "upgrade", telemetryData.get(1), policyViolation.getPolicyWaiverId(), ecosystem, policyName, violationId,
        cveNumber, cvssScore);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByDowngrade() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final String policyName = "defaultPolicyName";
    final String ecosystem = "npm";
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
    PolicyViolation policyViolation = createPolicyViolation(threatLevel, policyThreatCategory, lodashv5, policyName);
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
        telemetryData.get(0), policyViolation.getPolicyWaiverId(), ecosystem, policyName, null, null, null);
    assertTelemetryAttributes(TIME_TO_CHANGE_VERSION_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, isDirectDependency, isInnerSource,
        "downgrade", telemetryData.get(1), policyViolation.getPolicyWaiverId(), ecosystem, policyName, null,
        null, null);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByRemoval() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final String policyName = "myPolicy2";
    final String policyViolationId = "456";
    final String cveNumber = "cve-888";
    final double cvssScore = 2.2;
    final boolean isScmEnabled = true;
    final Date evalTime = new Date();
    final long expectedTTR = msForHours(37);
    final Date openTime = new Date(evalTime.getTime() - expectedTTR);
    PolicyViolation policyViolation =
        createPolicyViolation(threatLevel, policyThreatCategory, lodashv5, policyName, policyViolationId,
            cveNumber, cvssScore, 1);
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
        expectedTTR, openTime.getTime(), null, null, evalTime.getTime(), 1, null, null, null, telemetryData.get(0),
        policyViolation.getPolicyWaiverId(), null, policyName, policyViolationId, cveNumber, cvssScore);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedWithoutVersionChange() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final String policyName = "myPolicy1";
    final String policyViolationId = "456";
    final String ecosystem = "npm";
    final String cveNumber = "cve-000";
    final double cvssScore = 3.3;
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
    PolicyViolation policyViolation =
        createPolicyViolation(threatLevel, policyThreatCategory, lodashv5, policyName, policyViolationId,
            cveNumber, cvssScore, 2);
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
        telemetryData.get(0), policyViolation.getPolicyWaiverId(), ecosystem, policyName, policyViolationId,
        cveNumber, cvssScore);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUndetermined() {
    // setup : create a fixed policy violation
    final int threatLevel = 9;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final String policyName = "aPolicyName";
    final String policyViolationId = "lodashv4Id";
    final String cveNumber = "cve-222";
    final double cvssScore = 4.1;
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
    PolicyViolation policyViolation =
        createPolicyViolation(threatLevel, policyThreatCategory, lodashv4, policyName, policyViolationId,
            cveNumber, cvssScore, 0);
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
        telemetryData.get(0), policyViolation.getPolicyWaiverId(), null, policyName, policyViolationId,
        cveNumber, cvssScore);
  }

  @Test
  public void testAddTelemetryForUnwaivedViolation() {
    // setup : create an unwaived policy violation
    final String oldPolicyWaiverId = "some-old-policy-waiver-id";
    final int threatLevel = 2;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final String policyName = "urllib3Policy";
    final String policyViolationId = "urllib3Id";
    final String ecosystem = "pypi";
    final String cveNumber = "cvx-xxx";
    final double cvssScore = 5.1;
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
    PolicyViolation policyViolation =
        createPolicyViolation(threatLevel, policyThreatCategory, urllib3, policyName, policyViolationId,
            cveNumber, cvssScore, 1);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    // when
    telemetryCollector.addTelemetryForUnwaivedViolation(policyViolation, component, oldPolicyWaiverId);
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();

    // then
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.size()).isEqualTo(1);
    assertTelemetryAttributes(TIME_TO_WAIVE_POLICY_VIOLATION, threatLevel, policyThreatCategory, isScmEnabled,
        expectedTTR, openTime.getTime(), evalTime.getTime(), evalTime.getTime(), null, -1, isDirectDependency,
        isInnerSource, null, telemetryData.get(0), "some-old-policy-waiver-id", ecosystem,
        policyName, policyViolationId, cveNumber, cvssScore);
  }

  @Test
  public void testAddTelemetryForWaivedViolation() {
    // setup : create a waived policy violation
    final int threatLevel = 7;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final String policyName = "commonsLang3Policy";
    final String policyViolationId = "commonsId";
    final String ecosystem = "maven";
    final String cveNumber = "cve-111";
    final double cvssScore = 1;
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

    PolicyViolation policyViolation =
        createPolicyViolation(threatLevel, policyThreatCategory, commonsLang3, policyName, policyViolationId,
            cveNumber, cvssScore, 1);
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
        isInnerSource, null, telemetryData.get(0), policyViolation.getPolicyWaiverId(), ecosystem,
        policyName, policyViolationId, cveNumber, cvssScore);
    assertThat(attributes.get(WAIVER_EXPIRATION)).isEqualTo(waiverExpirationInDays);
  }

  @Test
  public void testAddTelemetryForWaivedViolation_NoDependencyInfo() {
    // setup : create a waived policy violation
    Component component = new Component();
    component.setComponentIdentifier(commonsLang3);
    final String policyName = "defaultPolicyName";

    PolicyViolation policyViolation = createPolicyViolation(7, PolicyThreatCategory.SECURITY, commonsLang3,
        policyName);
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
    assertThat(attributes.get(POLICY_NAME)).isEqualTo(policyName);
  }

  @Test
  public void testAddTelemetryForLegacyViolation() {
    // setup : create a legacy policy violation
    final int threatLevel = 7;
    final PolicyThreatCategory policyThreatCategory = PolicyThreatCategory.SECURITY;
    final String policyName = "commonsLang3Policy";
    final String policyViolationId = "commonsId";
    final String ecosystem = "maven";
    final String cveNumber = "cve-456";
    final double cvssScore = 7.1;
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

    PolicyViolation policyViolation =
        createPolicyViolation(threatLevel, policyThreatCategory, commonsLang3, policyName, policyViolationId,
            cveNumber, cvssScore, 1);
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
        telemetryData.get(0), policyViolation.getPolicyWaiverId(), ecosystem, policyName, policyViolationId,
        cveNumber, cvssScore);
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
    final String cveNumber = "cve-123";
    final double cvssScore = 9.8;
    PolicyViolation policyViolation =
        createPolicyViolation(threatLevel, policyThreatCategory, commonsLang3, cveNumber, cvssScore, 0);
    policyViolation.setOpenTime(openTime);
    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(policyWaiverDAO,
        telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(evalTime);

    final TelemetryPurpose telemetryPurpose = TelemetryPurpose.CONDITION_TYPE_VIOLATION;
    final String validConditionType = HygieneRatingConditionType.ID;

    // when
    telemetryCollector.addTelemetryForConditionTypeViolation(
        policyViolation,
        validConditionType,
        Collections.emptyList()
    );
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
    assertThat(attributes.get(CVE_NUMBER)).isEqualTo(cveNumber);
    assertThat(attributes.get(CVSS_SCORE)).isEqualTo(cvssScore);

    // Important check
    assertThat(attributes.get(CONDITION_TYPE)).isEqualTo(validConditionType);
  }

  /**
   * A useful constraint fact must have at least 1 condition facts because that's where
   * the test data is. Several are created because in real word scenarios there may be multiple constraint facts
   * with one condition fact nested with needed data.
   * The number of constraint and condition facts is fixed.
   * Only one condition fact with the cv metadata is instantiated in the whole list of
   * constraint facts, it is possible to choose where.
   * Hardcoded values are not important for these tests.
   * @param cveNumber CVE id to inject in the condition fact
   * @param cvssScore Score to inject in the condition fact
   * @param cvIteration Iteration you want to insert the cv metadata
   * @return A list of constraint fact with the same amount of condition fact that contain the cv metadata
   */
  private List<ConstraintFact> createConstraintFactsWithInjectedCondition(
      String cveNumber,
      double cvssScore,
      int cvIteration)
  {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      ConstraintFact constraintFact = new ConstraintFact("constraintId" + i, "constraintName" + i, "operatorName" + i);
      constraintFacts.add(constraintFact);
      for (int j = 0; j < 3; j++) {
        ConditionFact conditionFact;

        if (cvIteration == i && cvIteration == j) {
          conditionFact = createConditionFactWithCVMetadata(j, cveNumber, cvssScore);
        }
        else {
          conditionFact = createConditionFact(j);
        }

        constraintFact.addConditionFact(conditionFact);
      }
    }
    return constraintFacts;
  }

  private ConditionFact createConditionFactWithCVMetadata(int j, String cveNumber, double cvssScore) {
    TriggerReference triggerReference =
        new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, cveNumber);
    String triggerJson =
        String.format("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"CVE-2013-7285\",\"severity\":%f}}", cvssScore);
    ConditionFact conditionFact = new ConditionFact(LicenseConditionType.ID, j, "summary", "reason", triggerReference);
    conditionFact.setTriggerJson(triggerJson);
    return conditionFact;
  }

  private ConditionFact createConditionFact(int j) {
    return new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, j, "summary", "reason");
  }

  private List<ConstraintFact> createConstraintFactsWithInjectedCondition() {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      ConstraintFact constraintFact = new ConstraintFact("constraintId" + i, "constraintName" + i, "operatorName" + i);
      constraintFacts.add(constraintFact);
      for (int j = 0; j < 10; j++) {
        String conditionType = (i == 5 && j == 5)
            ? LicenseConditionType.ID
            : SecurityVulnerabilitySeverityConditionType.ID;
        ConditionFact conditionFact = new ConditionFact(conditionType, j, "summary", "reason");
        constraintFact.addConditionFact(conditionFact);
      }
    }
    return constraintFacts;
  }

  private PolicyViolation createPolicyViolation(
      int threatLevel,
      PolicyThreatCategory threatCategory,
      ComponentIdentifier componentIdentifier,
      String policyName)
  {
    return new PolicyViolation(
        policyEvaluation,
        policyName,
        "defaultPolicyName",
        threatLevel,
        threatCategory,
        "hash" + 1000 * Math.random(),
        componentIdentifier,
        createConstraintFactsWithInjectedCondition(),
        "/etc/policyEval123.zip"
    );
  }

  private PolicyViolation createPolicyViolation(
      int threatLevel,
      PolicyThreatCategory threatCategory,
      ComponentIdentifier componentIdentifier,
      String policyName,
      String policyViolationId,
      String cveNumber,
      double cvssScore,
      int cvIteration)
  {
    final PolicyViolation policyViolation = new PolicyViolation(
        policyEvaluation,
        "somePolicyId",
        policyName,
        threatLevel,
        threatCategory,
        "hash" + 1000 * Math.random(),
        componentIdentifier,
        createConstraintFactsWithInjectedCondition(cveNumber, cvssScore, cvIteration),
        "/etc/policyEval123.zip"
    );
    policyViolation.setId(policyViolationId);
    return policyViolation;
  }

  private PolicyViolation createPolicyViolation(
      int threatLevel,
      PolicyThreatCategory threatCategory,
      ComponentIdentifier componentIdentifier,
      String cveNumber,
      double cvssScore,
      int cvIteration)
  {
    final PolicyViolation policyViolation = new PolicyViolation(
        policyEvaluation,
        "somePolicyId",
        null,
        threatLevel,
        threatCategory,
        "hash" + 1000 * Math.random(),
        componentIdentifier,
        createConstraintFactsWithInjectedCondition(cveNumber, cvssScore, cvIteration),
        "/etc/policyEval123.zip"
    );
    return policyViolation;
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
      TelemetryData telemetryData,
      final String policyWaiverId,
      final String ecosystem,
      final String policyName,
      final String policyViolationId,
      final String cveNumber,
      final Double cvssScore)
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
    assertThat(attributes.get(POLICY_WAIVER_ID)).isEqualTo(policyWaiverId);
    assertThat(attributes.get(CVE_NUMBER)).isEqualTo(cveNumber);
    assertThat(attributes.get(CVSS_SCORE)).isEqualTo(cvssScore);

    assertNullableAttribute(attributes, WAIVE_TIME, waiveTime);
    assertNullableAttribute(attributes, UNWAIVE_TIME, unwaiveTime);
    assertNullableAttribute(attributes, FIX_TIME, fixTime);
    assertNullableAttribute(attributes, COUNT, count);
    assertNullableAttribute(attributes, DIRECT_DEPENDENCY, isDirectDependency);
    assertNullableAttribute(attributes, INNERSOURCE_DEPENDENCY, isInnersource);

    assertNonNullableAttribute(attributes, FIX_BY_VERSION_CHANGE, fixByVersionChange);
    assertNonNullableAttribute(attributes,ECOSYSTEM , ecosystem);
    assertNonNullableAttribute(attributes, CVE_NUMBER, cveNumber);
    assertNonNullableAttribute(attributes, CVSS_SCORE, cvssScore);

    assertThat(attributes.containsKey(POLICY_NAME)).isTrue();
    assertThat(attributes.get(POLICY_NAME)).isEqualTo(policyName);
    assertThat(attributes.get(POLICY_VIOLATION_ID)).isEqualTo(policyViolationId);
  }

  private void assertNullableAttribute(Map<String, Object> attributes, String key, Object expectedValue) {
    if (expectedValue == null) {
      assertThat(attributes.get(key)).isNull();
    }
    else {
      assertThat(attributes.get(key)).isEqualTo(expectedValue);
    }
  }

  private void assertNonNullableAttribute(Map<String, Object> attributes, String key, Object expectedValue) {
    if (expectedValue == null) {
      assertThat(attributes.containsKey(key)).isFalse();
    }
    else {
      assertThat(attributes.get(key)).isEqualTo(expectedValue);
    }
  }
}
