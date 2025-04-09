/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatus;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationPendingDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SourceControlPullRequestServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SourceControlPullRequestService service;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Test
  public void testGetPullRequestStatus_IdNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getPullRequestStatus("doesNotExist")
    ).withMessageContaining("SourceControlEvent with ID doesNotExist does not exist.");
  }

  @Test
  public void testGetPullRequestStatus_IdNotRemediation() {
    Application app = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(app);
    event.setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    sourceControlEventDAO.update(event);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getPullRequestStatus(event.getId())
    ).withMessageContaining(
        "Pull request not found for application '" + app.getPublicId() + "' and for id '" + event.getId() + "'.");
  }

  @Test
  public void testGetPullRequestStatus_ApplicationId() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
  }

  @Test
  public void testGetPullRequestStatus_ApplicationPublicId() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
  }

  @Test
  public void testGetPullRequestStatus_Pending_StatusNew() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
  }

  @Test
  public void testGetPullRequestStatus_Pending_StatusInProgress() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
  }

  @Test
  public void testGetPullRequestStatus_Error_StatusError() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
    assertThat(dto).isInstanceOf(PullRequestCreationFailedDTO.class);
    PullRequestCreationFailedDTO pullRequestCreationFailedDTO = (PullRequestCreationFailedDTO) dto;
    assertThat(pullRequestCreationFailedDTO.reason).isEqualTo("An unknown error occurred.");
  }

  @Test
  public void testGetPullRequestStatus_Error_StatusError_WithReason() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("some error");
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
    assertThat(dto).isInstanceOf(PullRequestCreationFailedDTO.class);
    PullRequestCreationFailedDTO pullRequestCreationFailedDTO = (PullRequestCreationFailedDTO) dto;
    assertThat(pullRequestCreationFailedDTO.reason).isEqualTo("some error");
  }

  @Test
  public void testGetPullRequestStatus_Completed_StatusComplete() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    event.setEventStatusDetails("some pr link");
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST);
    assertThat(dto).isInstanceOf(PullRequestDTO.class);
    PullRequestDTO pullRequestDTO = (PullRequestDTO) dto;
    assertThat(pullRequestDTO.url).isEqualTo("some pr link");
  }

  @Test
  public void testGetPullRequestStatus_Completed_StatusComplete_UrlMissing() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.update(event);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> service.getPullRequestStatus(event.getId())
    ).withMessageContaining("URL missing from pull request for id '" + event.getId() + "'.");
  }

  @Test
  public void testGetPullRequestStatus_UnsupportedStatus() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_PARTIALLY_COMPLETE);
    sourceControlEventDAO.update(event);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> service.getPullRequestStatus(event.getId())
    ).withMessageContaining("Unsupported event status 'partially complete'.");
  }

  @Test
  public void testGetPullRequestStatus_UnknownStatus() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus("someUnknownStatus");
    sourceControlEventDAO.update(event);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> service.getPullRequestStatus(event.getId())
    ).withMessageContaining("Unsupported event status 'someUnknownStatus'.");
  }

  private SourceControlEvent createRemediationEvent(final Application app) {
    SourceControlEvent event = new SourceControlEvent().forRemediationPullRequest().setApplicationId(app.getId());
    sourceControlEventDAO.insert(event);
    return event;
  }
}
