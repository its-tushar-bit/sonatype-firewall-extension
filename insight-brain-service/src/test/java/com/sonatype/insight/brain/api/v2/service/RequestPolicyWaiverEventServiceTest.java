/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.RequestPolicyWaiverEventService;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.brain.webhook.WaiverRequestEvent;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class RequestPolicyWaiverEventServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RequestPolicyWaiverEventService requestPolicyWaiverEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Test
  public void testPostRequestPolicyWaiverEvent_queuesRequestPolicyWaiverInBus() {
    TestEventHandler<WaiverRequestEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getName(), "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
    dto.comment = "waiver comment";
    dto.policyViolationLink = "policyViolationLink.com";
    dto.addWaiverLink = "addWaiverLink.com";

    try {
      requestPolicyWaiverEventService.postRequestPolicyWaiverEvent(policyViolation.getId(), dto);

      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      WaiverRequestEvent event = handler.getEvent();
      assertThat(event.initiator).isEqualTo(USERNAME);
      assertThat(event.timestamp).isNotNull();
      assertThat(event.comment).isEqualTo(dto.comment);
      assertThat(event.policyViolationId).isEqualTo(policyViolation.getId());
      assertThat(event.policyViolationLink).isEqualTo(dto.policyViolationLink);
      assertThat(event.addWaiverLink).isEqualTo(dto.addWaiverLink);
      assertThat(event.ownerId).isEqualTo(application.getId());
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    finally {
      asyncEventBus.unregister(handler);
    }
  }

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

  @Test
  public void testPostRequestPolicyWaiverEvent_requiresValidPolicyViolation() {
    final ApiRequestPolicyWaiverDTO dto = new ApiRequestPolicyWaiverDTO();
    dto.policyViolationLink = "policyViolationLink";
    dto.addWaiverLink = "addWaiverLink";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> requestPolicyWaiverEventService.postRequestPolicyWaiverEvent("invalidPolicyViolationId", dto))
        .withMessage("Could not find associated policy violation");
  }

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
}
