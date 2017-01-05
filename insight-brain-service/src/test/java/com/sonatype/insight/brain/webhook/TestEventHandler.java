/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.concurrent.CountDownLatch;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;

import com.google.common.eventbus.Subscribe;

public class TestEventHandler<T extends WebhookEvent>
{
  private CountDownLatch latch;

  private T event;

  public TestEventHandler(final CountDownLatch latch) {
    this.latch = latch;
  }

  public CountDownLatch getLatch() {
    return latch;
  }

  public void setLatch(final CountDownLatch latch) {
    this.latch = latch;
  }

  @Subscribe
  public void handleEvent(final T event) {
    this.event = event;
    latch.countDown();
  }

  public T getEvent() {
    return event;
  }
}
