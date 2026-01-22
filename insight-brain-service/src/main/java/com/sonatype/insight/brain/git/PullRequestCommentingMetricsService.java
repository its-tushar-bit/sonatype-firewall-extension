/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.PULL_REQUEST_COMMENT_TELEMETRY;

@Named
@Singleton
public class PullRequestCommentingMetricsService
{
  private final TelemetrySender telemetrySender;

  private final AuditRecorder auditRecorder;

  private final ApplicationDAO applicationDAO;

  @Inject
  PullRequestCommentingMetricsService(
      final TelemetrySender telemetrySender,
      final AuditRecorder auditRecorder,
      final ApplicationDAO applicationDAO)
  {
    this.telemetrySender = telemetrySender;
    this.auditRecorder = auditRecorder;
    this.applicationDAO = applicationDAO;
  }

  public void sendTelemetry(final PullRequestCommentTelemetry telemetry) {
    telemetry.applicationId = HdsClientAnalytics.obfuscate(telemetry.applicationId);
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_COMMENT);
    telemetryData.put(PULL_REQUEST_COMMENT_TELEMETRY, telemetry);
    telemetrySender.send(telemetryData);
  }

  public void addAuditRecord(
      final AuditEvent auditEvent,
      final String applicationId,
      final String repositoryUrl,
      final int prNumber)
  {
    try (AuditSession auditSession = auditRecorder.recordSystemEvent(auditEvent)) {
      Application application = applicationDAO.getById(applicationId);
      AuditData.get()
          .setApplication(application)
          .setData("repositoryUrl", repositoryUrl)
          .setData("pullRequestNumber", prNumber);
    }
  }
}
