/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.eventbus;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncEventBusDiscardPolicy
    extends DiscardPolicy
{
  private static final Logger log = LoggerFactory.getLogger(AsyncEventBusDiscardPolicy.class);

  @Override
  public void rejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) {
    log.error("Discarding event because the thread bounds and queue capacities are reached");
    super.rejectedExecution(runnable, executor);
  }
}
