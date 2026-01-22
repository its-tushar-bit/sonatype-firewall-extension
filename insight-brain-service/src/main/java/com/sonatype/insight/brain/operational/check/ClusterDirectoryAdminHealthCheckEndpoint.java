/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;

@Named
@Singleton
public class ClusterDirectoryAdminHealthCheckEndpoint
    extends DirectoryAdminHealthCheckEndpoint
{
  @Inject
  public ClusterDirectoryAdminHealthCheckEndpoint(final InsightConfig insightConfig) {
    super("Cluster Directory", "/healthcheck/clusterDirectory", insightConfig.getClusterDirectory());
  }
}
