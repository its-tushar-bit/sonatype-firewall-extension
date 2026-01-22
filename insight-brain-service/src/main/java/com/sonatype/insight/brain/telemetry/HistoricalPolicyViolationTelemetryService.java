/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.telemetry.HistoricalTelemetryStateDAO;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
@Singleton
public class HistoricalPolicyViolationTelemetryService
    extends HistoricalTelemetryService
{
  private static final Date CUTOFF_DATE = Date.from(
      LocalDate.of(2024, 1, 1)
          .atStartOfDay(ZoneId.of("GMT"))
          .toInstant()
  );

  private static final int BATCH_SIZE = 10_000;

  private final PolicyViolationDAO policyViolationDAO;

  private final TelemetryUtils telemetryUtils;

  private final LicenseNameProvider licenseNameProvider;

  @Inject
  public HistoricalPolicyViolationTelemetryService(
      HistoricalTelemetryStateDAO historicalTelemetryStateDAO,
      PolicyViolationDAO policyViolationDAO,
      TelemetrySender telemetrySender,
      TelemetryUtils telemetryUtils,
      LicenseNameProvider licenseNameProvider)
  {
    super(historicalTelemetryStateDAO, TelemetryPurpose.HISTORICAL_POLICY_VIOLATION, telemetrySender, BATCH_SIZE,
        CUTOFF_DATE);
    this.policyViolationDAO = policyViolationDAO;
    this.telemetryUtils = telemetryUtils;
    this.licenseNameProvider = licenseNameProvider;
  }

  /**
   * Fetches a number of days of historical policy violations and sends them as telemetry data.
   *
   * @return the total number of policy violation entries sent as telemetry
   */
  public long collectAndSendPolicyViolationTelemetry() {
    if (!canCollectAndSendTelemetry()) {
      return 0;
    }

    initialize();

    try {
      policyViolationDAO.consumePolicyViolationsSinceDate(getCutoffDate(), getBatchSize(), this::onPolicyViolation);
    }
    catch (Exception e) {
      onError(e);
    }

    return getTotalRecordsSent();
  }

  /**
   * Creates telemetry data from a policy violation.
   *
   * @param policyViolation the policy violation
   * @return the telemetry data
   */
  private TelemetryData createTelemetryData(PolicyViolation policyViolation) {
    return new PolicyViolationTelemetryBuilder(
        policyViolation,
        TelemetryPurpose.HISTORICAL_POLICY_VIOLATION,
        telemetryUtils,
        licenseNameProvider
    )
        .withComponentIdentifier(policyViolation.getComponentIdentifier())
        .withFixTime(policyViolation.getFixTime())
        .withLegacyViolationTime(policyViolation.getLegacyViolationTime())
        .withWaiveTime(policyViolation.getWaiveTime())
        .build();
  }

  private void onPolicyViolation(PolicyViolation policyViolation) {
    try {
      if (null != policyViolation) {
        checkMemory();

        TelemetryData telemetryData = createTelemetryData(policyViolation);
        push(telemetryData, policyViolation.getOpenTime(), policyViolation.getId());
      }
      else {
        TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION);
        telemetryData.put("transmission", "complete");
        push(telemetryData, null, null);
        done();
      }
    }
    catch (Exception e) {
      onError(e);
    }
  }
}
