/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;

import com.google.common.eventbus.Subscribe;

public class TestEventHandler<T extends WebhookEvent>
{
  private CountDownLatch latch;

  private final List<T> events = Collections.synchronizedList(new LinkedList<>());

  private final Class<?> eventClass;

  /**
   * The java compiler erases all info about parameterized types, so, at runtime, there is no information about the type
   * of event this class subscribes to.
   * Without any event type information available at runtime, the event bus will send all events to this subscriber,
   * which is undesired.
   *
   * The eventClass parameter was added to allow us to filter out undesired events in the handleEvent method. It must
   * match the class of the T parameter.
   */
  public TestEventHandler(final CountDownLatch latch, Class<?> eventClass) {
    this.latch = latch;
    this.eventClass = eventClass;
  }

  public CountDownLatch getLatch() {
    return latch;
  }

  public void setLatch(final CountDownLatch latch) {
    this.latch = latch;
  }

  @Subscribe
  public void handleEvent(final T event) {
    if (!eventClass.isAssignableFrom(event.getClass())) {
      return;
    }
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
