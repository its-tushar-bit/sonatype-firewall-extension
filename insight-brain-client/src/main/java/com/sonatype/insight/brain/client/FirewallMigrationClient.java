/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

/**
 * @since 1.28
 */
public class FirewallMigrationClient
    extends AbstractRequestClient
{
  private static final String RESOURCE_PATH = "rest/integration/repositories/migration";

  private static final String SUPPORTED_PATH = "supported";

  public FirewallMigrationClient(final Configuration config) {
    super(config);
  }

  public void verifyMigrationSupport(final String protocolVersion) throws IOException {
    verifyStatusCode(getRequest(path(RESOURCE_PATH, SUPPORTED_PATH, protocolVersion)));
  }
}
