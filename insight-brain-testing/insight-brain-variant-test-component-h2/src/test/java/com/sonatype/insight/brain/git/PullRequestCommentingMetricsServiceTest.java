/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuditTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import static com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry.PULL_REQUEST_COMMENT_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@ComponentH2Test
public class PullRequestCommentingMetricsServiceTest
    extends AbstractComponentH2AuditTest
{
  @Mock
  private TelemetrySender mockTelemetrySender;

  @Inject
  private PullRequestCommentingMetricsService pullRequestCommentingMetricsService;

  @Test
  public void testSendTelemetry() {
    // given: comment in PR of an application
    String realAppId = "app1";
    PullRequestCommentTelemetry commentTelemetry = new PullRequestCommentTelemetry(realAppId, 100, realAppId);
    ArgumentCaptor<TelemetryData> argCaptor = ArgumentCaptor.forClass(TelemetryData.class);

    // when: sending telemetry
    pullRequestCommentingMetricsService.sendTelemetry(commentTelemetry);

    // then: information of the comment sent
    verify(mockTelemetrySender, times(1)).send(argCaptor.capture());
    TelemetryData telemetryData = argCaptor.getValue();
    PullRequestCommentTelemetry telemetrySent =
        (PullRequestCommentTelemetry) telemetryData.getAttributes().get(PULL_REQUEST_COMMENT_TELEMETRY);
    assertThat(telemetrySent).isEqualTo(commentTelemetry);
    assertThat(telemetrySent.realApplicationId).isNotEmpty();
    assertThat(telemetrySent.realApplicationId).isEqualTo(realAppId);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_COMMENT);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
  }

  @Test
  public void testAddAuditRecord() {
    // given: pull request comment creation event for application
    AuditEvent auditEvent = AuditEvent.CREATE_PULL_REQUEST_COMMENT;
    Application app = tempEntity.newApplicationWithParent();

    // when: storing audit record
    pullRequestCommentingMetricsService.addAuditRecord(auditEvent, app.getId(), "example.com", 1);

    // then: event stored in log
    AuditDTO auditDTO = getLogEntries(auditEvent).get(0);
    assertThat(auditDTO.data).containsEntry("applicationName", app.getName());
    assertThat(auditDTO.data).containsEntry("applicationPublicId", app.getPublicId());
    assertThat(auditDTO.data).containsEntry("repositoryUrl", "example.com");
    assertThat(auditDTO.data).containsEntry("pullRequestNumber", 1);
  }
}
