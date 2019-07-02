/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

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

  static int CONNECT_TIMEOUT = SOCKET_TIMEOUT;

  @Inject
  public PingHdsClient(InsightProxy proxy,
                       CLMLicenseManager licenseManager,
                       InsightConfig insightConfig,
                       VersionService versionService,
                       TelemetryId telemetryId)
  {
    super(proxy, licenseManager, insightConfig, versionService, telemetryId);
  }

  @Override
  protected Configuration createConfiguration(InsightConfig insightConfig) {
    Configuration configuration = new Configuration();
    configuration.setSocketTimeout(SOCKET_TIMEOUT);
    configuration.setConnectTimeout(CONNECT_TIMEOUT);
    return configuration;
  }
}
