/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@Named
@Singleton
public class NewDbConnectionAdminHealthCheckEndpoint
    implements AdminHealthCheckEndpoint
{
  private final NewDbConnectionOperationalCheck databaseOperationalCheck;

  @Inject
  public NewDbConnectionAdminHealthCheckEndpoint(
      final NewDbConnectionOperationalCheck databaseOperationalCheck)
  {
    this.databaseOperationalCheck = databaseOperationalCheck;
  }

  @Override
  public String getName() {
    return "NewDatabaseConnections";
  }

  @Override
  public String getPath() {
    return "/healthcheck/newDatabaseConnections";
  }

  @Override
  public HealthCheckResponse getHealthCheckResponse() {
    try {
      Health health = databaseOperationalCheck.check();
      if (health.getStatus() == Status.UP) {
        return new HealthCheckResponse(true);
      }

      String message = health.getDetails().toString();
      return new HealthCheckResponse(false, message);
    }
    catch (Exception e) {
      return new HealthCheckResponse(false, e.getMessage());
    }
  }
}
