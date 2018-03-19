/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.LabelEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.LicenseThreatGroupEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.PolicyEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.RoleEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.TagEvent;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ManagementEventServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ManagementEventService managementEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  private Organization organization;

  @Before
  public void before() {
    organization = new Organization();
    organization.setId("organizationId");
  }

  @Test
  public void testPostEvent_Tag() throws InterruptedException {
    Tag tag = new Tag();
    tag.setOrganizationId(organization.getId());

    TestEventHandler<TagEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    managementEventService.postEvent(EventAction.CREATED, tag);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    TagEvent event = handler.getEvent();
    assertThat(event.initiator, is(USERNAME));
    assertThat(event.ownerId, is(organization.getId()));
    assertThat(event.action, is(EventAction.CREATED));
    assertThat(event.tag, is(tag));

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_Tag_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    managementEventService.postEvent(EventAction.CREATED, new Tag());

    verify(subject, atLeastOnce()).getPrincipal();
  }

  @Test
  public void testPostEvent_Label() throws InterruptedException {
    Label label = new Label();
    label.setOwnerId(organization.getId());

    TestEventHandler<LabelEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    managementEventService.postEvent(EventAction.CREATED, label);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    LabelEvent event = handler.getEvent();
    assertThat(event.initiator, is(USERNAME));
    assertThat(event.ownerId, is(organization.getId()));
    assertThat(event.action, is(EventAction.CREATED));
    assertThat(event.label, is(label));

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_Label_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    managementEventService.postEvent(EventAction.CREATED, new Label());

    verify(subject, atLeastOnce()).getPrincipal();
  }

  @Test
  public void testPostEvent_LicenseThreatGroup() throws InterruptedException {
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup();
    licenseThreatGroup.setOwnerId(organization.getId());

    TestEventHandler<LicenseThreatGroupEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    managementEventService.postEvent(EventAction.CREATED, licenseThreatGroup);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    LicenseThreatGroupEvent event = handler.getEvent();
    assertThat(event.initiator, is(USERNAME));
    assertThat(event.ownerId, is(organization.getId()));
    assertThat(event.action, is(EventAction.CREATED));
    assertThat(event.licenseThreatGroup, is(licenseThreatGroup));

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_LicenseThreatGroup_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    managementEventService.postEvent(EventAction.CREATED, new LicenseThreatGroup());

    verify(subject, atLeastOnce()).getPrincipal();
  }

  @Test
  public void testPostEvent_Application() throws InterruptedException {
    Application application = new Application();
    application.setOrganizationId(organization.getId());

    TestEventHandler<OwnerEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    managementEventService.postEvent(EventAction.CREATED, application);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    OwnerEvent event = handler.getEvent();
    assertThat(event.initiator, is(USERNAME));
    assertThat(event.ownerId, is(application.getId()));
    assertThat(event.action, is(EventAction.CREATED));
    assertThat(event.owner, is((Owner) application));

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_Application_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    managementEventService.postEvent(EventAction.CREATED, new Application());

    verify(subject, atLeastOnce()).getPrincipal();
  }

  @Test
  public void testPostEvent_Organization() throws InterruptedException {
    TestEventHandler<OwnerEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    managementEventService.postEvent(EventAction.CREATED, organization);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    OwnerEvent event = handler.getEvent();
    assertThat(event.initiator, is(USERNAME));
    assertThat(event.ownerId, is(organization.getId()));
    assertThat(event.action, is(EventAction.CREATED));
    assertThat(event.owner, is((Owner) organization));

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_Organization_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    managementEventService.postEvent(EventAction.CREATED, new Organization());

    verify(subject, atLeastOnce()).getPrincipal();
  }

  @Test
  public void testPostEvent_Member() throws InterruptedException {
    TestEventHandler<RoleEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    Map<String, List<Member>> roleIdToMemberMap = new HashMap<>();

    managementEventService.postEvent(EventAction.CREATED, roleIdToMemberMap, organization.getId());

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    RoleEvent event = handler.getEvent();
    assertThat(event.initiator, is(USERNAME));
    assertThat(event.ownerId, is(organization.getId()));
    assertThat(event.action, is(EventAction.CREATED));
    assertThat(event.roleIdToMemberMap, is(roleIdToMemberMap));

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_Member_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    managementEventService.postEvent(EventAction.CREATED, new HashMap<>(), organization.getId());

    verify(subject, atLeastOnce()).getPrincipal();
  }

  @Test
  public void testPostEvent_Policy() throws InterruptedException {
    Policy policy = new Policy();
    policy.setOwnerId(organization.getId());

    TestEventHandler<PolicyEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    managementEventService.postEvent(EventAction.CREATED, policy);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    PolicyEvent event = handler.getEvent();
    assertThat(event.initiator, is(USERNAME));
    assertThat(event.ownerId, is(organization.getId()));
    assertThat(event.action, is(EventAction.CREATED));
    assertThat(event.policy, is(policy));

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_Policy_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    managementEventService.postEvent(EventAction.CREATED, new Policy());

    verify(subject, atLeastOnce()).getPrincipal();
  }
}
