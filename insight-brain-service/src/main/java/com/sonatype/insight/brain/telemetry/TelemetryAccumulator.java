/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import io.opentelemetry.instrumentation.annotations.WithSpan;

public class TelemetryAccumulator
{
  private final int batchSize;

  private final TelemetryPurpose telemetryPurpose;

  private final TelemetrySender telemetrySender;

  private final List<TelemetryData> telemetryDataList = new ArrayList<>();

  public TelemetryAccumulator(TelemetryPurpose telemetryPurpose, TelemetrySender telemetrySender, int batchSize) {
    this.telemetryPurpose = telemetryPurpose;
    this.telemetrySender = telemetrySender;
    this.batchSize = batchSize;
  }

  /**
   * Add telemetry data to the accumulator. If the batch size is reached, the data is sent to the telemetry sender. If
   * the telemetry data purpose does not match the expected purpose, an IllegalArgumentException is thrown.
   *
   * @param telemetryData the telemetry data to add
   * @return the number of telemetry data entries sent
   */
  public int add(TelemetryData telemetryData) {
    if (!telemetryData.getPurpose().equals(telemetryPurpose)) {
      throw new IllegalArgumentException("Telemetry data purpose does not match the expected purpose");
    }

    telemetryDataList.add(telemetryData);
    if (telemetryDataList.size() >= batchSize) {
      return flush();
    }

    return 0;
  }

  /**
   * Flush any remaining telemetry data, if any, to the telemetry sender.
   *
   * @return the number of telemetry data entries sent
   */
  @WithSpan
  public int flush() {
    int sendCount = 0;

    if (!telemetryDataList.isEmpty()) {
      sendCount = telemetryDataList.size();
      var listToSend = new ArrayList<>(telemetryDataList);
      telemetrySender.send(listToSend);
      telemetryDataList.clear();
    }

    return sendCount;
  }
}
