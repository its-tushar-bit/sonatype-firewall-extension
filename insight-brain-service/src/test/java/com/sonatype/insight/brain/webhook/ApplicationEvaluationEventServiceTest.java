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

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

    Application application = tempEntity.newApplicationWithParent();
    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setId("policyEvaluationId");
    policyEvaluation.setStageTypeId("stageTypeId");
    policyEvaluation.setApplicationId(application.getId());
    policyEvaluation.setScanId("reportId");
    policyEvaluation.setTime(time);
    policyEvaluation.setCommitHash("commitHash");

    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    policyEvaluationResult.setAffectedComponentCount(1);
    policyEvaluationResult.setCriticalComponentCount(3);
    policyEvaluationResult.setSevereComponentCount(5);
    policyEvaluationResult.setModerateComponentCount(7);

    TestEventHandler<ApplicationEvaluationEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), ApplicationEvaluationEvent.class);
    asyncEventBus.register(handler);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult, application);
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.policyEvaluationId).isEqualTo(policyEvaluation.getId());
    assertThat(event.stageTypeId).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(event.ownerId).isEqualTo(policyEvaluation.getApplicationId());
    assertThat(event.isForLatestScan).isEqualTo(!policyEvaluation.isForObsoleteScan());
    assertThat(event.evaluationDate).isEqualTo(time);
    assertThat(event.affectedComponentCount).isEqualTo(1);
    assertThat(event.criticalComponentCount).isEqualTo(3);
    assertThat(event.severeComponentCount).isEqualTo(5);
    assertThat(event.moderateComponentCount).isEqualTo(7);
    assertThat(event.outcome).isEqualTo(ApplicationEvaluationEvent.ACTION_ID_NONE);
    assertThat(event.initiator).isEqualTo(USERNAME);
    assertThat(event.reportId).isEqualTo("reportId");
    assertThat(event.commitHash).isEqualTo("commitHash");
    assertThat(event.application.id).isEqualTo(application.getId());
    assertThat(event.application.publicId).isEqualTo(application.getPublicId());
    assertThat(event.application.name).isEqualTo(application.getName());
    assertThat(event.application.organizationId).isEqualTo(application.getOrganizationId());

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_CalculateNoneOutcome() throws InterruptedException {
    final Application application = tempEntity.newApplicationWithParent();
    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setApplicationId(application.getId());
    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();

    // No action provides a ACTION_ID_NONE outcome
    TestEventHandler<ApplicationEvaluationEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), ApplicationEvaluationEvent.class);
    asyncEventBus.register(handler);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult, application);
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event.outcome).isEqualTo(ApplicationEvaluationEvent.ACTION_ID_NONE);
    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_CalculateWarnOutcome() throws InterruptedException {
    final Application application = tempEntity.newApplicationWithParent();
    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setApplicationId(application.getId());
    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();

    // Warn provides a ID_WARN outcome
    policyEvaluationResult.setAlerts(
        Collections.singletonList(new PolicyAlert(null, Collections.singletonList(new Action(Action.ID_WARN)))));

    TestEventHandler<ApplicationEvaluationEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), ApplicationEvaluationEvent.class);
    asyncEventBus.register(handler);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult, application);
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event.outcome).isEqualTo(Action.ID_WARN);
    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_CalculateFailOutcome() throws InterruptedException {
    final Application application = tempEntity.newApplicationWithParent();
    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setApplicationId(application.getId());
    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();

    // Fail and warn provides a ID_FAIL outcome
    policyEvaluationResult.setAlerts(Arrays
        .asList(new PolicyAlert(null, Collections.singletonList(new Action(Action.ID_WARN))),
            new PolicyAlert(null, Collections.singletonList(new Action(Action.ID_FAIL)))));

    TestEventHandler<ApplicationEvaluationEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), ApplicationEvaluationEvent.class);
    asyncEventBus.register(handler);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult, application);
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event.outcome).isEqualTo(Action.ID_FAIL);
    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_HandlesRuntimeException() {
    Application app = tempEntity.newApplicationWithParent();
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    applicationEvaluationEventService.postEvent(new PolicyEvaluation(), new PolicyEvaluationResult(), app);

    verify(subject, atLeastOnce()).getPrincipal();
  }
}
