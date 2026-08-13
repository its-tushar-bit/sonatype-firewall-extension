/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.aspectj.lang.Aspects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GuideUsageEventAspectWeavingTest
{
  private GuideUsageTelemetryCollector collector;

  @BeforeEach
  public void setUp() {
    TelemetryId telemetryId = mock(TelemetryId.class);
    when(telemetryId.getId()).thenReturn("t1");
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUsernameOrSystem()).thenReturn("bob");
    collector = new GuideUsageTelemetryCollector(telemetryId, currentUser, mock(TelemetrySender.class), () -> 5L);
    Aspects.aspectOf(GuideUsageEventAspect.class).setCollector(collector); // wire CTW singleton
    GuideChannelContext.set(GuideChannel.API);
  }

  @AfterEach
  public void tearDown() {
    GuideChannelContext.clear();
    // The aspect is a process-wide CTW singleton; null its collector so this test's mock does not
    // leak into other tests in the same JVM (e.g. SearchApiClientImplTest invokes woven methods).
    Aspects.aspectOf(GuideUsageEventAspect.class).setCollector(null);
  }

  @Test
  public void wovenAnnotatedCallRecordsOneEvent() {
    new GuideUsageWeavingProbe().lookup("pkg:maven/g/a@1");

    java.util.List<TelemetryData> data = collector.collectAllData();
    assertThat(data).hasSize(1);
    assertThat(data.get(0).getAttributes()).containsEntry("operation_type", "component_lookup");
    assertThat(data.get(0).getAttributes()).containsEntry("purl", "pkg:maven/g/a@1");
  }
}
