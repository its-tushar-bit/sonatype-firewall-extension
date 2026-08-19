/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

public class ServerBootHealthCheckTest
{
  @BeforeEach
  public void setUp() {
    ServerBootHealthCheck.resetForTesting();
  }

  @AfterEach
  public void tearDown() {
    ServerBootHealthCheck.resetForTesting();
  }

  @Test
  public void testHealthCheck() throws Exception {
    ServerBootHealthCheck healthCheck = new ServerBootHealthCheck();
    assertThat(healthCheck.getName()).isEqualTo("server-boot");

    // default is unhealthy
    Health health = healthCheck.check();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);

    ServerBootHealthCheck.fullyBooted();

    health = healthCheck.check();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }
}
