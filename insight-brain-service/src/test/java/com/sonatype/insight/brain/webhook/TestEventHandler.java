/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;

import com.google.common.eventbus.Subscribe;

public class TestEventHandler<T extends WebhookEvent>
{
  private CountDownLatch latch;

  private final List<T> events;

  public TestEventHandler(final CountDownLatch latch) {
    this.latch = latch;
    events = Collections.synchronizedList(new LinkedList<>());
  }

  public CountDownLatch getLatch() {
    return latch;
  }

  public void setLatch(final CountDownLatch latch) {
    this.latch = latch;
  }

  @Subscribe
  public void handleEvent(final T event) {
    events.add(0, event);
    latch.countDown();
  }

  public T getEvent() {
    return events.get(0);
  }

  public Collection<T> getAllEvents() {
    return new ArrayList<>(events);
  }

  public boolean isEmpty() {
    return events.isEmpty();
  }
}
