/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.operational.check.ExistingDbConnectionOperationalCheck;

import com.codahale.metrics.health.HealthCheck.Result;

@Named
@Singleton
public class ExistingDbConnectionAdminHealthCheckEndpoint
    implements AdminHealthCheckEndpoint
{
  private final ExistingDbConnectionOperationalCheck databaseOperationalCheck;

  @Inject
  public ExistingDbConnectionAdminHealthCheckEndpoint(
      final ExistingDbConnectionOperationalCheck databaseOperationalCheck)
  {
    this.databaseOperationalCheck = databaseOperationalCheck;
  }

  @Override
  public String getName() {
    return "Database";
  }

  @Override
  public String getPath() {
    return "/healthcheck/database";
  }

  @Override
  public HealthCheckResponse getHealthCheckResponse() {
    Result result = databaseOperationalCheck.execute();
    if (result.isHealthy()) {
      return new HealthCheckResponse(true);
    }

    String message = result.getDetails().toString();
    return new HealthCheckResponse(false, message);
  }
}
