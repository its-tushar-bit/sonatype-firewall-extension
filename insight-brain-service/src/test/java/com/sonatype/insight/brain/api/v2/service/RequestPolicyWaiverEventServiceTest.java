/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.RequestPolicyWaiverEventService;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.brain.webhook.WaiverRequestEvent;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Category(SlowTest.class)
public class RequestPolicyWaiverEventServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RequestPolicyWaiverEventService requestPolicyWaiverEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Inject
  PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Before
  public void setUpBaseUrl() {
    setBaseUrl("http://localhost:1234");
  }

  /**
   * @deprecated Deprecated because the tested method is deprecated.
   */
  @Deprecated(since = "1.192")
  @Test
  public void testPostRequestPolicyWaiverEvent_queuesRequestPolicyWaiverInBus() {
    TestEventHandler<WaiverRequestEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), WaiverRequestEvent.class);
    asyncEventBus.register(handler);
    var mockTelemetrySender = mock(TelemetrySender.class);
    requestPolicyWaiverEventService.setTelemetrySender(mockTelemetrySender);

    try {
      Application application = tempEntity.newApplicationWithParent();
      Policy policy = tempEntity.newPolicy(application);
      PolicyEvaluation policyEvaluation =
          tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getName(), "scanId");
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

      ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
      dto.reasonId = "9b704ef5bc064fc29d7fe08a251ee9a6";
      dto.comment = "waiver comment";
      dto.policyViolationLink = "policyViolationLink.com";
      dto.addWaiverLink = "addWaiverLink.com";

      final var waiverReason = policyWaiverReasonDAO.getById(dto.reasonId);

      requestPolicyWaiverEventService.postRequestPolicyWaiverEvent(policyViolation.getId(), dto);

      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      WaiverRequestEvent event = handler.getEvent();
      assertThat(event.initiator).isEqualTo(USERNAME);
      assertThat(event.timestamp).isNotNull();
      assertThat(event.comment).isEqualTo(dto.comment);
      assertThat(event.reasonId).isEqualTo(dto.reasonId);
      assertThat(event.policyViolationId).isEqualTo(policyViolation.getId());
      assertThat(event.policyViolationLink).isEqualTo(dto.policyViolationLink);
      assertThat(event.addWaiverLink).isEqualTo(dto.addWaiverLink);
      assertThat(event.ownerId).isEqualTo(application.getId());

      ArgumentCaptor<TelemetryData> telemetryDataCaptor = ArgumentCaptor.forClass(TelemetryData.class);
      verify(mockTelemetrySender, times(1)).send(telemetryDataCaptor.capture());
      TelemetryData actualTelemetryData = telemetryDataCaptor.getValue();
      var attributes = actualTelemetryData.getAttributes();
      assertThat(attributes)
          .containsKey("application_id")
          .doesNotContainEntry("application_id", policyViolation.getApplicationId())
          .containsEntry("real_application_id", policyViolation.getApplicationId())
          .containsEntry("count", 1)
          .containsEntry("open_time", policyViolation.getOpenTime().getTime())
          .containsEntry("policy_name", policyViolation.getPolicyName())
          .containsEntry("policy_violation_id", policyViolation.getId())
          .containsEntry("stage_id", policyViolation.getStageTypeId())
          .containsEntry("threat_category", policyViolation.getThreatCategory().getName())
          .containsEntry("threat_level", policyViolation.getThreatLevel())
          .containsEntry("waiver_reason", waiverReason.getReasonText());
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    finally {
      asyncEventBus.unregister(handler);
    }
  }

  /**
   * @deprecated Deprecated because the tested method is deprecated.
   */
  @Deprecated(since = "1.192")
  @Test
  public void testPostRequestPolicyWaiverEvent_dtoFieldsAreRequiredExceptComment() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> requestPolicyWaiverEventService.postRequestPolicyWaiverEvent("policyViolationId", null))
        .withMessage("both addWaiverLink and policyViolationLink are required");

    final ApiRequestPolicyWaiverDTO dtoMissingWaiverLink = new ApiRequestPolicyWaiverDTO();
    dtoMissingWaiverLink.addWaiverLink = "addWaiverLink";
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> requestPolicyWaiverEventService.postRequestPolicyWaiverEvent("policyViolationId", dtoMissingWaiverLink))
        .withMessage("both addWaiverLink and policyViolationLink are required");

    final ApiRequestPolicyWaiverDTO dtoMissingViolationLink = new ApiRequestPolicyWaiverDTO();
    dtoMissingViolationLink.policyViolationLink = "policyViolationLink";
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> requestPolicyWaiverEventService.postRequestPolicyWaiverEvent("policyViolationId",
            dtoMissingViolationLink))
        .withMessage("both addWaiverLink and policyViolationLink are required");
  }

  /**
   * @deprecated Deprecated because the tested method is deprecated.
   */
  @Deprecated(since = "1.192")
  @Test
  public void testPostRequestPolicyWaiverEvent_requiresValidPolicyViolation() {
    final ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
    dto.policyViolationLink = "policyViolationLink";
    dto.addWaiverLink = "addWaiverLink";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> requestPolicyWaiverEventService.postRequestPolicyWaiverEvent("invalidPolicyViolationId", dto))
        .withMessage("Could not find associated policy violation");
  }

  /**
   * @deprecated Deprecated because the tested method is deprecated.
   */
  @Deprecated(since = "1.192")
  @Test
  public void testPostRequestPolicyWaiverEvent_commentHasMax1_000Characters() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getName(), "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    StringBuilder commentBuilder = new StringBuilder();
    IntStream.range(0, 1200).forEach(commentBuilder::append);
    ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
    dto.comment = commentBuilder.toString();
    dto.policyViolationLink = "policyViolationLink.com";
    dto.addWaiverLink = "addWaiverLink.com";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> requestPolicyWaiverEventService.postRequestPolicyWaiverEvent(policyViolation.getId(), dto))
        .withMessage("Comment length must not exceed 1000 characters.");
  }

  @Test
  public void testPostPolicyWaiverRequestEvent_queuesPolicyWaiverRequestInBus() throws InterruptedException {
    TestEventHandler<WaiverRequestEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), WaiverRequestEvent.class);
    asyncEventBus.register(handler);
    var mockTelemetrySender = mock(TelemetrySender.class);
    requestPolicyWaiverEventService.setTelemetrySender(mockTelemetrySender);

    try {
      Application application = tempEntity.newApplicationWithParent();
      Policy policy = tempEntity.newPolicy(application);
      PolicyEvaluation policyEvaluation =
          tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getName(), "scanId");
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

      String reasonId = "9b704ef5bc064fc29d7fe08a251ee9a6";
      String comment = "waiver comment";
      String policyWaiverRequestId = "policyWaiverRequestId001";

      String expectedPolicyViolationLink =
          String.format("http://localhost:1234/ui/links/policyViolation/%s", policyViolation.getId());
      String expectedAddWaiverLink = String.format(
          "http://localhost:1234/ui/links/addWaiver/%s?comments=%s&reasonId=%s",
          policyViolation.getId(), comment.replace(" ", "+"), reasonId
      );
      String expectedReviewWaiverRequestLink = String.format(
          "http://localhost:1234/ui/links/requestWaiverReview/%s/%s/%s",
          OwnerType.APPLICATION, application.getId(), policyWaiverRequestId
      );

      requestPolicyWaiverEventService.postPolicyWaiverRequestEvent(
          policyViolation.getId(), comment, reasonId, OwnerType.APPLICATION.toString(), application.getId(),
          policyWaiverRequestId);

      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      WaiverRequestEvent event = handler.getEvent();
      assertThat(event.initiator).isEqualTo(USERNAME);
      assertThat(event.timestamp).isNotNull();
      assertThat(event.comment).isEqualTo(comment);
      assertThat(event.reasonId).isEqualTo(reasonId);
      assertThat(event.policyViolationId).isEqualTo(policyViolation.getId());
      assertThat(event.policyViolationLink).isEqualTo(expectedPolicyViolationLink);
      assertThat(event.addWaiverLink).isEqualTo(expectedAddWaiverLink);
      assertThat(event.reviewWaiverRequestLink).isEqualTo(expectedReviewWaiverRequestLink);
      assertThat(event.ownerId).isEqualTo(application.getId());
    }
    finally {
      asyncEventBus.unregister(handler);
    }
  }

  @Test
  public void testPostPolicyWaiverRequestEvent_requiresValidPolicyViolationId() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
            () -> requestPolicyWaiverEventService.postPolicyWaiverRequestEvent(
                "invalidPolicyViolationId", "comment", "policyViolationLink", "addWaiverLink",
                "reviewWaiverRequestLink",
                "reasonId"))
        .withMessage("Could not find associated policy violation");
  }

  @Test
  public void testPostPolicyWaiverRequestEvent_commentExceeding1000Characters() {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getName(), "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    String comment = "a".repeat(1001);

    String policyWaiverRequestId = "policyWaiverRequestId001";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> requestPolicyWaiverEventService.postPolicyWaiverRequestEvent(
            policyViolation.getId(), comment, "reasonId", OwnerType.APPLICATION.toString(), application.getId(),
            policyWaiverRequestId))
        .withMessage("Comment length must not exceed 1000 characters.");
  }

  @Test
  public void testPostPolicyWaiverRequestEvent_commentWithExactly1000Characters() throws InterruptedException {
    TestEventHandler<WaiverRequestEvent> handler = 
        new TestEventHandler<>(new CountDownLatch(1), WaiverRequestEvent.class);
    asyncEventBus.register(handler);
    try {
      Application application = tempEntity.newApplicationWithParent();
      Policy policy = tempEntity.newPolicy(application);
      PolicyEvaluation policyEvaluation =
          tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getName(), "scanId");
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

      String comment = "a".repeat(1000);
      String reasonId = "9b704ef5bc064fc29d7fe08a251ee9a6";
      String policyWaiverRequestId = "policyWaiverRequestId001";

      // This should not throw an exception as the comment is exactly 1000 characters
      requestPolicyWaiverEventService.postPolicyWaiverRequestEvent(
          policyViolation.getId(), comment, reasonId, OwnerType.APPLICATION.toString(), application.getId(),
          policyWaiverRequestId);

      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      WaiverRequestEvent event = handler.getEvent();
      assertThat(event.comment).isEqualTo(comment);
      assertThat(event.comment.length()).isEqualTo(1000);
    }
    finally {
      asyncEventBus.unregister(handler);
    }
  }

  @Test
  public void testPostPolicyWaiverRequestEvent_commentIsNull() throws InterruptedException {
    TestEventHandler<WaiverRequestEvent> handler = 
        new TestEventHandler<>(new CountDownLatch(1), WaiverRequestEvent.class);
    asyncEventBus.register(handler);
    try {
      Application application = tempEntity.newApplicationWithParent();
      Policy policy = tempEntity.newPolicy(application);
      PolicyEvaluation policyEvaluation =
          tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getName(), "scanId");
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

      String comment = null;
      String reasonId = "9b704ef5bc064fc29d7fe08a251ee9a6";
      String policyWaiverRequestId = "policyWaiverRequestId001";

      String expectedPolicyViolationLink =
          String.format("http://localhost:1234/ui/links/policyViolation/%s", policyViolation.getId());
      String expectedAddWaiverLink = String.format(
          "http://localhost:1234/ui/links/addWaiver/%s?reasonId=%s",
          policyViolation.getId(), reasonId
      );
      String expectedReviewWaiverRequestLink = String.format(
          "http://localhost:1234/ui/links/requestWaiverReview/%s/%s/%s",
          OwnerType.APPLICATION, application.getId(), policyWaiverRequestId
      );

      requestPolicyWaiverEventService.postPolicyWaiverRequestEvent(
          policyViolation.getId(), comment, reasonId, OwnerType.APPLICATION.toString(), application.getId(),
          policyWaiverRequestId);

      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      WaiverRequestEvent event = handler.getEvent();
      assertThat(event.initiator).isEqualTo(USERNAME);
      assertThat(event.timestamp).isNotNull();
      assertThat(event.comment).isNull();
      assertThat(event.reasonId).isEqualTo(reasonId);
      assertThat(event.policyViolationId).isEqualTo(policyViolation.getId());
      assertThat(event.policyViolationLink).isEqualTo(expectedPolicyViolationLink);
      assertThat(event.addWaiverLink).isEqualTo(expectedAddWaiverLink);
      assertThat(event.reviewWaiverRequestLink).isEqualTo(expectedReviewWaiverRequestLink);
      assertThat(event.ownerId).isEqualTo(application.getId());
    }
    finally {
      asyncEventBus.unregister(handler);
    }
  }

  @Test
  public void testPostPolicyWaiverRequestEvent_reasonIdIsNull() throws InterruptedException {
    TestEventHandler<WaiverRequestEvent> handler = 
        new TestEventHandler<>(new CountDownLatch(1), WaiverRequestEvent.class);
    asyncEventBus.register(handler);
    try {
      Application application = tempEntity.newApplicationWithParent();
      Policy policy = tempEntity.newPolicy(application);
      PolicyEvaluation policyEvaluation =
          tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getName(), "scanId");
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

      String comment = "waiver comment";
      String reasonId = null;
      String policyWaiverRequestId = "policyWaiverRequestId001";

      String expectedPolicyViolationLink =
          String.format("http://localhost:1234/ui/links/policyViolation/%s", policyViolation.getId());
      String expectedAddWaiverLink = String.format(
          "http://localhost:1234/ui/links/addWaiver/%s?comments=%s",
          policyViolation.getId(), comment.replace(" ", "+")
      );
      String expectedReviewWaiverRequestLink = String.format(
          "http://localhost:1234/ui/links/requestWaiverReview/%s/%s/%s",
          OwnerType.APPLICATION, application.getId(), policyWaiverRequestId
      );

      requestPolicyWaiverEventService.postPolicyWaiverRequestEvent(
          policyViolation.getId(), comment, reasonId, OwnerType.APPLICATION.toString(), application.getId(),
          policyWaiverRequestId);

      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      WaiverRequestEvent event = handler.getEvent();
      assertThat(event.initiator).isEqualTo(USERNAME);
      assertThat(event.timestamp).isNotNull();
      assertThat(event.comment).isEqualTo(comment);
      assertThat(event.reasonId).isNull();
      assertThat(event.policyViolationId).isEqualTo(policyViolation.getId());
      assertThat(event.policyViolationLink).isEqualTo(expectedPolicyViolationLink);
      assertThat(event.addWaiverLink).isEqualTo(expectedAddWaiverLink);
      assertThat(event.reviewWaiverRequestLink).isEqualTo(expectedReviewWaiverRequestLink);
      assertThat(event.ownerId).isEqualTo(application.getId());
    }
    finally {
      asyncEventBus.unregister(handler);
    }
  }
}
