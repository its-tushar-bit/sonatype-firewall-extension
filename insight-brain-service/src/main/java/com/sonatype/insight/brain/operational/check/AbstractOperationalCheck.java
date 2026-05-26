/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

/**
 * Base class for operational health checks.
 * Migrated from Dropwizard's NamedHealthCheck to Spring Boot Actuator's HealthIndicator.
 */
public abstract class AbstractOperationalCheck
    implements HealthIndicator
{
  private final String name;

  protected AbstractOperationalCheck(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * Execute the health check and return a Health result.
   * Subclasses should implement this.
   */
  public abstract Health check() throws Exception;

  /**
   * Implementation of {@link HealthIndicator#health()} that delegates to {@link #check()}.
   */
  @Override
  public Health health() {
    try {
      return check();
    }
    catch (Exception e) {
      return Health.down(e).build();
    }
  }

  /**
   * @deprecated Use {@link #check()} instead. Kept for backward compatibility with tests.
   */
  @Deprecated
  public Health execute() throws Exception {
    return check();
  }

  /**
   * Create a Health.Builder with unhealthy status and a message.
   */
  protected Health.Builder unhealthy(String message) {
    return Health.status(Status.DOWN).withDetail("message", message);
  }

  /**
   * Create a Health.Builder with healthy status.
   */
  protected Health.Builder healthy() {
    return Health.status(Status.UP);
  }
}
