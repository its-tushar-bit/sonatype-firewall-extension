/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import com.sonatype.insight.brain.client.FirewallClient;

public enum RepositoryManagerType
{
  NEXUS(FirewallClient.NEXUS_RESOURCE_PATH), ARTIFACTORY(FirewallClient.ARTIFACTORY_RESOURCE_PATH);

  RepositoryManagerType(final String resourcePath) {
    this.resourcePath = resourcePath;
  }

  public final String resourcePath;
}
