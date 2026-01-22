/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterDirectoryAdminHealthCheckEndpointTest
    extends AbstractComponentTest
{
  @Inject
  private ClusterDirectoryAdminHealthCheckEndpoint clusterDirectoryAdminHealthCheckEndpoint;

  @Test
  public void testGetName() {
    assertThat(clusterDirectoryAdminHealthCheckEndpoint.getName()).isEqualTo("Cluster Directory");
  }

  @Test
  public void testGetPath() {
    assertThat(clusterDirectoryAdminHealthCheckEndpoint.getPath()).isEqualTo("/healthcheck/clusterDirectory");
  }
}
