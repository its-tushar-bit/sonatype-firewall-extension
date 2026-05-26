/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import com.sonatype.insight.brain.service.InsightConfig;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.boot.health.contributor.Health;

/**
 * Dropwizard exposes health/operational checks on:
 * http://localhost:8071/healthcheck
 */
@Named
@Singleton
public class WorkDirectoriesOperationalCheck
    extends AbstractOperationalCheck
{
  private final InsightConfig insightConfig;

  @Inject
  public WorkDirectoriesOperationalCheck(final InsightConfig insightConfig) {
    super("work-directory");
    this.insightConfig = insightConfig;
  }

  @Override
  public Health check() {
    if (!insightConfig.getSonatypeWork().isDirectory()) {
      return Health.down().withDetail("message", insightConfig.getSonatypeWork() + " is not a directory").build();
    }
    if (!insightConfig.getClusterDirectory().isDirectory()) {
      return Health.down().withDetail("message", insightConfig.getClusterDirectory() + " is not a directory").build();
    }
    return Health.up().build();
  }
}
