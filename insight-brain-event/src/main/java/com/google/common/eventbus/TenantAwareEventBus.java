/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.google.common.eventbus;

import java.util.concurrent.Executor;

public class TenantAwareEventBus
    extends EventBus
{
  public TenantAwareEventBus(Executor executor, SubscriberExceptionHandler exceptionHandler) {
    super("default", executor, new TenantAwareDispatcher(), exceptionHandler);
  }
}
