/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.policy.actions.NotifyActionType.TARGET_TYPE_ROLE;
import static com.sonatype.insight.brain.model.policy.actions.NotifyActionType.TARGET_TYPE_WEBHOOK;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class PolicyAlertEventServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private PolicyAlertEventService policyAlertEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  private TestEventHandler<PolicyAlertEvent> handler;

  private Application application;

  private Organization organization;

  @BeforeEach
  public void before() {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
  }

  @AfterEach
  public void after() {
    if (handler != null) {
      asyncEventBus.unregister(handler);
    }
  }

  @Test
  public void testPostEvent_webhook() throws InterruptedException {
    // evaluation result with webhook alerts
    final Date time = new Date();
    final PolicyEvaluation policyEvaluation = createPolicyEvaluation(time);
    final List<PolicyAlert> alerts = createAlerts(TARGET_TYPE_WEBHOOK);
    final PolicyEvaluationResult policyEvaluationResult = createPolicyEvaluationResult(alerts);
    handler = new TestEventHandler<>(new CountDownLatch(1), PolicyAlertEvent.class);
    asyncEventBus.register(handler);

    policyAlertEventService
        .postEvent(policyEvaluation, policyEvaluationResult, application, Collections.emptyList(),
            Collections.emptyList());

    // policyAlertEvent is posted
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    PolicyAlertEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.applicationEvaluation.policyEvaluationId).isEqualTo(policyEvaluation.getId());
    assertThat(event.applicationEvaluation.stageTypeId).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(event.applicationEvaluation.ownerId).isEqualTo(policyEvaluation.getOwnerId());
    assertThat(event.applicationEvaluation.isForLatestScan).isEqualTo(!policyEvaluation.isForObsoleteScan());
    assertThat(event.applicationEvaluation.evaluationDate).isEqualTo(time);
    assertThat(event.applicationEvaluation.affectedComponentCount).isEqualTo(1);
    assertThat(event.applicationEvaluation.criticalComponentCount).isEqualTo(3);
    assertThat(event.applicationEvaluation.severeComponentCount).isEqualTo(5);
    assertThat(event.applicationEvaluation.moderateComponentCount).isEqualTo(7);
    assertThat(event.applicationEvaluation.outcome).isEqualTo(Action.ID_FAIL);
    assertThat(event.applicationEvaluation.commitHash).isEqualTo("commitHash");
    assertThat(event.initiator).isEqualTo(USERNAME);
    assertThat(event.application.id).isEqualTo(policyEvaluation.getOwnerId());
    assertThat(event.application.publicId).isEqualTo(application.getPublicId());
    assertThat(event.application.name).isEqualTo(application.getName());
    assertThat(event.application.organizationId).isEqualTo(application.getOrganizationId());
  }

  @Test
  public void testPostEvent_WithoutWebhook() throws InterruptedException {
    // evaluation result without webhook alerts
    final Date time = new Date();
    final PolicyEvaluation policyEvaluation = createPolicyEvaluation(time);
    final List<PolicyAlert> alerts = createAlerts(TARGET_TYPE_ROLE);
    final PolicyEvaluationResult policyEvaluationResult = createPolicyEvaluationResult(alerts);
    handler = new TestEventHandler<>(new CountDownLatch(1), PolicyAlertEvent.class);
    asyncEventBus.register(handler);

    policyAlertEventService
        .postEvent(policyEvaluation, policyEvaluationResult, application, Collections.emptyList(),
            Collections.emptyList());

    // no event is posted
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  public void testPostEvent_GroupsWebhooks() throws InterruptedException {
    // evaluation result without webhook alerts
    final Date time = new Date();
    final PolicyEvaluation policyEvaluation = createPolicyEvaluation(time);
    final List<PolicyAlert> alerts = createAlerts(TARGET_TYPE_WEBHOOK, "target1", "target1", "target1");
    final PolicyEvaluationResult policyEvaluationResult = createPolicyEvaluationResult(alerts);
    handler = new TestEventHandler<>(new CountDownLatch(1), PolicyAlertEvent.class);
    asyncEventBus.register(handler);

    policyAlertEventService
        .postEvent(policyEvaluation, policyEvaluationResult, application, Collections.emptyList(),
            Collections.emptyList());

    // events for target1 are grouped into a single event
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    PolicyAlertEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.targetId).isEqualTo("target1");
    assertThat(event.policyFacts).hasSize(3); // 3 alerts grouped into same webhook post
  }

  @Test
  public void testPostEvent_PolicyViolationToMultipleWebhooks() throws InterruptedException {
    // evaluation result without webhook alerts
    final Date time = new Date();
    final PolicyEvaluation policyEvaluation = createPolicyEvaluation(time);
    final List<PolicyAlert> alerts = createAlerts(TARGET_TYPE_WEBHOOK, "target1", "target2", "target3");
    final PolicyEvaluationResult policyEvaluationResult = createPolicyEvaluationResult(alerts);
    handler = new TestEventHandler<>(new CountDownLatch(3), PolicyAlertEvent.class);
    asyncEventBus.register(handler);

    policyAlertEventService
        .postEvent(policyEvaluation, policyEvaluationResult, application, Collections.emptyList(),
            Collections.emptyList());

    // 3 events are received
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    PolicyAlertEvent event = handler.getEvent();
    assertThat(event).isNotNull();
  }

  @Test
  public void testPostEvent_NoViolationsWithFixedViolations() throws InterruptedException {
    // evaluation result without webhook alerts
    final Date time = new Date();
    final PolicyEvaluation policyEvaluation = createPolicyEvaluation(time);
    final List<PolicyAlert> alerts = createAlerts(TARGET_TYPE_WEBHOOK, "target1", "target2", "target3");
    final PolicyEvaluationResult policyEvaluationResult = createPolicyEvaluationResult(Collections.emptyList());
    handler = new TestEventHandler<>(new CountDownLatch(3), PolicyAlertEvent.class);
    asyncEventBus.register(handler);

    // Post event for eval with no active violations, but some fixed violations
    policyAlertEventService
        .postEvent(policyEvaluation, policyEvaluationResult, application, Collections.emptyList(),
            alerts);

    // Event without any policy facts is received
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    PolicyAlertEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.policyFacts).hasSize(0);
  }

  @Test
  public void testPostEvent_NoViolationsWithWaivedViolations() throws InterruptedException {
    // evaluation result without webhook alerts
    final Date time = new Date();
    final PolicyEvaluation policyEvaluation = createPolicyEvaluation(time);
    final List<PolicyAlert> alerts = createAlerts(TARGET_TYPE_WEBHOOK, "target1", "target2", "target3");
    final PolicyEvaluationResult policyEvaluationResult = createPolicyEvaluationResult(Collections.emptyList());
    handler = new TestEventHandler<>(new CountDownLatch(3), PolicyAlertEvent.class);
    asyncEventBus.register(handler);

    // Post event for eval with no active violations, but some waived violations
    policyAlertEventService
        .postEvent(policyEvaluation, policyEvaluationResult, application, alerts, Collections.emptyList());

    // Event without any policy facts is received
    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    PolicyAlertEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.policyFacts).hasSize(0);
  }

  private List<PolicyAlert> createAlerts(String targetType, String... targets) {
    return (targets.length > 0 ? Arrays.asList(targets) : singletonList("target")).stream()
        .map(target -> new PolicyAlert(null, singletonList(new Action(Action.ID_FAIL, target, targetType))))
        .collect(Collectors.toList());
  }

  private PolicyEvaluationResult createPolicyEvaluationResult(List<PolicyAlert> alerts) {
    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    policyEvaluationResult.setAffectedComponentCount(1);
    policyEvaluationResult.setCriticalComponentCount(3);
    policyEvaluationResult.setSevereComponentCount(5);
    policyEvaluationResult.setModerateComponentCount(7);
    policyEvaluationResult.setAlerts(alerts);
    return policyEvaluationResult;
  }

  private PolicyEvaluation createPolicyEvaluation(final Date time) {
    final PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setId("policyEvaluationId");
    policyEvaluation.setStageTypeId("stageTypeId");
    policyEvaluation.setOwnerId(application.getId());
    policyEvaluation.setTime(time);
    policyEvaluation.setCommitHash("commitHash");
    return policyEvaluation;
  }
}
