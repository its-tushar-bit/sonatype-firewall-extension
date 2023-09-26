/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.google.common.eventbus;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.collect.Queues;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is a copy of com.google.common.eventbus.Dispatcher.LegacyAsyncDispatcher
 * https://github.com/google/guava/blob/v32.0.0/guava/src/com/google/common/eventbus/Dispatcher.java#L130-L177 with the
 * exception that the queue is changed to be per tenant
 */
public class TenantAwareDispatcher
    extends Dispatcher
{
  private final TenantReference<ConcurrentLinkedQueue<EventWithSubscriber>> queues =
      new TenantReference<>(Queues::newConcurrentLinkedQueue);

  @Override
  void dispatch(Object event, Iterator<Subscriber> subscribers) {
    checkNotNull(event);
    ConcurrentLinkedQueue<EventWithSubscriber> queue = queues.get();
    while (subscribers.hasNext()) {
      queue.add(new EventWithSubscriber(event, subscribers.next()));
    }

    EventWithSubscriber e;
    while ((e = queue.poll()) != null) {
      e.subscriber.dispatchEvent(e.event);
    }
  }

  public static final class EventWithSubscriber
  {
    private final Object event;

    private final Subscriber subscriber;

    EventWithSubscriber(Object event, Subscriber subscriber) {
      this.event = event;
      this.subscriber = subscriber;
    }
  }
}
