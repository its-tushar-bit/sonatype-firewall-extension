/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.health;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Database health indicator for Spring Boot Actuator.
 */
@Named
@Singleton
public class DatabaseHealthIndicator
    implements HealthIndicator
{

  private final DataSource dataSource;

  @Inject
  public DatabaseHealthIndicator(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Health health() {
    try (Connection connection = dataSource.getConnection()) {
      if (connection.isValid(1)) {
        return Health.up().withDetail("database", "available").build();
      }
      return Health.down().withDetail("database", "connection invalid").build();
    }
    catch (SQLException e) {
      return Health.down(e).withDetail("database", "connection failed").build();
    }
  }
}
