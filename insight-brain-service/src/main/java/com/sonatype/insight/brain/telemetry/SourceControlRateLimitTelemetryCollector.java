/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.client.utils.RateLimitRecorder;
import com.sonatype.insight.client.utils.RateLimitRecorder.RateLimitData;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shiro.util.CollectionUtils;

@Named
@Singleton
public class SourceControlRateLimitTelemetryCollector
    implements TelemetryCollector
{
  public static final String NONE = "none";

  @Override
  public List<TelemetryData> collectAllData() {
    List<TelemetryData> telemetryDataList = new ArrayList<>();

    Map<String, RateLimitData> rateLimitDataMap = RateLimitRecorder.fetchAndResetDailyRateLimitData();
    if (CollectionUtils.isEmpty(rateLimitDataMap)) {
      addTelemetry(telemetryDataList, NONE, NONE, 0, 0, 0);
    }
    else {
      rateLimitDataMap.forEach((k, v) -> {
        addTelemetry(telemetryDataList, v.clientId, v.userId, v.count, v.minRemaining, v.timesExceeded);
      });
    }

    return telemetryDataList;
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }

  private void addTelemetry(
      final List<TelemetryData> telemetryDataList,
      final String clientId,
      final String userId,
      final int calls,
      final int minRemaining,
      final int timesExceeded)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_RATE_LIMITS);

    telemetryData.put(SourceControlRateLimitTelemetry.SOURCE_CONTROL_RATE_LIMITS, new SourceControlRateLimitTelemetry(
        clientId,
        DigestUtils.sha512Hex(userId),
        calls,
        minRemaining,
        timesExceeded
    ));

    telemetryDataList.add(telemetryData);
  }
}
