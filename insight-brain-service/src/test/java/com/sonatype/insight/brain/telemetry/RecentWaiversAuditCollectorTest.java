/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RecentWaiversAuditCollector.
 *
 * @since 205
 */
public class RecentWaiversAuditCollectorTest
    extends AbstractComponentTest
{
  private static final String TEST_APP_PUBLIC_ID = "testApp";

  private static final String TEST_STAGE = "build";

  private static final ComponentIdentifier jacksonDatabind = ComponentIdentifier.createMavenCoordinates(
      "com.fasterxml.jackson.core", "jackson-databind", "2.13.4");

  private String testAppId;

  private RecentWaiversAuditCollector collector;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

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
    super.beforeTest();

    // Create application once with parent organization to avoid foreign key constraint violations
    testAppId = tempEntity.newApplicationWithParent(TEST_APP_PUBLIC_ID).getId();

    ComponentHelper componentHelper = new ComponentHelper(null)
    {
      @Override
      public boolean isGoldenVersion(ComponentIdentifier toVersion, String appId) {
        return false;
      }
    };

    collector = new RecentWaiversAuditCollector(
        policyViolationDAO,
        policyWaiverDAO,
        sourceControlEventDAO,
        telemetryUtils,
        licenseNameProvider,
        componentHelper);
  }

  @Test
  public void collectAllData_WithNoRecentWaivers_ReturnsEmptyList() {
    // when: No violations waived in last 48 hours
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Empty list returned
    assertThat(telemetryData).isEmpty();
  }

  @Test
  public void collectAllData_WithRecentWaiver_ReturnsTelemetryData() {
    // given: A violation waived in last 48 hours
    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), testAppId);
    PolicyViolation violation = createWaivedViolation(jacksonDatabind, waiver, 24);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Telemetry data returned with audit flag
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION);
    assertThat(telemetryData.get(0).getAttributes().get("is_audit_telemetry")).isEqualTo(true);
  }

  @Test
  public void collectAllData_WithMultipleRecentWaivers_ReturnsAllTelemetryData() {
    // given: Multiple violations waived in last 48 hours
    ComponentIdentifier lodash = ComponentIdentifier.createNpmCoordinates("lodash", "4.17.15");
    PolicyWaiver waiver1 = tempEntity.newWaiver(tempEntity.newPolicy().getId(), testAppId);
    PolicyWaiver waiver2 = tempEntity.newWaiver(tempEntity.newPolicy().getId(), testAppId);

    createWaivedViolation(jacksonDatabind, waiver1, 24);
    createWaivedViolation(lodash, waiver2, 36);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Multiple telemetry entries returned
    assertThat(telemetryData).hasSize(2);
    telemetryData.forEach(data -> {
      assertThat(data.getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION);
      assertThat(data.getAttributes().get("is_audit_telemetry")).isEqualTo(true);
    });
  }

  @Test
  public void collectAllData_WithOldWaiver_ExcludesFromResults() {
    // given: A violation waived 72 hours ago (outside 48-hour window)
    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), testAppId);
    createWaivedViolation(jacksonDatabind, waiver, 72);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: No telemetry data returned (outside lookback window)
    assertThat(telemetryData).isEmpty();
  }

  @Test
  public void collectAllData_WithWaiverAtBoundary_IncludesInResults() {
    // given: A violation waived slightly less than 48 hours ago (within boundary)
    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), testAppId);
    createWaivedViolation(jacksonDatabind, waiver, 47);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Telemetry data returned (at boundary)
    assertThat(telemetryData).hasSize(1);
  }

  @Test
  public void collectAllData_WithWaivedAndRemediatedViolation_ExcludesFromResults() {
    // given: A violation waived 24 hours ago and then remediated (fixTime not null)
    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), testAppId);
    PolicyViolation violation = createWaivedViolation(jacksonDatabind, waiver, 24);

    // Remediate the violation (set fixTime)
    violation.setFixTime(DateTime.now().minusHours(12).toDate());
    tempEntity.updatePolicyViolation(violation);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: No telemetry (waiver query excludes remediated violations)
    assertThat(telemetryData).isEmpty();
  }

  @Test
  public void collectAllData_TelemetryContainsExpectedFields() {
    // given: A waived violation
    PolicyWaiver waiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), testAppId);
    PolicyViolation violation = createWaivedViolation(jacksonDatabind, waiver, 24);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Telemetry contains expected fields
    assertThat(telemetryData).hasSize(1);
    TelemetryData data = telemetryData.get(0);

    assertThat(data.getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION);
    assertThat(data.getAttributes().get("is_audit_telemetry")).isEqualTo(true);
    assertThat(data.getAttributes().get("application_id")).isNotNull();
    assertThat(data.getAttributes().get("policy_violation_id")).isNotNull();
    assertThat(data.getAttributes().get("waive_time")).isNotNull();
    assertThat(data.getAttributes().get("policy_waiver_id")).isNotNull();
  }

  @Test
  public void collectAllData_WithAutoWaivedViolation_ReturnsTelemetryData() {
    // given: An auto-waived violation in last 48 hours
    PolicyViolation violation = createAutoWaivedViolation(jacksonDatabind, 24);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Telemetry data returned with audit flag
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION);
    assertThat(telemetryData.get(0).getAttributes().get("is_audit_telemetry")).isEqualTo(true);
    assertThat(telemetryData.get(0).getAttributes().get("auto_policy_waiver_id")).isNotNull();
  }

  @Test
  public void collectAllData_WithMixedManualAndAutoWaivers_ReturnsAllTelemetryData() {
    // given: Both manual and auto-waived violations
    ComponentIdentifier lodash = ComponentIdentifier.createNpmCoordinates("lodash", "4.17.15");
    PolicyWaiver manualWaiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), testAppId);

    createWaivedViolation(jacksonDatabind, manualWaiver, 24);
    createAutoWaivedViolation(lodash, 36);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Both telemetry entries returned
    assertThat(telemetryData).hasSize(2);
    telemetryData.forEach(data -> {
      assertThat(data.getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION);
      assertThat(data.getAttributes().get("is_audit_telemetry")).isEqualTo(true);
    });
  }

  @Test
  public void isClusterTelemetry_ReturnsTrue() {
    // when
    boolean isClusterTelemetry = collector.isClusterTelemetry();

    // then: Should be cluster telemetry
    assertThat(isClusterTelemetry).isTrue();
  }

  /**
   * Creates a waived violation for testing.
   */
  private PolicyViolation createWaivedViolation(
      ComponentIdentifier component,
      PolicyWaiver waiver,
      int hoursAgo)
  {
    // Use unique scan ID to avoid duplicate key violations
    String scanId = "scanId-" + System.nanoTime();
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(testAppId, TEST_STAGE, scanId);

    Date openTime = DateTime.now().minusHours(hoursAgo + 10).toDate();
    evaluation.setTime(openTime);

    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy());
    violation.setComponentIdentifier(component);
    violation.setOpenTime(openTime);
    violation.setWaiveTime(DateTime.now().minusHours(hoursAgo).toDate());
    violation.setPolicyWaiverId(waiver.getId());

    tempEntity.updatePolicyViolation(violation);

    return violation;
  }

  /**
   * Creates an auto-waived violation for testing.
   */
  private PolicyViolation createAutoWaivedViolation(ComponentIdentifier component, int hoursAgo) {
    // Use unique scan ID to avoid duplicate key violations
    String scanId = "scanId-" + System.nanoTime();
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(testAppId, TEST_STAGE, scanId);

    Date openTime = DateTime.now().minusHours(hoursAgo + 10).toDate();
    evaluation.setTime(openTime);

    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy());
    violation.setComponentIdentifier(component);
    violation.setOpenTime(openTime);
    violation.setWaiveTime(DateTime.now().minusHours(hoursAgo).toDate());

    // Set auto waiver ID instead of manual policy waiver ID
    violation.setAutoPolicyWaiverId("auto-waiver-" + System.nanoTime());
    violation.setPolicyWaiverId(null);

    tempEntity.updatePolicyViolation(violation);

    return violation;
  }
}
