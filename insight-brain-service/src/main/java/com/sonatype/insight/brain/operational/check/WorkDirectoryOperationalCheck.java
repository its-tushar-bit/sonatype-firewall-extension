/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;

/**
 * Dropwizard exposes health/operational checks on:
 * http://localhost:8071/healthcheck
 */
@Named
@Singleton
public class WorkDirectoryOperationalCheck
    extends AbstractOperationalCheck
{
  private final InsightConfig insightConfig;

  @Inject
  public WorkDirectoryOperationalCheck(final InsightConfig insightConfig) {
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
