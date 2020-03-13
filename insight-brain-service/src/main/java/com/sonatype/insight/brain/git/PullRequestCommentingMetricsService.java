/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_CREATED;
import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.ACTION_UPDATED;
import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.PULL_REQUEST_COMMENT_TELEMETRY;
import static java.lang.String.format;

@Named
@Singleton
public class PullRequestCommentingMetricsService
{
  private final TelemetrySender telemetrySender;

  @Inject
  PullRequestCommentingMetricsService(TelemetrySender telemetrySender) {
    this.telemetrySender = telemetrySender;
  }

  public void onCommentCreated(String applicationId, int pullRequestNumber, int commentId) {
    sendTelemetry(applicationId, pullRequestNumber, commentId, ACTION_CREATED);
  }

  public void onCommentUpdated(String applicationId, int pullRequestNumber, int commentId) {
    sendTelemetry(applicationId, pullRequestNumber, commentId, ACTION_UPDATED);
  }

  private void sendTelemetry(String applicationId, int pullRequestNumber, int commentId, String action) {
    String hash = HdsClientAnalytics.obfuscate(format("%s:%d:%d", applicationId, pullRequestNumber, commentId));
    PullRequestCommentTelemetry commentTelemetry = new PullRequestCommentTelemetry(hash, action);
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_COMMENT);
    telemetryData.put(PULL_REQUEST_COMMENT_TELEMETRY, commentTelemetry);
    telemetrySender.send(telemetryData);
  }
}
