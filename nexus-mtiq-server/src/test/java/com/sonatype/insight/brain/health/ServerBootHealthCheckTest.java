/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerBootHealthCheckTest
{
  @Test
  public void testHealthCheck() throws Exception {
    ServerBootHealthCheck healthCheck = new ServerBootHealthCheck();
    assertThat(healthCheck.getName()).isEqualTo("server-boot");

    // default is unhealthy
    assertThat(healthCheck.check().isHealthy()).isFalse();

    ServerBootHealthCheck.fullyBooted();

    assertThat(healthCheck.check().isHealthy()).isTrue();
  }
}
