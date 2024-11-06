/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.shutdown.ShutdownHandler;

/**
 * @since 1.109
 */
@Named
@Singleton
public class ShutdownStateOperationalCheck
    extends AbstractOperationalCheck
{
  private final ShutdownHandler shutdownHandler;

  @Inject
  public ShutdownStateOperationalCheck(ShutdownHandler shutdownHandler) {
    super("shutdown-state");
    this.shutdownHandler = shutdownHandler;
  }

  @Override
  protected Result check() throws Exception {
    if (shutdownHandler.isTriggered()) {
      return Result.unhealthy("Shutdown has been triggered");
    }
    return Result.healthy();
  }
}
