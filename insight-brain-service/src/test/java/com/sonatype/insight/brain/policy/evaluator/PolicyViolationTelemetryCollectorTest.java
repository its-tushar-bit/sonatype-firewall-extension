/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.PresentReachableComponentVulnerabilities;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetryBuilder;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.hasPolicyViolationByComponentIdentifier;
import static com.sonatype.insight.brain.model.policy.conditions.ConditionTypes.HygieneRatingConditionType;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.*;
import static com.sonatype.insight.brain.telemetry.TelemetryUtils.REAL_APPLICATION_ID;
import static com.sonatype.insight.purl.PackageUrlIdentifier.fromComponentIdentifier;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.CALLFLOW_EVALUATION_COMPONENT_COUNTS;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.CONDITION_TYPE_VIOLATION;
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

  private static final LocalDate FIXED_TEST_DATE = LocalDate.of(2026, 1, 15);

  private static final Clock FIXED_CLOCK =
      Clock.fixed(FIXED_TEST_DATE.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private static final PolicyEvaluation policyEvaluation =
      new PolicyEvaluation(TEST_APP_ID, TEST_STAGE, "scanId123", CurrentUser.SYSTEM, ScanTriggerType.CLI);

  static {
    policyEvaluation.setTime(new Date());
  }

  private static final ComponentIdentifier commonsLang3 = ComponentIdentifier.createMavenCoordinates(
      "org.apache.commons", "commons-lang3", "3.8.1");

  private static final ComponentIdentifier jacksonDatabind_2_13_4 = ComponentIdentifier.createMavenCoordinates(
      "com.fasterxml.jackson.core", "jackson-databind", "2.13.4");

  private static final ComponentIdentifier jacksonDatabind_2_13_5 = ComponentIdentifier.createMavenCoordinates(
      "com.fasterxml.jackson.core", "jackson-databind", "2.13.5");

  private static final ComponentIdentifier lodashv3 = ComponentIdentifier.createNpmCoordinates("lodash", "3.0.4");

  private static final ComponentIdentifier lodashv4 = ComponentIdentifier.createNpmCoordinates("lodash", "4.17.15");

  private static final ComponentIdentifier lodashv5 = ComponentIdentifier.createNpmCoordinates("lodash", "5.1.0");

  private static final ComponentIdentifier urllib3 = ComponentIdentifier.createPypiCoordinates(
      "urllib3", "1.25.7", null, "py");

  private static final String TEST_POLICY = "testPolicy";

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private TelemetryUtils telemetryUtils;

  @Inject
  private LicenseNameProvider licenseNameProvider;

  @Override
  @Before
  public void beforeTest() {
    // Call parent's beforeTest first to ensure proper initialization
    super.beforeTest();

    // Clear the LicenseDataUpdater singleton before each test to prevent it from holding references
    // to DAOs with closed EntityManagerFactory instances from previous tests.
    // This is necessary because tests extending AbstractComponentTest may close the EntityManagerFactory
    // in their @After methods, but the singleton may still hold a reference to it.
    LicenseDataUpdater.setUpdater(null);
  }

  @After
  public void cleanUpLicenseDataUpdater() {
    // Clear the LicenseDataUpdater singleton to prevent it from holding references
    // to DAOs with closed EntityManagerFactory instances from previous tests
    LicenseDataUpdater.setUpdater(null);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolation() {
    // given a new policy violation and a telemetry collector
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createMinimalViolationForComponent(commonsLang3)
            .withPolicyViolationId("conditionTypeViolation")
            .withThreatCategory(PolicyThreatCategory.SECURITY)
            .withThreatLevel(7)
            .withGenericConditionType(HygieneRatingConditionType.ID);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // Build the constraints test data as
    List<Constraint> formattedConstraints = testablePolicyViolation.policyViolation.getConstraintFacts()
        .stream()
        .map(
            cf -> {
              List<Condition> conditions = cf.getConditionFacts()
                  .stream()
                  .map(condF -> telemetryCollector
                      .formatConditionForTelemetryData(condF, cf.getOperatorName()))
                  .collect(Collectors.toList());
              return telemetryCollector.formatConstraintForTelemetryData(cf, conditions);
            })
        .toList();

    // when
    telemetryCollector.addTelemetryForConditionTypeViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        formattedConstraints);

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, CONDITION_TYPE_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByDowngrade() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .openedHoursAgo(48)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByDowngrade")
            .markFixedByDowngrade()
            .withExpectedRemediationVersion(lodashv3.get(ComponentIdentifier.VERSION));

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in downgraded component
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(lodashv3, true, false));

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(
        telemetryData,
        TIME_TO_REMEDIATE_POLICY_VIOLATION,
        TIME_TO_CHANGE_VERSION_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByOtherMeans() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .openedHoursAgo(2)
            .asDirectDependency(true)
            .asInnerSourceDependency(true)
            .withPolicyViolationId("fixedByOtherMeans")
            .withAdditionalComponentVersion(lodashv3, true, true)
            .withAdditionalComponentVersion(lodashv5, true, true)
            .markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in multiple additional versions
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getAdditionalVersions());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByRemoval() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .openedHoursAgo(37)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByRemoval")
            .markFixedByRemoval();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in empty component list
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .withScmEnabled(true)
            .openedHoursAgo(72)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByUpgrade")
            .markFixedByUpgrade()
            .withExpectedRemediationVersion(lodashv5.get(ComponentIdentifier.VERSION));

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in an upgraded component
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(lodashv5, true, false));

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(
        telemetryData,
        TIME_TO_REMEDIATE_POLICY_VIOLATION,
        TIME_TO_CHANGE_VERSION_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_WithIsRemediatedByVersionChangeTrue() {
    // given a policy violation on lodash v4 that was fixed by upgrading with isRemediatedByVersionChange set to true
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .withScmEnabled(true)
            .openedHoursAgo(72)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByUpgradeWithVersionChangeFlag")
            .markFixedByUpgrade()
            .withIsRemediatedByVersionChange(true)
            .withExpectedRemediationVersion(lodashv5.get(ComponentIdentifier.VERSION));

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in an upgraded component
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(lodashv5, true, false));

    // then - verify telemetry includes isRemediatedByVersionChange field
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(
        telemetryData,
        TIME_TO_REMEDIATE_POLICY_VIOLATION,
        TIME_TO_CHANGE_VERSION_POLICY_VIOLATION);

    // Additional explicit check for the new field
    TelemetryData ttcvpvData = telemetryData.stream()
        .filter(td -> td.getPurpose() == TIME_TO_CHANGE_VERSION_POLICY_VIOLATION)
        .findFirst()
        .orElseThrow();
    assertThat(ttcvpvData.getAttributes()).containsEntry(REMEDIATION_BY_VERSION_CHANGE, true);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_WithIsRemediatedByVersionChangeNull() {
    // given a policy violation on lodash v4 that was fixed by upgrading but isRemediatedByVersionChange is not set
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .withScmEnabled(true)
            .openedHoursAgo(72)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByUpgradeWithoutVersionChangeFlag")
            .markFixedByUpgrade()
            .withExpectedRemediationVersion(lodashv5.get(ComponentIdentifier.VERSION));
    // Note: isRemediatedByVersionChange is NOT set, so it should be null in telemetry

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in an upgraded component
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(lodashv5, true, false));

    // then - verify telemetry includes isRemediatedByVersionChange field as null
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(
        telemetryData,
        TIME_TO_REMEDIATE_POLICY_VIOLATION,
        TIME_TO_CHANGE_VERSION_POLICY_VIOLATION);

    // Additional explicit check - field should be present with null value
    TelemetryData ttcvpvData = telemetryData.stream()
        .filter(td -> td.getPurpose() == TIME_TO_CHANGE_VERSION_POLICY_VIOLATION)
        .findFirst()
        .orElseThrow();
    assertThat(ttcvpvData.getAttributes()).containsEntry(REMEDIATION_BY_VERSION_CHANGE, null);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByRemoval_WithIsRemediatedByVersionChangeFalse() {
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .withScmEnabled(true)
            .openedHoursAgo(48)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByRemovalWithFalseFlag")
            .markFixedByRemoval()
            .withIsRemediatedByVersionChange(false);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents());

    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(
        telemetryData,
        TIME_TO_REMEDIATE_POLICY_VIOLATION);

    // Additional check - TIME_TO_CHANGE_VERSION_POLICY_VIOLATION should NOT be present
    boolean hasTTCVPV = telemetryData.stream()
        .anyMatch(td -> td.getPurpose() == TIME_TO_CHANGE_VERSION_POLICY_VIOLATION);
    assertThat(hasTTCVPV).isFalse();

    // The false value is stored in DB but not sent in telemetry because
    // TIME_TO_CHANGE_VERSION_POLICY_VIOLATION is only sent when version actually changes
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_WithRemediationPullRequestAttribution() {
    // given a policy violation on jackson-databind v2.13.4 that was fixed by upgrading to v2.13.5 and a matching
    // remediation PR event
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(jacksonDatabind_2_13_4)
            .withScmEnabled(true)
            .openedHoursAgo(24)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByUpgradeWithPR")
            .markFixedByUpgrade();

    // Ensure the referenced application exists to satisfy FK constraints
    // In tests we use a fixed applicationId (TEST_APP_ID), so create an application with that ID
    String orgId = tempEntity.newOrganization().getId();
    tempEntity.newApplicationWithSpecificId(TEST_APP_ID, "Test App for PR", "TestAppPublic", orgId);

    // Insert a completed remediation PR event matching the component and remediation version within the cutoff window
    int prNumber = 123;
    String remediationVersion = jacksonDatabind_2_13_5.get(ComponentIdentifier.VERSION);
    Date eventCompleteTime = new Date(policyEvaluation.getTime().getTime() - 60_000L); // 1 minute before evaluation

    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId(TEST_APP_ID)
        .setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT)
        .setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE)
        .setCreateTime(policyEvaluation.getTime())
        .setCompleteTime(eventCompleteTime)
        .setPullRequestNumber(prNumber);
    event.setRemediationVersion(remediationVersion);
    event.setComponentIdentifier(jacksonDatabind_2_13_4);
    sourceControlEventDAO.insert(event);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in an upgraded component (version matches event remediation version)
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(jacksonDatabind_2_13_5, true, false));

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation
        .withExpectedRemediationAttribution(prNumber, remediationVersion, true)
        .withExpectedRemediationVersion(remediationVersion)
        .validateTelemetryDataForPurposes(
            telemetryData,
            TIME_TO_REMEDIATE_POLICY_VIOLATION,
            TIME_TO_CHANGE_VERSION_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedWithoutVersionChange() {
    // given a policy violation on lodash v5
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv5)
            .openedHoursAgo(480)
            .asDirectDependency(false)
            .withPolicyViolationId("fixedWithoutVersionChange")
            .markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedWithoutVersionChange_licenseData() {
    // given a policy violation for a license vulnerability
    final var declaredLicenses = "D1, D2";
    final var declaredMultiLicenses = "DML1, DML2";
    final var observedLicenses = "O1, O2";
    final var observedMultiLicenses = "OML1, OML2";
    final var licenseOverrides = "OML2, D1";

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultLicenseViolationForComponent(lodashv5)
            .openedHoursAgo(480)
            .withScmEnabled(false)
            .withThreatLevel(10)
            .withConditionType(LicenseThreatGroupConditionType.ID)
            .asDirectDependency(false)
            .withPolicyViolationId("fixedWithoutVersionChange_license")
            .withLicenses(declaredLicenses, declaredMultiLicenses, observedLicenses, observedMultiLicenses)
            .markFixedByLicenseOverride(licenseOverrides, LicenseOverrideStatus.SELECTED);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForLegacyViolation() {
    // given a policy violation on lodash v3
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv3)
            .openedHoursAgo(480)
            .asDirectDependency(false)
            .withPolicyViolationId("legacyViolation")
            .markFixedAsLegacy();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForLegacyViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_LEGACY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForLegacyViolationAudit_BasicAudit() {
    // given: An unchanged legacy policy violation
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(720)
            .asDirectDependency(true)
            .withPolicyViolationId("unchangedLegacyViolation")
            .markFixedAsLegacy();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForLegacyViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(
        TelemetryPurpose.TIME_TO_LEGACY_VIOLATION_AUDIT);
    assertThat(telemetryData.get(0).getAttributes()).containsKey(LEGACY_VIOLATION_TIME);
  }

  @Test
  public void testAddTelemetryForLegacyViolationAudit_DifferentPurposeFromRegular() {
    // given: A legacy violation
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .openedHoursAgo(500)
            .markFixedAsLegacy();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when: Add both regular and audit telemetry
    telemetryCollector.addTelemetryForLegacyViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    telemetryCollector.addTelemetryForLegacyViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then: Verify both telemetry entries exist with different purposes
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(2);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TIME_TO_LEGACY_VIOLATION);
    assertThat(telemetryData.get(1).getPurpose()).isEqualTo(
        TelemetryPurpose.TIME_TO_LEGACY_VIOLATION_AUDIT);
  }

  @Test
  public void testAddTelemetryForLegacyViolationAudit_NullPolicyViolation() {
    // given
    PolicyViolationTelemetryCollector telemetryCollector = createTelemetryCollector(false);

    // when: Pass null policy violation
    telemetryCollector.addTelemetryForLegacyViolationAudit(null, null);

    // then: No telemetry should be added
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).isEmpty();
  }

  @Test
  public void testAddTelemetryForLegacyViolationAudit_IncludesStandardFields() {
    // given: Legacy violation with specific component
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(jacksonDatabind_2_13_4)
            .openedHoursAgo(1000)
            .asDirectDependency(true)
            .withPolicyViolationId("legacyViolationWithMetadata")
            .markFixedAsLegacy();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForLegacyViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then: Should include all standard telemetry fields
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);
    TelemetryData data = telemetryData.get(0);

    // Verify standard fields are present
    assertThat(data.getAttributes()).containsKeys(
        APPLICATION_ID,
        COMPONENT_IDENTIFIER,
        ECOSYSTEM,
        COMPONENT_NAME,
        COMPONENT_VERSION,
        LEGACY_VIOLATION_TIME,
        DIRECT_DEPENDENCY,
        POLICY_VIOLATION_ID);

    // Verify component metadata
    assertThat(data.getAttributes().get(ECOSYSTEM)).isEqualTo("maven");
    assertThat(data.getAttributes().get(COMPONENT_NAME)).isEqualTo("jackson-databind");
    assertThat(data.getAttributes().get(COMPONENT_VERSION)).isEqualTo("2.13.4");
    assertThat(data.getAttributes().get(DIRECT_DEPENDENCY)).isEqualTo(true);
  }

  @Test
  public void testAddTelemetryForLegacyViolationAudit_WithInnerSourceComponent() {
    // given: Legacy violation with inner source component
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
            .openedHoursAgo(600)
            .asInnerSourceDependency(true)
            .markFixedAsLegacy();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForLegacyViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then: Should mark as inner source dependency
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getAttributes().get(INNERSOURCE_DEPENDENCY)).isEqualTo(true);
  }

  @Test
  public void testAddTelemetryForLegacyViolationAudit_GatedToOncePerDay_SecondCallSameDaySkipped() {
    // given: A legacy violation already emitted today
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(720)
            .markFixedAsLegacy();
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLastTelemetryEmittedDate(FIXED_TEST_DATE);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);

    // when: Called again on same day
    telemetryCollector.addTelemetryForLegacyViolationAudit(violation, testablePolicyViolation.getComponent());

    // then: Skipped — gate suppresses duplicate same-day emit
    assertThat(telemetryCollector.getTelemetryData()).isEmpty();
  }

  @Test
  public void testAddTelemetryForLegacyViolationAudit_GatedToOncePerDay_PreviousDayAllowed() {
    // given: A legacy violation last emitted yesterday
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(720)
            .markFixedAsLegacy();
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLastTelemetryEmittedDate(FIXED_TEST_DATE.minusDays(1));

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);

    // when
    telemetryCollector.addTelemetryForLegacyViolationAudit(violation, testablePolicyViolation.getComponent());

    // then: Emitted — previous day allows new emit; date stamped to today
    assertThat(telemetryCollector.getTelemetryData()).hasSize(1);
    assertThat(telemetryCollector.getTelemetryData().get(0).getPurpose())
        .isEqualTo(TelemetryPurpose.TIME_TO_LEGACY_VIOLATION_AUDIT);
    assertThat(violation.getLastTelemetryEmittedDate()).isEqualTo(FIXED_TEST_DATE);
  }

  @Test
  public void testAddTelemetryForUnwaivedViolation() {
    // given a policy violation that was unwaived and the new open violation created for it
    var policyWaiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyEvaluation.getOwnerId());

    var replacementPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(0)
        .asDirectDependency(true)
        .withPolicyViolationId("newViolationAfterUnwaived")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    var unwaivedPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(500)
        .asDirectDependency(true)
        .withPolicyViolationId("unwaivedViolation")
        .markWaived(policyWaiver)
        .markUnwaived(policyWaiver.getId(), replacementPolicyViolation.getPolicyViolation());

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(unwaivedPolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForUnwaivedViolation(
        unwaivedPolicyViolation.getPolicyViolation(),
        replacementPolicyViolation.getPolicyViolation(),
        unwaivedPolicyViolation.getComponent());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    unwaivedPolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForWaivedViolation() {
    // given a policy violation and a waiver for it
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyEvaluation.getOwnerId());

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(5)
            .asDirectDependency(true)
            .withPolicyViolationId("waivedViolation")
            .markWaived(policyWaiver);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForWaivedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForAutoWaivedViolation() {
    // given a policy violation and a waiver for it
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(policyEvaluation.getOwnerId());

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(5)
            .asDirectDependency(true)
            .withPolicyViolationId("waivedViolation")
            .markAutoWaived(autoPolicyWaiver);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForAutoWaivedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForUnwaivedViolation_wasAutoWaived() {
    // given an uwaived policy violation and the new open violation that replaces it
    var autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(policyEvaluation.getOwnerId());

    var replacementPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(0)
        .asDirectDependency(true)
        .withPolicyViolationId("newViolationAfterUnwaived")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    var unwaivedPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(500)
        .asDirectDependency(true)
        .withPolicyViolationId("unwaivedViolation")
        .markAutoWaived(autoPolicyWaiver)
        .markUnAutoWaived(autoPolicyWaiver.getId(), replacementPolicyViolation.getPolicyViolation());

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(unwaivedPolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForUnwaivedViolation(
        unwaivedPolicyViolation.getPolicyViolation(),
        replacementPolicyViolation.getPolicyViolation(),
        unwaivedPolicyViolation.getComponent());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    unwaivedPolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForReachableViolation_When_ViolationIsNotReachable() {
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv3);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    telemetryCollector.addTelemetryForReachableViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent(),
        null);

    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, CALLFLOW_EVALUATION_COMPONENT_COUNTS);
  }

  @Test
  public void testAddTelemetryForReachableViolation_When_ViolationIsReachable() {
    TestablePolicyViolation testablePolicyViolation = TestablePolicyViolation
        .createDefaultSecurityViolationForComponent(lodashv3);

    PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities = new PurlIdentifiersWithVulnerabilities(
        null,
        null,
        Map.of(
            fromComponentIdentifier(testablePolicyViolation.policyViolation.getComponentIdentifier()),
            new PresentReachableComponentVulnerabilities(Set.of("CVE-1234"))));

    testablePolicyViolation.withPurlIdentifiersWithVulnerabilities(purlIdentifiersWithVulnerabilities);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    telemetryCollector.addTelemetryForReachableViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent(),
        purlIdentifiersWithVulnerabilities);

    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, CALLFLOW_EVALUATION_COMPONENT_COUNTS);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_BasicAudit() {
    // given: A policy violation with constraints
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // Build the constraints test data
    List<Constraint> formattedConstraints = testablePolicyViolation.policyViolation.getConstraintFacts()
        .stream()
        .map(
            cf -> {
              List<Condition> conditions = cf.getConditionFacts()
                  .stream()
                  .map(
                      condF -> telemetryCollector
                          .formatConditionForTelemetryData(condF, cf.getOperatorName()))
                  .collect(Collectors.toList());
              return telemetryCollector.formatConstraintForTelemetryData(cf, conditions);
            })
        .toList();

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        formattedConstraints);

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData,
        TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_DifferentPurposeFromRegular() {
    // given
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    List<Constraint> formattedConstraints = buildFormattedConstraints(telemetryCollector,
        testablePolicyViolation);

    // when- Add both regular and audit telemetry
    telemetryCollector.addTelemetryForConditionTypeViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        formattedConstraints);

    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        formattedConstraints);

    // then- Verify both telemetry entries exist with different purposes
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(2);

    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.CONDITION_TYPE_VIOLATION);
    assertThat(telemetryData.get(1).getPurpose()).isEqualTo(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_NullPolicyViolation() {
    // given
    PolicyViolationTelemetryCollector telemetryCollector = createTelemetryCollector(false);

    // when: Pass null policy violation
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        null,
        Collections.emptyList(),
        Collections.emptyList());

    // then: No telemetry should be added
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).isEmpty();
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_EmptyConstraints() {
    // given
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when- Pass empty constraints
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        Collections.emptyList());

    // then- Telemetry should still be added with empty constraints
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(
        TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
    assertThat(telemetryData.get(0).getAttributes().get(POLICY_CONSTRAINTS)).isEqualTo(
        Collections.emptyList());
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_MultipleComponents() {
    // given: Policy violation with multiple component versions
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .withConditionType(SecurityVulnerabilitySeverityConditionType.ID)
            .withAdditionalComponentVersion(lodashv3, true, false)
            .withAdditionalComponentVersion(lodashv5, true, false);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    List<Constraint> formattedConstraints = buildFormattedConstraints(telemetryCollector,
        testablePolicyViolation);

    // when: Pass multiple components
    List<Component> allComponents = new ArrayList<>(testablePolicyViolation.getComponents());
    allComponents.addAll(testablePolicyViolation.getAdditionalVersions());

    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        allComponents,
        formattedConstraints);

    // then: Should use first component for telemetry
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData,
        TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_LicenseViolation() {
    // given: License threat violation
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultLicenseViolationForComponent(commonsLang3)
            .withConditionType(LicenseThreatGroupConditionType.ID)
            .withLicenses("MIT", "Apache-2.0", "BSD-3-Clause", "GPL-3.0");

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    List<Constraint> formattedConstraints = buildFormattedConstraints(telemetryCollector,
        testablePolicyViolation);

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        formattedConstraints);

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData,
        TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_WaivedViolation() {
    // given: A waived policy violation
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyEvaluation.getOwnerId());

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .withPolicyViolationId("waivedViolationAudit")
            .markWaived(policyWaiver);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    List<Constraint> formattedConstraints = buildFormattedConstraints(telemetryCollector,
        testablePolicyViolation);

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent() != null
            ? List.of(testablePolicyViolation.getComponent())
            : Collections.emptyList(),
        formattedConstraints);

    // then: Should capture waived violations in audit telemetry
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(
        TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_WithScmEnabled() {
    // given: Violation with SCM enabled
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(jacksonDatabind_2_13_4)
            .withScmEnabled(true);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(true);

    List<Constraint> formattedConstraints = buildFormattedConstraints(telemetryCollector,
        testablePolicyViolation);

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(

        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        formattedConstraints);

    // then- Should include SCM enabled flag
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getAttributes()).containsEntry(IS_SCM_ENABLED, true);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_EmitsWithElapsedTimeAttribute() {
    // given: violation opened 48 hours ago
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .openedHoursAgo(48);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    List<Constraint> formattedConstraints = buildFormattedConstraints(telemetryCollector,
        testablePolicyViolation);

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        formattedConstraints);

    // then: telemetry emitted and TIME attribute is present (elapsed ms since open, same as
    // CONDITION_TYPE_VIOLATION — computed by createTelemetry via computeTimeBetween)
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getAttributes()).containsKey(TIME);
    assertThat((long) telemetryData.get(0).getAttributes().get(TIME)).isGreaterThan(0L);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_emittedFirstTimeWhenNoPriorDate() {
    // given: violation with no prior telemetry date
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    assertThat(violation.getLastTelemetryEmittedDate()).isNull();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation,
        testablePolicyViolation.getComponents(),
        constraints);

    // then: telemetry emitted and date stamped to today UTC
    assertThat(telemetryCollector.getTelemetryData()).hasSize(1);
    assertThat(telemetryCollector.getTelemetryData().get(0).getPurpose())
        .isEqualTo(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
    assertThat(violation.getLastTelemetryEmittedDate()).isEqualTo(FIXED_TEST_DATE);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_suppressedWhenAlreadyEmittedToday() {
    // given: violation with last emitted date already set to today
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLastTelemetryEmittedDate(FIXED_TEST_DATE);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation,
        testablePolicyViolation.getComponents(),
        constraints);

    // then: no telemetry emitted (already emitted today)
    assertThat(telemetryCollector.getTelemetryData()).isEmpty();
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_reemittedWhenLastEmitWasYesterday() {
    // given: violation last emitted yesterday
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLastTelemetryEmittedDate(FIXED_TEST_DATE.minusDays(1));

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation,
        testablePolicyViolation.getComponents(),
        constraints);

    // then: telemetry emitted and date updated to today
    assertThat(telemetryCollector.getTelemetryData()).hasSize(1);
    assertThat(violation.getLastTelemetryEmittedDate()).isEqualTo(FIXED_TEST_DATE);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_suppressedOnSecondCallSameScan() {
    // given: same violation object passed twice (simulates consecutive scans in same day)
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);

    // when: called twice for the same violation object (first call sets date, second skips)
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation, testablePolicyViolation.getComponents(), constraints);
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation, testablePolicyViolation.getComponents(), constraints);

    // then: only one telemetry entry emitted
    assertThat(telemetryCollector.getTelemetryData()).hasSize(1);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_SkipsLegacyViolations() {
    // given: an applied legacy violation (legacyViolationTime != null makes isLegacyViolation() true)
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(720);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLegacyViolationTime(new Date(System.currentTimeMillis() - 720 * 3600_000L));
    violation.setLegacyViolationApplied(true);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation, testablePolicyViolation.getComponents(), constraints);

    // then: legacy violations are excluded — they have their own audit path
    assertThat(telemetryCollector.getTelemetryData()).isEmpty();
    assertThat(violation.getLastTelemetryEmittedDate()).isNull();
  }

  @Test
  public void testConditionTypeAuditThenLegacyAudit_OnSameViolation_LegacyAuditStillEmits() {
    // given: an applied legacy violation with no prior emit
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(720);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLegacyViolationTime(new Date(System.currentTimeMillis() - 720 * 3600_000L));
    violation.setLegacyViolationApplied(true);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);

    // when: ScanPolicyEvaluator calls condition-type audit first (line 842), then legacy audit (line 974)
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation, testablePolicyViolation.getComponents(), constraints);
    telemetryCollector.addTelemetryForLegacyViolationAudit(violation, testablePolicyViolation.getComponent());

    // then: condition-type audit is skipped (legacy violation), legacy audit emits once
    assertThat(telemetryCollector.getTelemetryData()).hasSize(1);
    assertThat(telemetryCollector.getTelemetryData().get(0).getPurpose())
        .isEqualTo(TelemetryPurpose.TIME_TO_LEGACY_VIOLATION_AUDIT);
    assertThat(violation.getLastTelemetryEmittedDate()).isEqualTo(FIXED_TEST_DATE);
  }

  @Test
  public void testLegacyViolationTransitionDay_ConditionTypeAuditRunsFirst_LegacyAuditSuppressedSameDay() {
    // given: violation with legacyViolationTime set but legacyViolationApplied=false (not yet persisted)
    // — this is the state at ScanPolicyEvaluator line 842 on the transition scan
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(720);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLegacyViolationTime(new Date(System.currentTimeMillis() - 720 * 3600_000L));
    // legacyViolationApplied is false (default) — guard in addTelemetryForConditionTypeViolationAudit
    // does not fire yet

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);

    // when: condition-type audit runs first (applied=false → guard inactive → emits + stamps date)
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation, testablePolicyViolation.getComponents(), constraints);
    assertThat(violation.getLastTelemetryEmittedDate()).isEqualTo(FIXED_TEST_DATE);

    // ScanPolicyEvaluator line 964: !isLegacyViolationApplied() → enters the if-branch, sets
    // legacyViolationApplied=true and calls addTelemetryForLegacyViolation (not audit).
    // addTelemetryForLegacyViolationAudit is in the else-if branch and is never reached on the
    // transition scan. This call is a defensive gate test only — it verifies that IF the audit
    // method were called after the date was stamped, the gate would still suppress it.
    violation.setLegacyViolationApplied(true);
    telemetryCollector.addTelemetryForLegacyViolationAudit(violation, testablePolicyViolation.getComponent());

    // then: only CONDITION_TYPE_VIOLATION_AUDIT emitted; the defensive call above is suppressed
    // by the gate (date already stamped). In production the audit method is never called here at all.
    assertThat(telemetryCollector.getTelemetryData()).hasSize(1);
    assertThat(telemetryCollector.getTelemetryData().get(0).getPurpose())
        .isEqualTo(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_ComplianceStageLegacyViolation_StillEmits() {
    // given: an applied legacy violation at COMPLIANCE stage — ScanPolicyEvaluator never calls
    // addTelemetryForLegacyViolationAudit for COMPLIANCE stage, so CONDITION_TYPE_VIOLATION_AUDIT
    // is its only audit path and must not be skipped
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(720);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLegacyViolationTime(new Date(System.currentTimeMillis() - 720 * 3600_000L));
    violation.setLegacyViolationApplied(true);
    violation.setStageTypeId(Stage.ID_COMPLIANCE);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);
    assertThat(violation.getLastTelemetryEmittedDate()).isNull();

    // when
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation, testablePolicyViolation.getComponents(), constraints);

    // then: COMPLIANCE-stage legacy violations are NOT skipped — they emit CONDITION_TYPE_VIOLATION_AUDIT
    assertThat(telemetryCollector.getTelemetryData()).hasSize(1);
    assertThat(telemetryCollector.getTelemetryData().get(0).getPurpose())
        .isEqualTo(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT);
    assertThat(violation.getLastTelemetryEmittedDate()).isEqualTo(FIXED_TEST_DATE);
  }

  @Test
  public void testAddTelemetryForConditionTypeViolationAudit_ComplianceStageLegacyViolation_GatedToOncePerDay() {
    // given: a COMPLIANCE-stage applied legacy violation already emitted today
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(720);
    PolicyViolation violation = testablePolicyViolation.getPolicyViolation();
    violation.setLegacyViolationTime(new Date(System.currentTimeMillis() - 720 * 3600_000L));
    violation.setLegacyViolationApplied(true);
    violation.setStageTypeId(Stage.ID_COMPLIANCE);
    violation.setLastTelemetryEmittedDate(FIXED_TEST_DATE);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());
    telemetryCollector.setClockForTesting(FIXED_CLOCK);
    List<Constraint> constraints = buildFormattedConstraints(telemetryCollector, testablePolicyViolation);

    // when: called a second time on the same day
    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        violation, testablePolicyViolation.getComponents(), constraints);

    // then: suppressed — already emitted today
    assertThat(telemetryCollector.getTelemetryData()).isEmpty();
    assertThat(violation.getLastTelemetryEmittedDate()).isEqualTo(FIXED_TEST_DATE);
  }

  private List<Constraint> buildFormattedConstraints(
      PolicyViolationTelemetryCollector telemetryCollector,
      TestablePolicyViolation testablePolicyViolation)
  {
    return testablePolicyViolation.policyViolation.getConstraintFacts()
        .stream()
        .map(
            cf -> {
              List<Condition> conditions = cf.getConditionFacts()
                  .stream()
                  .map(condF -> telemetryCollector.formatConditionForTelemetryData(condF,
                      cf.getOperatorName()))
                  .collect(Collectors.toList());
              return telemetryCollector.formatConstraintForTelemetryData(cf, conditions);
            })
        .toList();
  }

  private PolicyViolationTelemetryCollector createTelemetryCollector(boolean isScmEnabled) {
    ComponentHelper componentHelper = new ComponentHelper(null)
    {
      @Override
      public boolean isGoldenVersion(ComponentIdentifier toVersion, String appId) {
        return true;
      }
    };

    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(
        policyWaiverDAO, sourceControlEventDAO, telemetryUtils, licenseNameProvider, isScmEnabled, componentHelper);

    telemetryCollector.setTimeOfPolicyEvaluation(policyEvaluation.getTime());
    return telemetryCollector;
  }

  private List<Component> createWrappedComponent(
      ComponentIdentifier componentIdentifier,
      boolean isDirectDependency,
      boolean isInnersource)
  {
    Component component = new Component(componentIdentifier);
    component.setDirectDependency(isDirectDependency);
    component.setInnerSourceData(isInnersource ? Collections.singleton(new InnerSourceData()) : null);
    return List.of(component);
  }

  /**
   * Helper class to create the policy violation and component data needed for the tests and the validations against
   * that data.
   */
  private static class TestablePolicyViolation
  {
    private final List<Component> additionalVersions = new ArrayList<>();

    private final List<Component> components = new ArrayList<>();

    private final PolicyViolation policyViolation;

    private PolicyViolation replacementPolicyViolation;

    private Component component;

    private String conditionType;

    private int count = 1;

    private String cveIdentifier;

    private Double cvssScore;

    private boolean expectUnwaived;

    private String fixReason;

    private Long fixTime;

    private boolean isScmEnabled;

    private String licenseThreatGroup;

    private String licensesDeclared;

    private String licensesEffective;

    private String licensesObserved;

    private String licensesOverrideStatus;

    private String waiverId;

    private String autoPolicyWaiverId;

    private Long openTime;

    private String waiverExpiration;

    private PurlIdentifiersWithVulnerabilities reachablePurlIdentifiersWithVulnerabilities;

    // Expected PR attribution fields for TIME_TO_CHANGE_VERSION telemetry
    private Integer expectedPullRequestNumber;

    private String expectedPullRequestRemediationVersion;

    private Boolean expectedPullRequestIsGolden;

    // Expected remediation version for TIME_TO_CHANGE_VERSION telemetry
    private String expectedRemediationVersion;

    // Expected isRemediatedByVersionChange flag for TIME_TO_CHANGE_VERSION telemetry
    private Boolean expectedIsRemediatedByVersionChange;

    TestablePolicyViolation(ComponentIdentifier componentIdentifier, String policyName) {
      this.component = new Component(componentIdentifier);
      this.components.add(component);
      this.policyViolation = new PolicyViolation();
      policyViolation.setComponentIdentifier(componentIdentifier);
      policyViolation.setPolicyName(policyName);
      policyViolation.setPolicyId(createPolicyId(policyName));
      policyViolation.setOwnerId(policyEvaluation.getOwnerId());
      policyViolation.setStageTypeId(policyEvaluation.getStageTypeId());
      policyViolation.setOpenTime(policyEvaluation.getTime());
    }

    static TestablePolicyViolation createDefaultSecurityViolationForComponent(ComponentIdentifier componentIdentifier) {
      return createMinimalViolationForComponent(componentIdentifier)
          .withPolicyViolationId("conditionTypeViolation")
          .withThreatCategory(PolicyThreatCategory.SECURITY)
          .withThreatLevel(7)
          .withCveAndCvssScore("CVE-123", 7.5, 1);
    }

    static TestablePolicyViolation createDefaultLicenseViolationForComponent(ComponentIdentifier componentIdentifier) {
      return createMinimalViolationForComponent(componentIdentifier)
          .withPolicyViolationId("conditionTypeViolation")
          .withThreatCategory(PolicyThreatCategory.LICENSE)
          .withLicenseThreatGroup("GPL")
          .withThreatLevel(8);
    }

    static TestablePolicyViolation createMinimalViolationForComponent(ComponentIdentifier componentIdentifier) {
      return new TestablePolicyViolation(componentIdentifier, TEST_POLICY);
    }

    private long calculateExpectedFixTime() {
      return calculateTimeDifference(getOpenTime(), policyViolation.getFixTime());
    }

    private long calculateExpectedLegacyTime() {
      return calculateTimeDifference(getOpenTime(), policyViolation.getLegacyViolationTime());
    }

    private long calculateExpectedUnwaiveTime() {
      return calculateTimeDifference(getOpenTime(), policyViolation.getWaiveTime());
    }

    private long calculateTimeDifference(long startTime, Date endTime) {
      if (null == endTime) {
        endTime = policyEvaluation.getTime();
      }
      return endTime.getTime() - startTime;
    }

    List<Component> getAdditionalVersions() {
      return additionalVersions;
    }

    Component getComponent() {
      return component;
    }

    List<Component> getComponents() {
      return components;
    }

    String getConditionType() {
      return conditionType;
    }

    String getWaiverId() {
      return waiverId;
    }

    String getAutoPolicyWaiverId() {
      return autoPolicyWaiverId;
    }

    long getOpenTime() {
      return null != openTime ? openTime : policyEvaluation.getTime().getTime();
    }

    PolicyViolation getPolicyViolation() {
      return PolicyViolationTestUtils.copyPolicyViolation(policyViolation);
    }

    boolean isScmEnabled() {
      return isScmEnabled;
    }

    private long msForHours(int hours) {
      return 1000L * 60 * 60 * hours;
    }

    TestablePolicyViolation asDirectDependency(boolean isDirectDependency) {
      component.setDirectDependency(isDirectDependency);
      return this;
    }

    TestablePolicyViolation asInnerSourceDependency(boolean isInnerSource) {
      component.setInnerSourceData(isInnerSource ? Collections.singleton(new InnerSourceData()) : null);
      return this;
    }

    TestablePolicyViolation markFixedAsLegacy() {
      this.fixTime = policyEvaluation.getTime().getTime();
      policyViolation.setFixTime(new Date(this.fixTime));
      return this;
    }

    TestablePolicyViolation markFixedByDowngrade() {
      this.fixTime = policyEvaluation.getTime().getTime();
      this.fixReason = COMPONENT_DOWNGRADE;
      policyViolation.setFixTime(new Date(this.fixTime));
      return this;
    }

    TestablePolicyViolation markFixedByOtherMeans() {
      this.fixTime = policyEvaluation.getTime().getTime();
      policyViolation.setFixTime(new Date(this.fixTime));
      return this;
    }

    TestablePolicyViolation markFixedByLicenseOverride(
        String licenseOverrides,
        LicenseOverrideStatus overrideStatus)
    {
      this.fixTime = policyEvaluation.getTime().getTime();
      this.licensesEffective = licenseOverrides;
      this.licensesOverrideStatus = overrideStatus.getName();
      component.setLicenseOverrideIds(toLicenseSet(licenseOverrides));
      component.setLicenseOverrideStatus(overrideStatus);
      policyViolation.setFixTime(new Date(this.fixTime));
      return this;
    }

    TestablePolicyViolation markFixedByRemoval() {
      this.fixTime = policyEvaluation.getTime().getTime();
      component = null;
      this.components.clear();
      policyViolation.setFixTime(new Date(this.fixTime));
      return this;
    }

    TestablePolicyViolation markFixedByUpgrade() {
      this.fixTime = policyEvaluation.getTime().getTime();
      this.fixReason = COMPONENT_UPGRADE;
      policyViolation.setFixTime(new Date(this.fixTime));
      return this;
    }

    TestablePolicyViolation markUnwaived(String oldWaiverId, PolicyViolation replacementPolicyViolation) {
      this.expectUnwaived = true;
      this.waiverId = oldWaiverId;
      this.replacementPolicyViolation = replacementPolicyViolation;
      policyViolation.setPolicyWaiverId(oldWaiverId);
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      withCount(-1);
      return this;
    }

    TestablePolicyViolation markWaived(PolicyWaiver policyWaiver) {
      policyViolation.setPolicyWaiverId(policyWaiver.getId());
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      waiverId = policyWaiver.getId();
      waiverExpiration = "never";
      return this;
    }

    TestablePolicyViolation markUnAutoWaived(String oldAutoPolicyWaiverId, PolicyViolation replacementPolicyViolation) {
      this.expectUnwaived = true;
      this.autoPolicyWaiverId = oldAutoPolicyWaiverId;
      this.replacementPolicyViolation = replacementPolicyViolation;
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      policyViolation.setAutoPolicyWaiverId(oldAutoPolicyWaiverId);
      withCount(-1);
      return this;
    }

    TestablePolicyViolation markAutoWaived(AutoPolicyWaiver autoPolicyWaiver) {
      policyViolation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      autoPolicyWaiverId = autoPolicyWaiver.getId();
      waiverExpiration = "never";
      return this;
    }

    TestablePolicyViolation openedHoursAgo(int hours) {
      this.openTime = policyEvaluation.getTime().getTime() - msForHours(hours);
      policyViolation.setOpenTime(new Date(openTime));
      return this;
    }

    TestablePolicyViolation withAdditionalComponentVersion(
        ComponentIdentifier componentIdentifier,
        boolean isDirect,
        boolean isInnerSource)
    {
      Component anotherVersion = new Component(componentIdentifier);
      anotherVersion.setDirectDependency(isDirect);
      anotherVersion.setInnerSourceData(isInnerSource ? Collections.singleton(new InnerSourceData()) : null);
      additionalVersions.add(anotherVersion);

      return this;
    }

    TestablePolicyViolation withConditionType(String conditionType) {
      this.conditionType = conditionType;
      return this;
    }

    TestablePolicyViolation withGenericConditionType(String conditionType) {
      this.conditionType = conditionType;
      // Create constraint facts with the specified condition type (no special CVE or license metadata)
      policyViolation.setConstraintFacts(
          ConditionGenerator.createConstraintFactsWithGenericConditionType(conditionType));
      return this;
    }

    TestablePolicyViolation withCount(int count) {
      this.count = count;
      return this;
    }

    TestablePolicyViolation withCveAndCvssScore(String cveIdentifier, double cvssScore, int index) {
      this.cveIdentifier = cveIdentifier;
      this.cvssScore = cvssScore;

      policyViolation.setConstraintFacts(
          ConditionGenerator.creatConstraintFactsWithInjectedSecurityCondition(cveIdentifier, cvssScore, index));
      return this;
    }

    TestablePolicyViolation withLicenseThreatGroup(String licenseThreatGroup) {
      this.licenseThreatGroup = licenseThreatGroup;
      this.conditionType = LicenseThreatGroupConditionType.ID;
      policyViolation.setConstraintFacts(
          ConditionGenerator.createConstraintFactsWithInjectedLicenseCondition(licenseThreatGroup, 1));
      return this;
    }

    TestablePolicyViolation withLicenses(
        String declared,
        String declaredMulti,
        String observed,
        String observedMulti)
    {
      this.licensesDeclared = String.join(", ", declaredMulti, declared);
      this.licensesObserved = String.join(", ", observedMulti, observed);
      // licensesEffective is the combination of all licenses when there's no override
      this.licensesEffective = String.join(", ", declaredMulti, declared, observedMulti, observed);
      // Set override status to Open (default for components with licenses set)
      this.licensesOverrideStatus = "Open";
      this.component.setDeclaredLicenseIds(toLicenseSet(declared));
      this.component.setDeclaredMultiLicenseIds(toLicenseSet(declaredMulti));
      this.component.setObservedLicenseIds(toLicenseSet(observed));
      this.component.setObservedMultiLicenseIds(toLicenseSet(observedMulti));
      this.component.setLicenseOverrideStatus(LicenseOverrideStatus.OPEN);
      return this;
    }

    TestablePolicyViolation withPolicyViolationId(String policyViolationId) {
      policyViolation.setId(policyViolationId);
      return this;
    }

    TestablePolicyViolation withScmEnabled(boolean scmEnabled) {
      this.isScmEnabled = scmEnabled;
      return this;
    }

    TestablePolicyViolation withThreatLevel(int threatLevel) {
      policyViolation.setThreatLevel(threatLevel);
      return this;
    }

    TestablePolicyViolation withThreatCategory(PolicyThreatCategory threatCategory) {
      policyViolation.setThreatCategory(threatCategory);
      return this;
    }

    TestablePolicyViolation withPurlIdentifiersWithVulnerabilities(
        final PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities)
    {
      this.reachablePurlIdentifiersWithVulnerabilities = purlIdentifiersWithVulnerabilities;
      return this;
    }

    TestablePolicyViolation withExpectedRemediationVersion(String remediationVersion) {
      this.expectedRemediationVersion = remediationVersion;
      return this;
    }

    TestablePolicyViolation withExpectedRemediationAttribution(
        int pullRequestNumber,
        String remediationVersion,
        boolean isGolden)
    {
      this.expectedPullRequestNumber = pullRequestNumber;
      this.expectedPullRequestRemediationVersion = remediationVersion;
      this.expectedPullRequestIsGolden = isGolden;
      return this;
    }

    TestablePolicyViolation withIsRemediatedByVersionChange(Boolean isRemediatedByVersionChange) {
      this.expectedIsRemediatedByVersionChange = isRemediatedByVersionChange;
      policyViolation.setIsRemediatedByVersionChange(isRemediatedByVersionChange);
      return this;
    }

    private String createPolicyId(String policyName) {
      return "ID_" + policyName;
    }

    void validateTelemetryDataForPurposes(List<TelemetryData> telemetryDataList, TelemetryPurpose... purposeCodes) {
      assertThat(telemetryDataList).hasSize(purposeCodes.length);
      for (int i = 0; i < telemetryDataList.size(); i++) {
        TelemetryData telemetryData = telemetryDataList.get(i);
        TelemetryPurpose purpose = purposeCodes[i];
        assertThat(telemetryData.getPurpose()).isEqualTo(purpose);

        validateCommonAttributes(telemetryData);
        validateComponentInfo(telemetryData.getAttributes());
        validateDependencyInfo(telemetryData.getAttributes());
        validatePurposeSpecificAttributes(telemetryData.getAttributes(), purpose);
      }
    }

    void validateCommonAttributes(TelemetryData telemetryData) {
      Map<String, Object> attributes = telemetryData.getAttributes();

      // the application ID is obfuscated
      assertThat(attributes).doesNotContainEntry(APPLICATION_ID, policyEvaluation.getOwnerId());

      assertThat(attributes).containsEntry(COUNT, count);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.CVE_NUMBER, cveIdentifier);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.CVSS_SCORE, cvssScore);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.LICENSE_THREAT_GROUP_ATTRIBUTE,
          licenseThreatGroup);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.LICENSES_DECLARED, licensesDeclared);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.LICENSES_EFFECTIVE, licensesEffective);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.LICENSES_OBSERVED, licensesObserved);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.LICENSES_OVERRIDE_STATUS,
          licensesOverrideStatus);
      assertThat(attributes).containsEntry(IS_SCM_ENABLED, isScmEnabled);
      assertThat(attributes).containsEntry(OPEN_TIME, getOpenTime());
      assertThat(attributes).containsEntry(POLICY_VIOLATION_ID, policyViolation.getId());
      assertThat(attributes).containsEntry(REAL_APPLICATION_ID, policyEvaluation.getOwnerId());
      assertThat(attributes).containsEntry(STAGE, policyEvaluation.getStageTypeId());
      assertThat(attributes).containsEntry(THREAT_LEVEL, policyViolation.getThreatLevel());
      assertThat(attributes).containsEntry(THREAT_CATEGORY, policyViolation.getThreatCategory().getName());

      ReachabilityStatus reachabilityStatus = policyViolation.getReachabilityStatus();
      assertThat(attributes)
          .containsEntry(REACHABILITY_STATUS, reachabilityStatus == null ? null : reachabilityStatus.getName());
    }

    void validateComponentInfo(Map<String, Object> attributes) {
      assertThat(attributes).containsEntry(ECOSYSTEM, policyViolation.getComponentIdentifier().getFormat());
      assertThat(attributes).containsEntry(COMPONENT_IDENTIFIER, policyViolation.getComponentIdentifier().toString());

      PackageUrlIdentifier packageUrlIdentifier =
          PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier());

      assertThat(attributes).containsEntry(COMPONENT_NAMESPACE, packageUrlIdentifier.getNamespace());
      assertThat(attributes).containsEntry(COMPONENT_NAME, packageUrlIdentifier.getName());
      assertThat(attributes).containsEntry(COMPONENT_VERSION, packageUrlIdentifier.getVersion());
    }

    void validateDependencyInfo(Map<String, Object> attributes) {
      if (null != component) {
        assertThat(attributes).containsEntry(DIRECT_DEPENDENCY, component.getDirectDependency());
        assertThat(attributes).containsEntry(INNERSOURCE_DEPENDENCY, component.getInnerSourceData() != null);
      }
    }

    void validateMatchesOrNotExists(Map<String, Object> attributes, String key, Object value) {
      if (null != value) {
        assertThat(attributes).containsEntry(key, value);
      }
      else {
        assertThat(attributes).doesNotContainKey(key);
      }
    }

    void validatePurposeSpecificAttributes(Map<String, Object> attributes, TelemetryPurpose purpose) {
      switch (purpose) {
        case CONDITION_TYPE_VIOLATION:
          validateTimeAttribute(attributes, 0L);
          validateConstraintsData(attributes);
          break;

        case CONDITION_TYPE_VIOLATION_AUDIT:
          validateTimeAttribute(attributes, 0L);
          validateConstraintsData(attributes);
          break;

        case TIME_TO_CHANGE_VERSION_POLICY_VIOLATION:
          validateMatchesOrNotExists(attributes, FIX_TIME, fixTime);
          validateMatchesOrNotExists(attributes, FIX_BY_VERSION_CHANGE, fixReason);
          // is_remediated_by_version_change is always present in telemetry (even with null value)
          assertThat(attributes).containsEntry(REMEDIATION_BY_VERSION_CHANGE, expectedIsRemediatedByVersionChange);
          // Assert remediation version if expected
          if (expectedRemediationVersion != null) {
            assertThat(attributes).containsEntry(REMEDIATION_VERSION, expectedRemediationVersion);
          }
          // New attributes for PR attribution
          if (expectedPullRequestNumber != null) {
            assertThat(attributes).containsEntry(PULL_REQUEST_NUMBER, expectedPullRequestNumber);
            assertThat(attributes).containsEntry(PULL_REQUEST_REMEDIATION_VERSION,
                expectedPullRequestRemediationVersion);
            assertThat(attributes).containsEntry(PULL_REQUEST_IS_GOLDEN, expectedPullRequestIsGolden);
          }
          else {
            assertThat(attributes).doesNotContainKeys(PULL_REQUEST_NUMBER, PULL_REQUEST_REMEDIATION_VERSION,
                PULL_REQUEST_IS_GOLDEN);
          }
          validateTimeAttribute(attributes, calculateExpectedFixTime());
          break;

        case TIME_TO_LEGACY_VIOLATION:
          validateMatchesOrNotExists(attributes, LEGACY_VIOLATION_TIME, fixTime);
          validateTimeAttribute(attributes, calculateExpectedLegacyTime());
          break;

        case TIME_TO_REMEDIATE_POLICY_VIOLATION:
          validateMatchesOrNotExists(attributes, FIX_TIME, fixTime);
          validateTimeAttribute(attributes, calculateExpectedFixTime());
          // Validate waiver audit fields if present
          if (attributes.containsKey(FROM_WAIVED_STATUS)) {
            if (Boolean.TRUE.equals(attributes.get(FROM_WAIVED_STATUS))) {
              assertThat(attributes).containsKey(WAIVER_AUDIT_INFO);
            }
          }
          break;

        case TIME_TO_WAIVE_POLICY_VIOLATION:
          validateMatchesOrNotExists(attributes, WAIVE_TIME, policyViolation.getWaiveTime().getTime());
          validateMatchesOrNotExists(attributes, POLICY_WAIVER_ID, getWaiverId());
          validateMatchesOrNotExists(attributes, AUTO_POLICY_WAIVER_ID, getAutoPolicyWaiverId());
          validateMatchesOrNotExists(attributes, WAIVER_EXPIRATION, waiverExpiration);
          if (expectUnwaived) {
            validateMatchesOrNotExists(attributes, UNWAIVE_TIME, policyViolation.getWaiveTime().getTime());
            validateMatchesOrNotExists(attributes, NEW_POLICY_VIOLATION_ID, replacementPolicyViolation.getId());
          }
          validateTimeAttribute(attributes, calculateExpectedUnwaiveTime());
          break;

        case CALLFLOW_EVALUATION_COMPONENT_COUNTS:
          boolean evaluationSuccessful = reachablePurlIdentifiersWithVulnerabilities != null;
          validateMatchesOrNotExists(attributes, CALL_FLOW_EVALUATION_SUCCESSFUL, evaluationSuccessful);
          validateMatchesOrNotExists(attributes, CALL_FLOW_HAS_REACHABLE_INFORMATION_FOR_COMPONENT,
              hasPolicyViolationByComponentIdentifier(policyViolation, reachablePurlIdentifiersWithVulnerabilities));
          break;

        default:
          throw new IllegalArgumentException("Unexpected purpose: " + purpose);
      }
    }

    void validateTimeAttribute(Map<String, Object> attributes, long expectedTime) {
      assertThat(attributes).containsEntry(TIME, expectedTime);
    }

    private Set<String> toLicenseSet(String licenses) {
      return new LinkedHashSet<>(Arrays.asList(licenses.split(", ")));
    }

    private void validateConstraintsData(Map<String, Object> attributes) {
      assertThat(attributes).containsKey(POLICY_CONSTRAINTS);

      @SuppressWarnings("unchecked")
      List<Constraint> constraints = (List<Constraint>) attributes
          .get(POLICY_CONSTRAINTS);
      assertThat(constraints).hasSize(3);

      // Validate each constraint has required properties
      for (int i = 0; i < constraints.size(); i++) {
        Constraint constraint = constraints.get(i);
        assertThat(constraint.getId()).isEqualTo("constraintId" + i);
        assertThat(constraint.getName()).isEqualTo("constraintName" + i);
        assertThat(constraint.getOperator()).isEqualTo(LogicalOperator.AND);
        assertThat(constraint.getConditions()).hasSize(3);

        List<Condition> conditions = constraint.getConditions();
        // Validate each condition within the constraint
        for (int j = 0; j < conditions.size(); j++) {
          Condition condition = conditions.get(j);
          assertThat(condition.getConditionTypeId()).isEqualTo(conditionType);
          assertThat(condition.getConditionIndex()).isEqualTo(j);
          assertThat(condition.getOperator()).isEqualTo("operatorName" + i);
        }
      }

      // Only check CVE value for security violations
      if (cveIdentifier != null) {
        assertThat(constraints.get(1).getConditions().get(1).getValue()).isEqualTo("CVE-123");
      }
    }
  }

  /**
   * Helper class to generate the constraint facts with the condition facts needed for the tests.
   */
  private class ConditionGenerator
  {
    static List<ConstraintFact> createConstraintFactsWithInjectedLicenseCondition(
        String licenseThreatGroup,
        int cvIteration)
    {
      return createConstraintFactsWithInjectedCondition(licenseThreatGroup, null, null, cvIteration);
    }

    static List<ConstraintFact> creatConstraintFactsWithInjectedSecurityCondition(
        String cveNumber,
        Double cvssScore,
        int cvIteration)
    {
      return createConstraintFactsWithInjectedCondition(null, cveNumber, cvssScore, cvIteration);
    }

    static List<ConstraintFact> createConstraintFactsWithGenericConditionType(String conditionType) {
      List<ConstraintFact> constraintFacts = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        ConstraintFact constraintFact =
            new ConstraintFact("constraintId" + i, "constraintName" + i, "operatorName" + i);
        constraintFacts.add(constraintFact);
        for (int j = 0; j < 3; j++) {
          ConditionFact conditionFact = createConditionFact(j, conditionType);
          constraintFact.addConditionFact(conditionFact);
        }
      }
      return constraintFacts;
    }

    /**
     * A useful constraint fact must have at least 1 condition facts because that's where the test data is. Several are
     * created because in real word scenarios there may be multiple constraint facts with one condition fact nested with
     * needed data. The number of constraint and condition facts is fixed. Only one condition fact with the cv metadata
     * is instantiated in the whole list of constraint facts, it is possible to choose where. Hardcoded values are not
     * important for these tests.
     *
     * @param cveNumber CVE id to inject in the condition fact
     * @param cvssScore Score to inject in the condition fact
     * @param licenseThreatGroup use for license thread conditions; mutually exclusive with the cve params above
     * @param cvIteration Iteration you want to insert the cv metadata
     * @return A list of constraint fact with the same amount of condition fact that contain the cv metadata
     */
    private static List<ConstraintFact> createConstraintFactsWithInjectedCondition(
        String licenseThreatGroup,
        String cveNumber,
        Double cvssScore,
        int cvIteration)
    {
      // Determine the default condition type based on what kind of condition is being created
      String defaultConditionType = StringUtils.isNotBlank(licenseThreatGroup)
          ? LicenseThreatGroupConditionType.ID
          : SecurityVulnerabilitySeverityConditionType.ID;

      List<ConstraintFact> constraintFacts = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        ConstraintFact constraintFact =
            new ConstraintFact("constraintId" + i, "constraintName" + i, "operatorName" + i);
        constraintFacts.add(constraintFact);
        for (int j = 0; j < 3; j++) {
          ConditionFact conditionFact;

          if (cveNumber != null && cvssScore != null && cvIteration == i && cvIteration == j) {
            conditionFact = createConditionFactWithCVMetadata(j, cveNumber, cvssScore);
          }
          else if (StringUtils.isNotBlank(licenseThreatGroup) && cvIteration == i && cvIteration == j) {
            conditionFact = createConditionFactForLicenseThreat(j, licenseThreatGroup);
          }
          else {
            conditionFact = createConditionFact(j, defaultConditionType);
          }

          constraintFact.addConditionFact(conditionFact);
        }
      }
      return constraintFacts;
    }

    private static ConditionFact createConditionFactWithCVMetadata(int j, String cveNumber, double cvssScore) {
      TriggerReference triggerReference =
          new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, cveNumber);
      String triggerJson =
          String.format("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"%s\",\"severity\":%f}}", cveNumber, cvssScore);
      ConditionFact conditionFact =
          new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, j, "summary", "reason", triggerReference);
      conditionFact.setTriggerJson(triggerJson);
      return conditionFact;
    }

    private static ConditionFact createConditionFactForLicenseThreat(int j, String licenseThreatGroup) {
      var triggerJson = String.format("{\"conditionIndex\":0,\"trigger\":{\"id\":\"%s\"}}", "threatGroupId");
      var summary = String.format("License Threat Group is '%s'", licenseThreatGroup);
      ConditionFact conditionFact =
          new ConditionFact(LicenseThreatGroupConditionType.ID, j, summary, "reason", null);
      conditionFact.setTriggerJson(triggerJson);
      return conditionFact;
    }

    private static ConditionFact createConditionFact(int j, String conditionType) {
      return new ConditionFact(conditionType, j, "summary", "reason");
    }
  }

  @Test
  public void testAddTelemetryForFixedViolation_WithWaiverAuditInfo() {
    // given a policy violation that was waived then fixed
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(jacksonDatabind_2_13_4)
            .openedHoursAgo(48)
            .withPolicyViolationId("waivedThenFixed");

    // Create a policy waiver
    Policy policy = tempEntity.newPolicy();
    Application app = tempEntity.newApplicationWithParent();
    Date waiverCreateTime = new Date(policyEvaluation.getTime().getTime() - 24 * 60 * 60 * 1000); // 24 hours ago
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        null, // hash
        policy.getId(),
        app.getId(),
        "Test waiver comment",
        null, // expiryTime
        null, // constraintFacts
        "REASON_123" // policyWaiverReasonId
    );
    policyWaiver.setCreateTime(waiverCreateTime);
    policyWaiverDAO.updateWithNoChecks(policyWaiver);

    Date waiveTime = new Date(policyEvaluation.getTime().getTime() - 12 * 60 * 60 * 1000);
    testablePolicyViolation.policyViolation.setPolicyWaiverId(policyWaiver.getId());
    testablePolicyViolation.policyViolation.setPolicyWaiverComment("Test waiver comment");
    testablePolicyViolation.policyViolation.setWaiveTime(waiveTime);

    testablePolicyViolation.markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);

    TelemetryData data = telemetryData.get(0);
    assertThat(data.getPurpose()).isEqualTo(TIME_TO_REMEDIATE_POLICY_VIOLATION);

    Map<String, Object> attributes = data.getAttributes();
    assertThat(attributes).containsEntry(FROM_WAIVED_STATUS, true);
    assertThat(attributes).containsKey(WAIVER_AUDIT_INFO);

    @SuppressWarnings("unchecked")
    Map<String, Object> waiverAuditInfo = (Map<String, Object>) attributes.get(WAIVER_AUDIT_INFO);
    assertThat(waiverAuditInfo).containsEntry(ORIGINAL_WAIVER_REASON, "REASON_123");
    assertThat(waiverAuditInfo).containsEntry(ORIGINAL_WAIVER_COMMENT, "Test waiver comment");
    assertThat(waiverAuditInfo).containsEntry(ORIGINAL_WAIVER_DATE, waiveTime.getTime());
    assertThat(waiverAuditInfo).containsEntry(WAIVER_ID, policyWaiver.getId());
  }

  @Test
  public void testAddTelemetryForFixedViolation_NotFromWaived() {
    // given a policy violation that was never waived, just fixed
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(jacksonDatabind_2_13_4)
            .openedHoursAgo(48)
            .withPolicyViolationId("neverWaived")
            .markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents());

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);

    TelemetryData data = telemetryData.get(0);
    Map<String, Object> attributes = data.getAttributes();
    assertThat(attributes).containsEntry(FROM_WAIVED_STATUS, false);
    assertThat(attributes).doesNotContainKey(WAIVER_AUDIT_INFO);
  }

  // EI-1096: Tests for unwaive detection and previous waiver info
  @Test
  public void testAddTelemetryForUnwaivedViolation_IncludesPreviousWaiverInfoForManualWaiver() {
    // given: A policy violation with manual waiver that has reason and comment
    Policy policy = tempEntity.newPolicy();
    Date waiverCreateTime = new Date(policyEvaluation.getTime().getTime() - 48 * 60 * 60 * 1000); // 48 hours ago
    PolicyWaiver policyWaiver = tempEntity.newWaiver(
        null, // hash
        policy.getId(),
        policyEvaluation.getOwnerId(),
        "Manual waiver comment for testing",
        null, // expiryTime
        null, // constraintFacts
        "SECURITY_REVIEW_REASON" // policyWaiverReasonId
    );
    policyWaiver.setCreateTime(waiverCreateTime);
    policyWaiverDAO.updateWithNoChecks(policyWaiver);

    var replacementPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(0)
        .asDirectDependency(true)
        .withPolicyViolationId("newViolationAfterUnwaived")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    var unwaivedPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(500)
        .asDirectDependency(true)
        .withPolicyViolationId("unwaivedViolation")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID)
        .markWaived(policyWaiver)
        .markUnwaived(policyWaiver.getId(), replacementPolicyViolation.getPolicyViolation());

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(unwaivedPolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForUnwaivedViolation(
        unwaivedPolicyViolation.getPolicyViolation(),
        replacementPolicyViolation.getPolicyViolation(),
        unwaivedPolicyViolation.getComponent());

    // then: Telemetry should include previous_waiver_info with all fields populated
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);

    TelemetryData data = telemetryData.get(0);
    Map<String, Object> attributes = data.getAttributes();
    assertThat(attributes).containsKey(PREVIOUS_WAIVER_INFO);

    @SuppressWarnings("unchecked")
    Map<String, Object> previousWaiverInfo = (Map<String, Object>) attributes.get(PREVIOUS_WAIVER_INFO);
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_REASON, "SECURITY_REVIEW_REASON");
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_COMMENT, "Manual waiver comment for testing");
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_DATE, policyEvaluation.getTime().getTime());
  }

  @Test
  public void testAddTelemetryForUnwaivedViolation_PreviousWaiverInfoWithNullsForAutoWaiver() {
    // given: A policy violation that was auto-waived and then unwaived
    var autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(policyEvaluation.getOwnerId());

    var replacementPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(0)
        .asDirectDependency(true)
        .withPolicyViolationId("newViolationAfterUnwaived")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    var unwaivedPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(500)
        .asDirectDependency(true)
        .withPolicyViolationId("unwaivedViolation")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID)
        .markAutoWaived(autoPolicyWaiver)
        .markUnAutoWaived(autoPolicyWaiver.getId(), replacementPolicyViolation.getPolicyViolation());

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(unwaivedPolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForUnwaivedViolation(
        unwaivedPolicyViolation.getPolicyViolation(),
        replacementPolicyViolation.getPolicyViolation(),
        unwaivedPolicyViolation.getComponent());

    // then: Telemetry should include previous_waiver_info with null reason/comment but valid date
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);

    TelemetryData data = telemetryData.get(0);
    Map<String, Object> attributes = data.getAttributes();
    assertThat(attributes).containsKey(PREVIOUS_WAIVER_INFO);

    @SuppressWarnings("unchecked")
    Map<String, Object> previousWaiverInfo = (Map<String, Object>) attributes.get(PREVIOUS_WAIVER_INFO);
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_REASON, null);
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_COMMENT, null);
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_DATE, policyEvaluation.getTime().getTime());
  }

  @Test
  public void testAddTelemetryForUnwaivedViolation_PreviousWaiverInfoWithNullsWhenPolicyWaiverNotFound() {
    // given: A policy violation with a policyWaiverId that doesn't exist in database
    var replacementPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(0)
        .asDirectDependency(true)
        .withPolicyViolationId("newViolationAfterUnwaived")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    var unwaivedPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(500)
        .asDirectDependency(true)
        .withPolicyViolationId("unwaivedViolation")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    // Set a non-existent policyWaiverId and markUnwaived will set waiveTime to policyEvaluation.getTime()
    unwaivedPolicyViolation.markUnwaived("NON_EXISTENT_WAIVER_ID", replacementPolicyViolation.getPolicyViolation());

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(unwaivedPolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForUnwaivedViolation(
        unwaivedPolicyViolation.getPolicyViolation(),
        replacementPolicyViolation.getPolicyViolation(),
        unwaivedPolicyViolation.getComponent());

    // then: Telemetry should include previous_waiver_info with null values for missing waiver
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);

    TelemetryData data = telemetryData.get(0);
    Map<String, Object> attributes = data.getAttributes();
    assertThat(attributes).containsKey(PREVIOUS_WAIVER_INFO);

    @SuppressWarnings("unchecked")
    Map<String, Object> previousWaiverInfo = (Map<String, Object>) attributes.get(PREVIOUS_WAIVER_INFO);
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_REASON, null);
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_COMMENT, null);
    assertThat(previousWaiverInfo).containsEntry(ORIGINAL_WAIVER_DATE, policyEvaluation.getTime().getTime());
  }

  @Test
  public void testAddTelemetryForWaivedViolation_DoesNotIncludeUnwaiveFields() {
    // given: A regular waived policy violation (not unwaived)
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyEvaluation.getOwnerId());

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(5)
            .asDirectDependency(true)
            .withPolicyViolationId("waivedViolation")
            .markWaived(policyWaiver);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForWaivedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then: Telemetry should NOT include previous_waiver_info
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);

    TelemetryData data = telemetryData.get(0);
    assertThat(data.getPurpose()).isEqualTo(TIME_TO_WAIVE_POLICY_VIOLATION);

    Map<String, Object> attributes = data.getAttributes();
    assertThat(attributes).doesNotContainKey(PREVIOUS_WAIVER_INFO);
  }

  @Test
  public void testAddTelemetryForAutoWaivedViolation_DoesNotIncludeUnwaiveFields() {
    // given: A regular auto-waived policy violation (not unwaived)
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(policyEvaluation.getOwnerId());

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(5)
            .asDirectDependency(true)
            .withPolicyViolationId("autoWaivedViolation")
            .markAutoWaived(autoPolicyWaiver);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForAutoWaivedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent());

    // then: Telemetry should NOT include previous_waiver_info
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    assertThat(telemetryData).hasSize(1);

    TelemetryData data = telemetryData.get(0);
    assertThat(data.getPurpose()).isEqualTo(TIME_TO_WAIVE_POLICY_VIOLATION);

    Map<String, Object> attributes = data.getAttributes();
    assertThat(attributes).doesNotContainKey(PREVIOUS_WAIVER_INFO);
  }

  /**
   * Helper class used to isolate the policy violation data to ensure that the telemetry collector can't inadvertently
   * modify the reference data.
   */
  private class PolicyViolationTestUtils
  {
    static PolicyViolation copyPolicyViolation(PolicyViolation original) {
      PolicyViolation copy = new PolicyViolation();

      copy.setOwnerId(original.getOwnerId());
      copy.setAutoPolicyWaiverId(original.getAutoPolicyWaiverId());
      copy.setComponentIdentifier(original.getComponentIdentifier());
      copy.setConstraintFacts(
          null != original.getConstraintFacts() ? new ArrayList<>(original.getConstraintFacts()) : new ArrayList<>());
      copy.setFilename(original.getFilename());
      copy.setFixTime(original.getFixTime());
      copy.setHash(original.getHash());
      copy.setId(original.getId());
      copy.setLegacyViolationApplied(original.isLegacyViolationApplied());
      copy.setLegacyViolationTime(original.getLegacyViolationTime());
      copy.setOpenTime(original.getOpenTime());
      copy.setPolicyId(original.getPolicyId());
      copy.setPolicyName(original.getPolicyName());
      copy.setPolicyWaiverId(original.getPolicyWaiverId());
      copy.setAutoPolicyWaiverId(original.getAutoPolicyWaiverId());
      copy.setReachabilityStatus(original.getReachabilityStatus());
      copy.setSeenByMonitoringEvaluation(original.isSeenByMonitoringEvaluation());
      copy.setSeenByPrimaryEvaluation(original.isSeenByPrimaryEvaluation());
      copy.setStageTypeId(original.getStageTypeId());
      copy.setThreatCategory(original.getThreatCategory());
      copy.setThreatLevel(original.getThreatLevel());
      copy.setWaiveTime(original.getWaiveTime());
      copy.setReachabilityStatus(original.getReachabilityStatus());
      copy.setIsRemediatedByVersionChange(original.getIsRemediatedByVersionChange());
      copy.setLastTelemetryEmittedDate(original.getLastTelemetryEmittedDate());

      return copy;
    }
  }
}
