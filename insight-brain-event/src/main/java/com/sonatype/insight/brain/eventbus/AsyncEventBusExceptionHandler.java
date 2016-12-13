/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.eventbus;

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AsyncEventBusExceptionHandler
    implements SubscriberExceptionHandler
{
  private final Logger log = LoggerFactory.getLogger(AsyncEventBusExceptionHandler.class);

  @Override
  public void handleException(final Throwable exception, final SubscriberExceptionContext context) {
    if (exception instanceof Error) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      exception.printStackTrace();
      log.error(exception.getMessage(), exception);
      System.exit(2);
    }

    log.error("Could not dispatch event {} to subscriber {} method [{}]",
        context.getEvent(), context.getSubscriber(), context.getSubscriberMethod(), exception);
  }
}
