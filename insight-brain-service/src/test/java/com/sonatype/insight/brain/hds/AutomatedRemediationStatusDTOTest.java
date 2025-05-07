/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationPendingDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestDTO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AutomatedRemediationStatusDTOTest
{
  @Test
  public void testFromSourceControlEvent_CompleteStatus_WithDetails() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    event.setEventStatusDetails("https://example.com/pull-request/1");

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto).isInstanceOf(PullRequestDTO.class);
    PullRequestDTO pullRequestDTO = (PullRequestDTO) dto;
    assertThat(pullRequestDTO.url).isEqualTo("https://example.com/pull-request/1");
  }

  @Test
  public void testFromSourceControlEvent_CompleteStatus_WithoutDetails() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);

    assertThatThrownBy(() -> AutomatedRemediationStatusDTO.fromSourceControlEvent(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("URL missing from pull request for id 'eventId'.");
  }

  @Test
  public void testFromSourceControlEvent_PendingStatus_New() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);

    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
    PullRequestCreationPendingDTO pendingDTO = (PullRequestCreationPendingDTO) dto;
    assertThat(pendingDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(pendingDTO.id).isEqualTo("eventId");
  }

  @Test
  public void testFromSourceControlEvent_PendingStatus_InProgress() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);

    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
    PullRequestCreationPendingDTO pendingDTO = (PullRequestCreationPendingDTO) dto;
    assertThat(pendingDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(pendingDTO.id).isEqualTo("eventId");
  }

  @Test
  public void testFromSourceControlEvent_ErrorStatus_WithReason() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("Some error occurred");

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);

    assertThat(dto).isInstanceOf(PullRequestCreationFailedDTO.class);
    PullRequestCreationFailedDTO failedDTO = (PullRequestCreationFailedDTO) dto;
    assertThat(failedDTO.reason).isEqualTo("Some error occurred");
  }

  @Test
  public void testFromSourceControlEvent_ErrorStatus_WithoutReason() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);

    assertThat(dto).isInstanceOf(PullRequestCreationFailedDTO.class);
    PullRequestCreationFailedDTO failedDTO = (PullRequestCreationFailedDTO) dto;
    assertThat(failedDTO.reason).isEqualTo("An unknown error occurred.");
  }

  @Test
  public void testFromSourceControlEvent_UnsupportedStatus() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus("unsupportedStatus");

    assertThatThrownBy(() -> AutomatedRemediationStatusDTO.fromSourceControlEvent(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Unsupported event status 'unsupportedStatus'.");
  }

  @Test
  public void testFromSourceControlEvent_ValidEventTypes() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto).isInstanceOf(AutomatedRemediationStatusDTO.class);

    event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);

    dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto).isInstanceOf(AutomatedRemediationStatusDTO.class);
  }

  @Test
  public void testFromSourceControlEvent_InvalidEventTypes() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType("invalidEventType");

    assertThatThrownBy(() -> AutomatedRemediationStatusDTO.fromSourceControlEvent(event))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Source control event with ID 'eventId' is not a remediation event.");
  }
}
