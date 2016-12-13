/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.LicenseThreatGroupEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;

import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LicenseThreatGroupServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LicenseThreatGroupService service;

  @Inject
  private AsyncEventBus eventBus;

  @Test
  public void testAddUpdateAndDeleteLicenseThreatGroupPostsEvents() throws Exception {
    TestEventHandler<LicenseThreatGroupEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup ltg = new LicenseThreatGroup(organization.getId(), "EvilThings", 5);

    LicenseThreatGroup created = service.addLicenseThreatGroup(organization.getId(), ltg);

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(CREATED));
    assertThat(handler.getEvent().ownerId, is(organization.getId()));
    assertThat(handler.getEvent().licenseThreatGroup.getId(), is(created.getId()));

    handler.setLatch(new CountDownLatch(1));

    created.setName("some new threat");
    service.updateLicenseThreatGroup(ORGANIZATION, organization.getId(), created);

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(UPDATED));
    assertThat(handler.getEvent().ownerId, is(organization.getId()));
    assertThat(handler.getEvent().licenseThreatGroup.getId(), is(created.getId()));

    handler.setLatch(new CountDownLatch(1));

    service.deleteLicenseThreatGroup(ORGANIZATION, organization.getId(), created.getId());

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(DELETED));
    assertThat(handler.getEvent().ownerId, is(organization.getId()));
    assertThat(handler.getEvent().licenseThreatGroup.getId(), is(created.getId()));

    eventBus.unregister(handler);
  }
}
