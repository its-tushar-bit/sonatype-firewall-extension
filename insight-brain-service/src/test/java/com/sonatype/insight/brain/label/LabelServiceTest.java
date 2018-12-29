/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.LabelEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;

import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class LabelServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LabelService labelService;

  @Inject
  private AsyncEventBus eventBus;

  @Test
  public void testAddUpdateAndDeleteLabelPostsEvents() throws Exception {
    TestEventHandler<LabelEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    Label label = new Label(organization.getId(), "LABEL", "test label", Color.yellow);

    Label created = labelService.addLabel(ORGANIZATION, organization.getId(), label);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(CREATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().label.getId()).isEqualTo(label.getId());

    handler.setLatch(new CountDownLatch(1));

    created.setDescription("some new description");
    labelService.updateLabel(ORGANIZATION, organization.getId(), created);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().label.getId()).isEqualTo(label.getId());

    handler.setLatch(new CountDownLatch(1));

    labelService.deleteLabel(ORGANIZATION, organization.getId(), created.getId());

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(DELETED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().label.getId()).isEqualTo(label.getId());

    eventBus.unregister(handler);
  }
}
