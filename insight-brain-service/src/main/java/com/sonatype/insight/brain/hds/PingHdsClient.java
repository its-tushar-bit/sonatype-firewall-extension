/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils;

/**
 * Dedicated HTTP client with a short timeout for accessing HDS ping endpoint.
 *
 * @since 1.47
 */
@Named
@Singleton
public class PingHdsClient
    extends HdsClient
{
  static final int SOCKET_TIMEOUT = 5000;

  static final int CONNECT_TIMEOUT = SOCKET_TIMEOUT;

  @Inject
  public PingHdsClient(
      InsightProxy proxy,
      ProductLicense productLicense,
      Configuration configuration,
      VersionService versionService,
      TelemetryId telemetryId,
      CurrentUser currentUser)
  {
    super(proxy, productLicense, configuration, versionService, telemetryId, currentUser);
  }

  @Override
  protected void customizeConfiguration(HttpClientUtils.Configuration configuration) {
    configuration.setSocketTimeout(SOCKET_TIMEOUT);
    configuration.setConnectTimeout(CONNECT_TIMEOUT);
  }
}
