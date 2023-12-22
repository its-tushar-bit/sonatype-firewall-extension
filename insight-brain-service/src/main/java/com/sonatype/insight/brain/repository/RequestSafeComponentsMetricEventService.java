/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class RequestSafeComponentsMetricEventService
{
  private static final Logger log = LoggerFactory
      .getLogger(RequestSafeComponentsMetricEventService.class);

  private final AsyncEventBus eventBus;

  @Inject
  public RequestSafeComponentsMetricEventService(final AsyncEventBus eventBus) {
    this.eventBus = eventBus;
  }

  public void postRequestSafeComponentsMetricEvent(int policyCompliantVersionCount) {
    if (policyCompliantVersionCount <= 0) {
      log.debug("Skipping processing as Policy Compliant Version Count is 0");
      return;
    }
    eventBus.post(new RequestSafeComponentsAutoSelectMetricEvent());
  }
}
