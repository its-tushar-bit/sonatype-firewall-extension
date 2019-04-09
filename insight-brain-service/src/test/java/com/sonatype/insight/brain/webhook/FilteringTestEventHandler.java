/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;

import com.google.common.eventbus.Subscribe;

import static java.util.Optional.empty;

public class FilteringTestEventHandler<T extends WebhookEvent>
{
  private final Predicate<? super WebhookEvent> filter;

  private final TestEventHandler<T> delegateHandler;

  public FilteringTestEventHandler(final CountDownLatch latch, final Predicate<? super WebhookEvent> filter) {
    this.delegateHandler = new TestEventHandler<>(latch);
    this.filter = filter;
  }

  @Subscribe
  public  void handleEvent(final T event) {
    Optional.ofNullable(event)
        .filter(filter)
        .ifPresent(delegateHandler::handleEvent);
  }

  public Optional<T> waitForEvent(final Duration duration) throws InterruptedException {
    return delegateHandler.getLatch().await(duration.toMillis(), TimeUnit.MILLISECONDS)
        ? Optional.of(delegateHandler.getEvent())
        : empty();
  }

  public T getEvent() {
    return delegateHandler.getEvent();
  }
}
