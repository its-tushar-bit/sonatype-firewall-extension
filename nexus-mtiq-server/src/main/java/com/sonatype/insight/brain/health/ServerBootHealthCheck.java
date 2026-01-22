/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.operational.check.AbstractOperationalCheck;

/**
 * A 'READY' health check to ensure the app is fully 'booted' before considered ready. Note there can be a delay between
 * when the port (8070/8071) is up (which happens in the DropWizard class `ServerCommand#run()`) and when the app has
 * truly completed all the boot logic. So the port is not enough for K8S to send traffic.
 * <p>
 * Future note: this health check can be moved higher up to on-prem if needed.
 */
@Named
@Singleton
public class ServerBootHealthCheck
    extends AbstractOperationalCheck
{
  private static volatile boolean fullyBooted = false;

  public ServerBootHealthCheck() {
    super("server-boot");
  }

  public static void fullyBooted() {
    ServerBootHealthCheck.fullyBooted = true;
  }

  @Override
  protected Result check() throws Exception {
    ResultBuilder resultBuilder = Result.builder();
    if (!fullyBooted) {
      resultBuilder.unhealthy();
    }
    return resultBuilder.build();
  }
}
