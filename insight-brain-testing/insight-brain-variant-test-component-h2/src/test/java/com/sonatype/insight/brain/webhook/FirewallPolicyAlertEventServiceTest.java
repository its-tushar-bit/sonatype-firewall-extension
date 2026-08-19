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

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.webhook.dto.FirewallPolicyAlertViolationDTO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.insight.brain.model.policy.actions.NotifyActionType.TARGET_TYPE_ROLE;
import static com.sonatype.insight.brain.model.policy.actions.NotifyActionType.TARGET_TYPE_WEBHOOK;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FirewallPolicyAlertEventService} (NEXUS-52728).
 */
@ComponentH2Test
public class FirewallPolicyAlertEventServiceTest
    extends AbstractComponentH2Test
{
  private static final String COMPONENT_HASH = "abc123";

  private static final String COMPONENT_PATHNAME = "com/example/lib-1.0.jar";

  @Inject
  private FirewallPolicyAlertEventService firewallPolicyAlertEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  private TestEventHandler<FirewallPolicyAlertEvent> handler;

  private Repository repository;

  @BeforeEach
  public void before() {
    repository = tempEntity.newRepository();
  }

  @AfterEach
  public void after() {
    if (handler != null) {
      asyncEventBus.unregister(handler);
    }
  }

  @Test
  public void postEvent_withWebhookTarget_postsEvent() throws InterruptedException {
    handler = registerHandler(1);

    Date quarantineTime = new Date();
    List<PolicyAlert> alerts = singletonList(createAlertWithWebhookTarget("webhook-1"));

    firewallPolicyAlertEventService.postEvent(repository, COMPONENT_PATHNAME, COMPONENT_HASH, quarantineTime, alerts);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    FirewallPolicyAlertEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.targetId).isEqualTo("webhook-1");
    assertThat(event.repositoryId).isEqualTo(repository.getId());
    assertThat(event.repositoryPublicId).isEqualTo(repository.getPublicId());
    assertThat(event.repositoryFormat).isEqualTo(repository.getFormat());
    assertThat(event.quarantineTime).isEqualTo(quarantineTime);
    assertThat(event.violations).hasSize(1);

    FirewallPolicyAlertViolationDTO violation = event.violations.get(0);
    assertThat(violation.policyId).isEqualTo("policy-1");
    assertThat(violation.policyName).isEqualTo("Policy One");
    assertThat(violation.threatLevel).isEqualTo(7);
    assertThat(violation.policyViolationId).isEqualTo("pv-1");
    assertThat(violation.componentFacts).hasSize(1);
    assertThat(violation.componentFacts.get(0).hash).isEqualTo(COMPONENT_HASH);
  }

  @Test
  public void postEvent_withoutWebhookTarget_postsNothing() throws InterruptedException {
    handler = registerHandler(1);

    List<PolicyAlert> alerts = singletonList(createAlertWithRoleTarget("role-1"));

    firewallPolicyAlertEventService.postEvent(repository, COMPONENT_PATHNAME, COMPONENT_HASH, new Date(), alerts);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  public void postEvent_multipleWebhookTargets_postsOneEventPerTarget() throws InterruptedException {
    handler = registerHandler(3);

    List<PolicyAlert> alerts = Arrays.asList(
        createAlertWithWebhookTarget("webhook-A", "policy-A", "pv-A"),
        createAlertWithWebhookTarget("webhook-B", "policy-B", "pv-B"),
        createAlertWithWebhookTarget("webhook-C", "policy-C", "pv-C"));

    firewallPolicyAlertEventService.postEvent(repository, COMPONENT_PATHNAME, COMPONENT_HASH, new Date(), alerts);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(handler.getAllEvents()).hasSize(3);
    assertThat(handler.getAllEvents().stream().map(e -> e.targetId))
        .containsExactlyInAnyOrder("webhook-A", "webhook-B", "webhook-C");
  }

  @Test
  public void postEvent_sameTargetMultipleAlerts_groupsViolations() throws InterruptedException {
    handler = registerHandler(1);

    List<PolicyAlert> alerts = Arrays.asList(
        createAlertWithWebhookTarget("webhook-1", "policy-1", "pv-1"),
        createAlertWithWebhookTarget("webhook-1", "policy-2", "pv-2"));

    firewallPolicyAlertEventService.postEvent(repository, COMPONENT_PATHNAME, COMPONENT_HASH, new Date(), alerts);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    FirewallPolicyAlertEvent event = handler.getEvent();
    assertThat(event.targetId).isEqualTo("webhook-1");
    assertThat(event.violations).hasSize(2);
    assertThat(event.violations.stream().map(v -> v.policyId))
        .containsExactlyInAnyOrder("policy-1", "policy-2");
  }

  @Test
  public void postEvent_alertWithBothWebhookAndRoleActions_postsForWebhookOnly() throws InterruptedException {
    handler = registerHandler(1);

    PolicyAlert mixed = new PolicyAlert(
        createPolicyFact("policy-1", "pv-1"),
        Arrays.asList(
            new Action(Action.ID_FAIL, "webhook-1", TARGET_TYPE_WEBHOOK),
            new Action(Action.ID_NOTIFY, "role-1", TARGET_TYPE_ROLE)));

    firewallPolicyAlertEventService.postEvent(repository, COMPONENT_PATHNAME, COMPONENT_HASH, new Date(),
        singletonList(mixed));

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(handler.getAllEvents()).hasSize(1);
    assertThat(handler.getEvent().targetId).isEqualTo("webhook-1");
  }

  @Test
  public void postEvent_componentFactNotMatchingPersistedHash_skipsThatComponent() throws InterruptedException {
    handler = registerHandler(1);

    // createPolicyFact adds one ComponentFact with COMPONENT_HASH/COMPONENT_PATHNAME.
    // Add a second unrelated ComponentFact with a different hash; it must be skipped.
    PolicyFact policyFact = createPolicyFact("policy-1", "pv-1");
    ComponentIdentifier unrelatedId = createMavenCoordinates("com.example", "other", "2.0", "test", "jar");
    ComponentFact unrelatedFact = new ComponentFact(unrelatedId, "different-hash");
    unrelatedFact.addPathnames(singletonList("com/example/other-2.0.jar"));
    policyFact.addComponentFact(unrelatedFact);

    PolicyAlert alert = new PolicyAlert(policyFact,
        singletonList(new Action(Action.ID_FAIL, "webhook-1", TARGET_TYPE_WEBHOOK)));

    firewallPolicyAlertEventService.postEvent(repository, COMPONENT_PATHNAME, COMPONENT_HASH, new Date(),
        singletonList(alert));

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    FirewallPolicyAlertEvent event = handler.getEvent();
    assertThat(event.violations).hasSize(1);
    // Only the matching ComponentFact is included; the different-hash one is filtered out.
    assertThat(event.violations.get(0).componentFacts).hasSize(1);
    assertThat(event.violations.get(0).componentFacts.get(0).hash).isEqualTo(COMPONENT_HASH);
  }

  @Test
  public void postEvent_emptyAlerts_postsNothing() throws InterruptedException {
    handler = registerHandler(1);

    firewallPolicyAlertEventService.postEvent(repository, COMPONENT_PATHNAME, COMPONENT_HASH, new Date(),
        Collections.emptyList());

    assertThat(handler.getLatch().await(500, TimeUnit.MILLISECONDS)).isFalse();
  }

  private TestEventHandler<FirewallPolicyAlertEvent> registerHandler(int expectedEventCount) {
    TestEventHandler<FirewallPolicyAlertEvent> h =
        new TestEventHandler<>(new CountDownLatch(expectedEventCount), FirewallPolicyAlertEvent.class);
    asyncEventBus.register(h);
    return h;
  }

  private PolicyAlert createAlertWithWebhookTarget(String webhookId) {
    return createAlertWithWebhookTarget(webhookId, "policy-1", "pv-1");
  }

  private PolicyAlert createAlertWithWebhookTarget(String webhookId, String policyId, String policyViolationId) {
    PolicyFact policyFact = createPolicyFact(policyId, policyViolationId);
    return new PolicyAlert(policyFact,
        singletonList(new Action(Action.ID_FAIL, webhookId, TARGET_TYPE_WEBHOOK)));
  }

  private PolicyAlert createAlertWithRoleTarget(String roleId) {
    PolicyFact policyFact = createPolicyFact("policy-1", "pv-1");
    return new PolicyAlert(policyFact,
        singletonList(new Action(Action.ID_NOTIFY, roleId, TARGET_TYPE_ROLE)));
  }

  private PolicyFact createPolicyFact(String policyId, String policyViolationId) {
    PolicyFact policyFact = new PolicyFact(policyId, "Policy One", 7, policyViolationId);
    ComponentIdentifier id = createMavenCoordinates("com.example", "lib", "1.0", "test", "jar");
    ComponentFact componentFact = new ComponentFact(id, COMPONENT_HASH);
    componentFact.addPathnames(singletonList(COMPONENT_PATHNAME));
    policyFact.addComponentFact(componentFact);
    return policyFact;
  }
}
