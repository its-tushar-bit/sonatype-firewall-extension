/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApplicationEvaluationEventServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationEvaluationEventService applicationEvaluationEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Test
  public void testPostEvent() throws InterruptedException {
    final Date time = new Date();

    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setId("policyEvaluationId");
    policyEvaluation.setStageTypeId("stageTypeId");
    policyEvaluation.setApplicationId("applicationId");
    policyEvaluation.setTime(time);

    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    policyEvaluationResult.setAffectedComponentCount(1);
    policyEvaluationResult.setCriticalComponentCount(3);
    policyEvaluationResult.setSevereComponentCount(5);
    policyEvaluationResult.setModerateComponentCount(7);

    TestEventHandler<ApplicationEvaluationEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult);
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event, is(notNullValue()));
    assertThat(event.policyEvaluationId, is(policyEvaluation.getId()));
    assertThat(event.stageTypeId, is(policyEvaluation.getStageTypeId()));
    assertThat(event.ownerId, is(policyEvaluation.getApplicationId()));
    assertThat(event.evaluationDate, is(time));
    assertThat(event.affectedComponentCount, is(1));
    assertThat(event.criticalComponentCount, is(3));
    assertThat(event.severeComponentCount, is(5));
    assertThat(event.moderateComponentCount, is(7));
    assertThat(event.outcome, is(ApplicationEvaluationEvent.ACTION_ID_NONE));
    assertThat(event.initiator, is(USERNAME));

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_CalculateNoneOutcome() throws InterruptedException {
    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();

    // No action provides a ACTION_ID_NONE outcome
    TestEventHandler<ApplicationEvaluationEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult);
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event.outcome, is(ApplicationEvaluationEvent.ACTION_ID_NONE));
    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_CalculateWarnOutcome() throws InterruptedException {
    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();

    // Warn provides a ID_WARN outcome
    policyEvaluationResult.setAlerts(
        Collections.singletonList(new PolicyAlert(null, Collections.singletonList(new Action(Action.ID_WARN)))));

    TestEventHandler<ApplicationEvaluationEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult);
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event.outcome, is(Action.ID_WARN));
    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_CalculateFailOutcome() throws InterruptedException {
    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();

    // Fail and warn provides a ID_FAIL outcome
    policyEvaluationResult.setAlerts(Arrays
        .asList(new PolicyAlert(null, Collections.singletonList(new Action(Action.ID_WARN))),
            new PolicyAlert(null, Collections.singletonList(new Action(Action.ID_FAIL)))));

    TestEventHandler<ApplicationEvaluationEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult);
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event.outcome, is(Action.ID_FAIL));
    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    applicationEvaluationEventService.postEvent(new PolicyEvaluation(), new PolicyEvaluationResult());

    verify(subject, atLeastOnce()).getPrincipal();
  }
}
