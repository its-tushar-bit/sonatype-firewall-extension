/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class LicenseOverrideEventServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private LicenseOverrideEventService licenseOverrideEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Test
  public void testPostEvent() throws InterruptedException {
    LicenseOverride override = new LicenseOverride();
    TestEventHandler<LicenseOverrideEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), LicenseOverrideEvent.class);
    asyncEventBus.register(handler);

    licenseOverrideEventService.postEvent(EventAction.CREATED, override);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    LicenseOverrideEvent event = handler.getEvent();
    assertThat(event.initiator).isEqualTo(USERNAME);
    assertThat(event.action).isEqualTo(EventAction.CREATED);
    assertThat(event.licenseOverride).isEqualTo(override);

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEvent_HandlesRuntimeException() {
    when(subject.getPrincipal()).thenThrow(new RuntimeException("testing"));

    licenseOverrideEventService.postEvent(EventAction.CREATED, new LicenseOverride());

    verify(subject, atLeastOnce()).getPrincipal();
  }
}
