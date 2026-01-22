/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;

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
  protected Result check() {
    if (!insightConfig.getSonatypeWork().isDirectory()) {
      return Result.unhealthy(insightConfig.getSonatypeWork() + " is not a directory");
    }
    if (!insightConfig.getClusterDirectory().isDirectory()) {
      return Result.unhealthy(insightConfig.getClusterDirectory() + " is not a directory");
    }
    return Result.healthy();
  }
}
