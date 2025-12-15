/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.Set;

import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantTelemetryTaskTest
{
  @Spy
  private TelemetryCollector telemetryCollector1;

  @Spy
  private TelemetryCollector telemetryCollector2;

  @Mock
  private TelemetrySender telemetrySender;

  private MultiTenantTelemetryCollectorsProvider collectors;

  private MultiTenantTelemetryTask multiTenantTelemetryTask;

  @Before
  public void before() {
    collectors = new MultiTenantTelemetryCollectorsProvider(Set.of(telemetryCollector1, telemetryCollector2));
    multiTenantTelemetryTask = new MultiTenantTelemetryTask(collectors, telemetrySender);
  }

  @Test
  public void testExecute_SendsTelemetry() {
    TelemetryData telemetryData1 = mock(TelemetryData.class);
    when(telemetryCollector1.collectData()).thenReturn(telemetryData1);

    TelemetryData telemetryData2 = mock(TelemetryData.class);
    when(telemetryCollector2.collectData()).thenReturn(telemetryData2);

    multiTenantTelemetryTask.execute(null);

    verify(telemetryCollector1).collectAllData();
    verify(telemetryCollector2).collectAllData();
    verify(telemetrySender).send(Collections.singletonList(telemetryData1));
    verify(telemetrySender).send(Collections.singletonList(telemetryData2));
  }
}
