/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LicenseOverrideEventServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LicenseOverrideEventService licenseOverrideEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Test
  public void testPostEvent() throws InterruptedException {
    LicenseOverride override = new LicenseOverride();
    TestEventHandler<LicenseOverrideEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    asyncEventBus.register(handler);

    licenseOverrideEventService.postEvent(EventAction.CREATED, override);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    LicenseOverrideEvent event = handler.getEvent();
    assertThat(event.initiator, is(USERNAME));
    assertThat(event.action, is(EventAction.CREATED));
    assertThat(event.licenseOverride, is(override));

    asyncEventBus.unregister(handler);
  }
}
