/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GuideUsageTelemetryCollectorTest
{
  private TelemetryId telemetryId;

  private CurrentUser currentUser;

  private TelemetrySender sender;

  private GuideUsageTelemetryCollector collector;

  @Before
  public void setUp() {
    telemetryId = mock(TelemetryId.class);
    when(telemetryId.getId()).thenReturn("tele-123");
    currentUser = mock(CurrentUser.class);
    when(currentUser.getUsernameOrSystem()).thenReturn("alice");
    sender = mock(TelemetrySender.class);
    collector = new GuideUsageTelemetryCollector(telemetryId, currentUser, sender, () -> 1_000L);
  }

  @After
  public void tearDown() {
    GuideChannelContext.clear();
  }

  @Test
  public void collectAllDataProducesGuideSelfHostedRecordsAndClears() {
    GuideChannelContext.set(GuideChannel.API);
    collector.record(GuideOperationType.COMPONENT_LOOKUP, new Object[]{"pkg:maven/g/a@1"});

    List<TelemetryData> data = collector.collectAllData();
    assertThat(data).hasSize(1);
    TelemetryData td = data.get(0);
    assertThat(td.getPurpose()).isEqualTo(TelemetryPurpose.GUIDE_SELF_HOSTED_CREDIT_CONSUMPTION);
    assertThat(td.getTimestamp()).isEqualTo(1_000L);
    assertThat(td.getAttributes()).containsEntry("event_type", "CREDIT_CONSUMED_SELF_HOSTED");
    assertThat(td.getAttributes()).containsEntry("event_component", "BACKEND");
    assertThat(td.getAttributes()).containsEntry("operation_type", "component_lookup");
    assertThat(td.getAttributes()).containsEntry("channel", "API");
    assertThat(td.getAttributes()).containsEntry("plan_tier", "SELF_HOSTED");
    assertThat(td.getAttributes()).containsEntry("purl", "pkg:maven/g/a@1");
    // user_id is the sha256 hash of telemetryId+username, never the raw username
    assertThat((String) td.getAttributes().get("user_id")).isNotEqualTo("alice").hasSize(64);

    assertThat(collector.collectAllData()).isEmpty(); // buffer cleared after drain
  }

  @Test
  public void mcpChannelOverridesOperationTypeToMcpLookup() {
    GuideChannelContext.set(GuideChannel.MCP);
    collector.record(GuideOperationType.COMPONENT_LOOKUP, new Object[]{"pkg:npm/lodash@4"});

    TelemetryData td = collector.collectAllData().get(0);
    assertThat(td.getAttributes()).containsEntry("operation_type", "mcp_lookup");
    assertThat(td.getAttributes()).containsEntry("channel", "MCP");
  }

  @Test
  public void mcpVulnerabilityLookupKeysIdentifierAsVulnerabilityId() {
    // The MCP channel overrides operation_type to "mcp_lookup", but the identifier key must still be
    // chosen from the original annotated operation: a vulnerability ref id must not be filed under "purl".
    GuideChannelContext.set(GuideChannel.MCP);
    collector.record(GuideOperationType.VULNERABILITY_LOOKUP, new Object[]{"CVE-2024-0001"});

    TelemetryData td = collector.collectAllData().get(0);
    assertThat(td.getAttributes()).containsEntry("operation_type", "mcp_lookup");
    assertThat(td.getAttributes()).containsEntry("channel", "MCP");
    assertThat(td.getAttributes()).containsEntry("vulnerability_id", "CVE-2024-0001");
    assertThat(td.getAttributes()).doesNotContainKey("purl");
  }

  @Test
  public void isNotClusterTelemetry() {
    assertThat(collector.isClusterTelemetry()).isFalse();
  }

  @Test
  public void earlyFlushSendsBatchAtThresholdAndClearsBuffer() {
    GuideChannelContext.set(GuideChannel.API);
    for (int i = 0; i < GuideUsageTelemetryCollector.FLUSH_THRESHOLD; i++) {
      collector.record(GuideOperationType.COMPONENT_LOOKUP, new Object[]{"pkg:maven/g/a@" + i});
    }
    // early flush drained the buffer and handed the batch to the sender
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<TelemetryData>> captor = ArgumentCaptor.forClass(List.class);
    verify(sender).send(captor.capture());
    assertThat(captor.getValue()).hasSize(GuideUsageTelemetryCollector.FLUSH_THRESHOLD);
    assertThat(collector.collectAllData()).isEmpty();
  }

  @Test
  public void earlyFlushSendFailureDoesNotPropagateToCaller() {
    doThrow(new RuntimeException("sender boom")).when(sender).send(anyList());
    GuideChannelContext.set(GuideChannel.API);
    // recording up to the threshold triggers the guarded early-flush; the sender throws but record() must not
    for (int i = 0; i < GuideUsageTelemetryCollector.FLUSH_THRESHOLD; i++) {
      collector.record(GuideOperationType.COMPONENT_LOOKUP, new Object[]{"pkg:maven/g/a@" + i});
    }
    // no exception propagated; the guard swallowed it. (Buffer was drained by the flush attempt.)
    assertThat(collector.collectAllData()).isEmpty();
  }
}
