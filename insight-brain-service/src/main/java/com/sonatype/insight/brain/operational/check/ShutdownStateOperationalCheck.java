/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.boot.health.contributor.Health;

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
  public Health check() throws Exception {
    if (shutdownHandler.isTriggered()) {
      return Health.down().withDetail("message", "Shutdown has been triggered").build();
    }
    return Health.up().build();
  }
}
