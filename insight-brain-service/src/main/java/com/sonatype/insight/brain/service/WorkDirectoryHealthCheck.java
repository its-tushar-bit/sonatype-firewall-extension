/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * This is a Dropwizard health check. It has nothing to do with Insight's Health Check features like "Application Health
 * Check" or "Repository Health Check". :)
 */
@Named
@Singleton
public class WorkDirectoryHealthCheck
    extends AbstractOperationalCheck
{
  private final InsightConfig insightConfig;

  @Inject
  public WorkDirectoryHealthCheck(final InsightConfig insightConfig) {
    super("work-directory");
    this.insightConfig = insightConfig;
  }

  @Override
  protected Result check() throws Exception {
    if (!insightConfig.getSonatypeWork().isDirectory()) {
      return Result.unhealthy(insightConfig.getSonatypeWork() + " is not a directory");
    }
    return Result.healthy();
  }
}
