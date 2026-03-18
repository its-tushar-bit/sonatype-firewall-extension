/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestSafeComponentsMetricEventServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(RequestSafeComponentsMetricEventService.class);

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private RequestSafeComponentsMetricEventService service;

  @Test
  public void testPostRequestSafeComponentsMetricEvent_eventIsPosted() throws Exception {
    TestEventHandler<RequestSafeComponentsAutoSelectMetricEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), RequestSafeComponentsAutoSelectMetricEvent.class);
    eventBus.register(handler);

    service.postRequestSafeComponentsMetricEvent();
    try {
      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      RequestSafeComponentsAutoSelectMetricEvent event = handler.getEvent();
      assertThat(event).isInstanceOfAny(RequestSafeComponentsAutoSelectMetricEvent.class);
    }
    finally {
      eventBus.unregister(handler);
    }
  }
}
