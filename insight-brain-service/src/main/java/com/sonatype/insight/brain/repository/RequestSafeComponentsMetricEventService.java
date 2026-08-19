/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;

@Named
@Singleton
public class RequestSafeComponentsMetricEventService
{
  private final AsyncEventBus eventBus;

  @Inject
  public RequestSafeComponentsMetricEventService(final AsyncEventBus eventBus) {
    this.eventBus = eventBus;
  }

  public void postRequestSafeComponentsMetricEvent() {
    eventBus.post(new RequestSafeComponentsAutoSelectMetricEvent());
  }
}
