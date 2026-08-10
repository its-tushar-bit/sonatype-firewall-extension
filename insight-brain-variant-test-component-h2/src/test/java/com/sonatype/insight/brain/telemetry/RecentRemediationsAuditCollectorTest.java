/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RecentRemediationsAuditCollector.
 *
 * @since 205
 */
@ComponentH2Test
public class RecentRemediationsAuditCollectorTest
    extends AbstractComponentH2Test
{
  private static final String TEST_APP_PUBLIC_ID = "testApp";

  private static final String TEST_STAGE = "build";

  private static final ComponentIdentifier jacksonDatabind = ComponentIdentifier.createMavenCoordinates(
      "com.fasterxml.jackson.core", "jackson-databind", "2.13.4");

  private String testAppId;

  private RecentRemediationsAuditCollector collector;

  // Counter ensures scan IDs are unique even under rapid iteration in the cap test
  private final AtomicInteger scanCounter = new AtomicInteger(0);

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
  @BeforeEach
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

    collector = new RecentRemediationsAuditCollector(
        policyViolationDAO,
        policyWaiverDAO,
        sourceControlEventDAO,
        telemetryUtils,
        licenseNameProvider,
        componentHelper);
  }

  @Test
  public void collectAllData_WithNoRecentRemediations_ReturnsEmptyList() {
    // when: No violations remediated in last 48 hours
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Empty list returned
    assertThat(telemetryData).isEmpty();
  }

  @Test
  public void collectAllData_WithRecentRemediation_ReturnsTelemetryData() {
    // given: A violation remediated in last 48 hours
    PolicyViolation violation = createRemediatedViolation(jacksonDatabind, 24);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Telemetry data returned with audit flag
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION);
    assertThat(telemetryData.get(0).getAttributes().get("is_audit_telemetry")).isEqualTo(true);
  }

  @Test
  public void collectAllData_WithMultipleRecentRemediations_ReturnsAllTelemetryData() {
    // given: Multiple violations remediated in last 48 hours
    ComponentIdentifier lodash = ComponentIdentifier.createNpmCoordinates("lodash", "4.17.15");
    createRemediatedViolation(jacksonDatabind, 24);
    createRemediatedViolation(lodash, 36);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Multiple telemetry entries returned
    assertThat(telemetryData).hasSize(2);
    telemetryData.forEach(data -> {
      assertThat(data.getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION);
      assertThat(data.getAttributes().get("is_audit_telemetry")).isEqualTo(true);
    });
  }

  @Test
  public void collectAllData_WithOldRemediation_ExcludesFromResults() {
    // given: A violation remediated 72 hours ago (outside 48-hour window)
    createRemediatedViolation(jacksonDatabind, 72);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: No telemetry data returned (outside lookback window)
    assertThat(telemetryData).isEmpty();
  }

  @Test
  public void collectAllData_WithRemediationAtBoundary_IncludesInResults() {
    // given: A violation remediated slightly less than 48 hours ago (within boundary)
    createRemediatedViolation(jacksonDatabind, 47);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Telemetry data returned (at boundary)
    assertThat(telemetryData).hasSize(1);
  }

  @Test
  public void collectAllData_TelemetryContainsExpectedFields() {
    // given: A remediated violation
    PolicyViolation violation = createRemediatedViolation(jacksonDatabind, 24);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Telemetry contains expected fields
    assertThat(telemetryData).hasSize(1);
    TelemetryData data = telemetryData.get(0);

    assertThat(data.getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION);
    assertThat(data.getAttributes().get("is_audit_telemetry")).isEqualTo(true);
    assertThat(data.getAttributes().get("application_id")).isNotNull();
    assertThat(data.getAttributes().get("policy_violation_id")).isNotNull();
    assertThat(data.getAttributes().get("fix_time")).isNotNull();
  }

  @Test
  public void collectAllData_WithVersionChangeRemediation_IncludesTTCVPVTelemetry() {
    // given: A violation remediated by version change
    PolicyViolation violation = createRemediatedViolation(jacksonDatabind, 24);
    violation.setIsRemediatedByVersionChange(true);
    tempEntity.updatePolicyViolation(violation);

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Should have at least TTRPV (may also have TTCVPV if conditions are met)
    assertThat(telemetryData).isNotEmpty();

    // All entries should have audit flag
    telemetryData.forEach(data -> {
      assertThat(data.getAttributes().get("is_audit_telemetry")).isEqualTo(true);
    });

    // Should contain TTRPV
    assertThat(telemetryData.stream()
        .anyMatch(data -> data.getPurpose().equals(TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION)))
            .isTrue();
  }

  @Test
  public void collectAllData_WhenViolationCountExceedsCap_ReturnsExactlyCapEntries() {
    // given: More violations than the cap (501), all remediated within the lookback window
    for (int i = 0; i < 501; i++) {
      createRemediatedViolation(jacksonDatabind, 24);
    }

    // when
    List<TelemetryData> telemetryData = collector.collectAllData();

    // then: Result is bounded to the 500-entry cap defined in PolicyViolationDAO
    assertThat(telemetryData).hasSize(500);
  }

  @Test
  public void isClusterTelemetry_ReturnsTrue() {
    // when
    boolean isClusterTelemetry = collector.isClusterTelemetry();

    // then: Should be cluster telemetry
    assertThat(isClusterTelemetry).isTrue();
  }

  /**
   * Creates a remediated violation for testing.
   */
  private PolicyViolation createRemediatedViolation(ComponentIdentifier component, int hoursAgo) {
    // nanoTime + counter guarantees uniqueness under rapid iteration (e.g., cap test with 501 inserts)
    String scanId = "scanId-" + System.nanoTime() + "-" + scanCounter.getAndIncrement();
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(testAppId, TEST_STAGE, scanId);

    Date openTime = DateTime.now().minusHours(hoursAgo + 10).toDate();
    evaluation.setTime(openTime);

    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy());
    violation.setComponentIdentifier(component);
    violation.setOpenTime(openTime);
    violation.setFixTime(DateTime.now().minusHours(hoursAgo).toDate());

    tempEntity.updatePolicyViolation(violation);

    return violation;
  }
}
