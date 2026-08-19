/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;

import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TelemetryQueueTest
{
  @Mock
  private TelemetrySender mockTelemetrySender;

  private TelemetryQueue telemetryQueue;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    telemetryQueue = new TelemetryQueue(() -> mockTelemetrySender);
  }

  @Test
  public void testAdd_doesNotCallSend() {
    TelemetryData telemetryData = mock(TelemetryData.class);

    // when:
    telemetryQueue.add(telemetryData);

    // Verify that send() is not called
    verify(mockTelemetrySender, never()).send(anyList());
  }

  @Test
  public void testFlush_noData() {
    // when:
    telemetryQueue.flush();

    // Verify that send() is called with the correct data
    verify(mockTelemetrySender, never()).send(anyList());
  }

  @Test
  public void testFlush_withData() {
    TelemetryData telemetryData = mock(TelemetryData.class);
    telemetryQueue.add(telemetryData);

    // when:
    telemetryQueue.flush();

    // Verify that send() is called with the correct data
    verify(mockTelemetrySender, times(1)).send(List.of(telemetryData));
  }
}
