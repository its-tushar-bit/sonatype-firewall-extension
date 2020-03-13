/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_CREATED;
import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_UPDATED;
import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.PULL_REQUEST_COMMENT_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestCommentingMetricsServiceTest
{
  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private VersionService mockVersionService;

  @Mock
  private TelemetryId mockTelemetryId;

  @Spy
  TelemetrySenderSpy telemetrySenderSpy = new TelemetrySenderSpy();

  @Test
  public void testOnCommentCreated() {
    // given: metrics service instance with a telemetry sender we can spy on
    PullRequestCommentingMetricsService metricsService = new PullRequestCommentingMetricsService(telemetrySenderSpy);
    String appId = "appId123abc";

    // when: comment created notification
    metricsService.onCommentCreated(appId, 1, 2);

    // then: telemetry sender sent telemetry with the info we're expecting but comment ID is obfuscated
    verify(telemetrySenderSpy, times(1)).send(any(TelemetryData.class));
    TelemetryData telemetryData = telemetrySenderSpy.getTelemetryDataSent();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_COMMENT);

    PullRequestCommentTelemetry commentTelemetry = (PullRequestCommentTelemetry) telemetryData.getAttributes()
        .get(PULL_REQUEST_COMMENT_TELEMETRY);
    assertThat(commentTelemetry).isNotNull();
    assertThat(commentTelemetry.id).isNotEmpty();
    assertThat(commentTelemetry.id).doesNotContain(appId);
    assertThat(commentTelemetry.action).isEqualTo(ACTION_CREATED);
  }

  @Test
  public void testOnCommentUpdated() {
    // given: metrics service instance with a telemetry sender we can spy on
    PullRequestCommentingMetricsService metricsService = new PullRequestCommentingMetricsService(telemetrySenderSpy);
    String appId = "appId456xyz";

    // when: comment created notification
    metricsService.onCommentUpdated(appId, 1, 2);

    // then: telemetry sender sent telemetry with the info we're expecting but comment ID is obfuscated
    verify(telemetrySenderSpy, times(1)).send(any(TelemetryData.class));
    TelemetryData telemetryData = telemetrySenderSpy.getTelemetryDataSent();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_COMMENT);

    PullRequestCommentTelemetry commentTelemetry = (PullRequestCommentTelemetry) telemetryData.getAttributes()
        .get(PULL_REQUEST_COMMENT_TELEMETRY);
    assertThat(commentTelemetry).isNotNull();
    assertThat(commentTelemetry.id).isNotEmpty();
    assertThat(commentTelemetry.id).doesNotContain(appId);
    assertThat(commentTelemetry.action).isEqualTo(ACTION_UPDATED);
  }

  private class TelemetrySenderSpy
      extends TelemetrySender
  {
    private TelemetryData telemetryDataSent;

    TelemetrySenderSpy() {
      super(mockHdsClient, mockVersionService, mockTelemetryId);
    }

    @Override
    public void send(TelemetryData telemetryData) {
      telemetryDataSent = telemetryData;
    }

    TelemetryData getTelemetryDataSent() {
      return telemetryDataSent;
    }
  }
}
