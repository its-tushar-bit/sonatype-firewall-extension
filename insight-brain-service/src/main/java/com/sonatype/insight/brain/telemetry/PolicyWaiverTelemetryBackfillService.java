/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.WaiverReasonData;
import com.sonatype.insight.brain.dataaccess.telemetry.HistoricalTelemetryStateDAO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

/**
 * Waiver reason was added to policy waivers in August of 2024. Unfortunately, the waiver reason wasn't added to the
 * policy waiver telemetry at that time. The purpose of this service is to backfill the missing waiver reason into the
 * policy waiver telemetry. It will be sent as a new telemetry purpose and then processes on the backend in Databricks
 * will merge this info into the existing policy waiver and time to waive policy violation telemetry.
 *
 * At the time of this writing there are customers with hundreds of thousands (455k being the highest) of policy waivers
 * since Aug 2024 with missing waiver reasons.
 */
@Named
@Singleton
public class PolicyWaiverTelemetryBackfillService
    extends HistoricalTelemetryService
{
  // we're sending telemetry data out one at a time since each represents thousands of records
  private static final int BATCH_SIZE = 1;

  // the cutoff date is irrelevant since it's not used for anything in this implementation but is still required
  // by the base class and gets recorded in the telemetry state, so we'll at least back date it to before the
  // time that the waiver reason was added to the policy waiver
  private static final Date CUTOFF_DATE = Date.from(
      LocalDate.of(2024, 8, 1)
          .atStartOfDay(ZoneId.of("GMT"))
          .toInstant());

  private static final TelemetryPurpose TELEMETRY_PURPOSE = TelemetryPurpose.POLICY_WAIVER_BACKFILL;

  @VisibleForTesting
  static final String WAIVER_REASON_MAPPING = "waiverReasonMapping";

  // each entry is about 100 bytes max, so 10k entries can be up to about 1MB
  @VisibleForTesting
  int recordsPerBatch = 10_000;

  private final PolicyWaiverDAO policyWaiverDAO;

  @Inject
  public PolicyWaiverTelemetryBackfillService(
      HistoricalTelemetryStateDAO historicalTelemetryStateDAO,
      PolicyWaiverDAO policyWaiverDAO,
      TelemetrySender telemetrySender)
  {
    super(historicalTelemetryStateDAO, TELEMETRY_PURPOSE, telemetrySender, BATCH_SIZE, CUTOFF_DATE);
    this.policyWaiverDAO = policyWaiverDAO;
  }

  public long collectAndSendPolicyWaiverBackfillTelemetry() {
    if (!canCollectAndSendTelemetry()) {
      return 0;
    }

    initialize();

    final var now = new Date();
    var dataLists = Lists.partition(policyWaiverDAO.getPolicyWaiverReasonMappings(), recordsPerBatch);
    dataLists.forEach(dataList -> {
      var telemetryData = toTelemetryData(dataList);
      push(telemetryData, now, getLastWaiverId(dataList));
    });

    return done();
  }

  private String getLastWaiverId(List<WaiverReasonData> dataList) {
    return dataList.get(dataList.size() - 1).policyWaiverId();
  }

  private TelemetryData toTelemetryData(List<WaiverReasonData> dataList) {
    return new TelemetryData(TELEMETRY_PURPOSE).put(WAIVER_REASON_MAPPING, dataList);
  }
}
