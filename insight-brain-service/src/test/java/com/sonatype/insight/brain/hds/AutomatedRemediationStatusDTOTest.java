/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.git.PullRequestFailureCategory;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationPendingDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestDTO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AutomatedRemediationStatusDTOTest
{
  @RegisterExtension
  public LogOutput logOutput = new LogOutput(AutomatedRemediationStatusDTO.class);

  @Test
  public void testFromSourceControlEvent_CompleteStatus_WithDetails() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    event.setEventStatusDetails("https://example.com/pull-request/1");
    event.setPullRequestNumber(123);

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto).isInstanceOf(PullRequestDTO.class);
    PullRequestDTO pullRequestDTO = (PullRequestDTO) dto;
    assertThat(pullRequestDTO.url).isEqualTo("https://example.com/pull-request/1");
    assertThat(pullRequestDTO.pullRequestId).isEqualTo(123);
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
  public void testFromSourceControlEvent_CompleteStatus_WithDetailsNoPRNumber() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("eventId");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    event.setEventStatusDetails("https://example.com/pull-request/1");

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);

    assertThat(dto).isInstanceOf(PullRequestDTO.class);
    PullRequestDTO pendingDTO = (PullRequestDTO) dto;
    assertThat(pendingDTO.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST);
    assertThat(pendingDTO.pullRequestId).isNull();
    assertThat(pendingDTO.url).isEqualTo("https://example.com/pull-request/1");
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

  @Test
  public void testFromSourceControlEvent_ErrorStatus_ManifestCategory() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("e1");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("Pull request creation failed: ...");
    event.setEventFailureCategory("MANIFEST_COMPONENT_NOT_FOUND");
    event.setEventIsRetryable(false);

    AutomatedRemediationStatusDTO dto = AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto).isInstanceOf(PullRequestCreationFailedDTO.class);
    PullRequestCreationFailedDTO failed = (PullRequestCreationFailedDTO) dto;
    assertThat(failed.reason).isEqualTo("Pull request creation failed: ...");
    assertThat(failed.failureCategory).isEqualTo(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND);
    assertThat(failed.isRetryable).isFalse();
  }

  @Test
  public void testFromSourceControlEvent_ErrorStatus_ScmErrorCategory() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("e2");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("boom");
    event.setEventFailureCategory("SCM_ERROR");
    event.setEventIsRetryable(true);

    PullRequestCreationFailedDTO dto =
        (PullRequestCreationFailedDTO) AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto.failureCategory).isEqualTo(PullRequestFailureCategory.SCM_ERROR);
    assertThat(dto.isRetryable).isTrue();
  }

  @Test
  public void testFromSourceControlEvent_ErrorStatus_LegacyNulls_DefaultToUnknownRetryable() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("e3");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("legacy reason");
    // Both new fields left null (simulates pre-migration row)

    PullRequestCreationFailedDTO dto =
        (PullRequestCreationFailedDTO) AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto.failureCategory).isEqualTo(PullRequestFailureCategory.UNKNOWN);
    assertThat(dto.isRetryable).isTrue();
  }

  @Test
  public void testFromSourceControlEvent_ErrorStatus_UnknownEnumString_MapsToUnknown() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("e4");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("newer-server reason");
    event.setEventFailureCategory("FUTURE_CATEGORY_NOT_YET_IN_THIS_BUILD");
    event.setEventIsRetryable(true);

    PullRequestCreationFailedDTO dto =
        (PullRequestCreationFailedDTO) AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto.failureCategory).isEqualTo(PullRequestFailureCategory.UNKNOWN);
    assertThat(dto.isRetryable).isTrue();
    // Drift between server versions should leave a breadcrumb in the logs so ops can
    // catch a downgrade or split-deploy that's silently dropping classification info.
    assertThat(logOutput).atWarnLevel().contains("FUTURE_CATEGORY_NOT_YET_IN_THIS_BUILD");
  }

  @Test
  public void testFromSourceControlEvent_ErrorStatus_CategorySetButIsRetryableNull_DerivesRetryable() {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("e5");
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("partial legacy");
    event.setEventFailureCategory("MANIFEST_COMPONENT_NOT_FOUND");
    // isRetryable left null — should derive from enum

    PullRequestCreationFailedDTO dto =
        (PullRequestCreationFailedDTO) AutomatedRemediationStatusDTO.fromSourceControlEvent(event);
    assertThat(dto.failureCategory).isEqualTo(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND);
    assertThat(dto.isRetryable).isFalse();
  }

  /*
   * Jackson deserialization round-trips. Guards the JSON-read path against the same
   * version-skew failure mode that parseCategory() handles for the DB-read path.
   */

  @Test
  public void deserializeFromJson_knownCategory_setsFields() throws Exception {
    String json = "{\"status\":\"PULL_REQUEST_CREATION_FAILED\","
        + "\"reason\":\"boom\",\"failureCategory\":\"MANIFEST_COMPONENT_NOT_FOUND\",\"isRetryable\":false}";
    PullRequestCreationFailedDTO dto = new ObjectMapper().readValue(json, PullRequestCreationFailedDTO.class);
    assertThat(dto.failureCategory).isEqualTo(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND);
    assertThat(dto.isRetryable).isFalse();
    assertThat(dto.reason).isEqualTo("boom");
  }

  @Test
  public void deserializeFromJson_unknownCategoryString_mapsToUnknown() {
    String json = "{\"status\":\"PULL_REQUEST_CREATION_FAILED\","
        + "\"reason\":\"newer-server reason\",\"failureCategory\":\"FUTURE_CATEGORY_FROM_NEWER_NODE\",\"isRetryable\":true}";
    // Without the @JsonCreator factory routing failureCategory through parseCategory(), default
    // Jackson behaviour would throw InvalidFormatException on the unknown enum value. Asserting
    // it does NOT throw guards the downgrade / split-version-deploy path.
    assertThatCode(() -> {
      PullRequestCreationFailedDTO dto = new ObjectMapper().readValue(json, PullRequestCreationFailedDTO.class);
      assertThat(dto.failureCategory).isEqualTo(PullRequestFailureCategory.UNKNOWN);
      assertThat(dto.isRetryable).isTrue(); // value from JSON is preserved
    }).doesNotThrowAnyException();
    assertThat(logOutput).atWarnLevel().contains("FUTURE_CATEGORY_FROM_NEWER_NODE");
  }

  @Test
  public void deserializeFromJson_nullCategory_defaultsToUnknownRetryable() throws Exception {
    String json = "{\"status\":\"PULL_REQUEST_CREATION_FAILED\","
        + "\"reason\":\"legacy\",\"failureCategory\":null,\"isRetryable\":null}";
    PullRequestCreationFailedDTO dto = new ObjectMapper().readValue(json, PullRequestCreationFailedDTO.class);
    assertThat(dto.failureCategory).isEqualTo(PullRequestFailureCategory.UNKNOWN);
    assertThat(dto.isRetryable).isTrue();
  }
}
