/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

public class ServerBootHealthCheckTest
{
  @Before
  public void setUp() {
    ServerBootHealthCheck.resetForTesting();
  }

  @After
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
