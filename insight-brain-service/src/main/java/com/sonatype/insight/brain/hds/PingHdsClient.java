/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;
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
    extends DefaultHdsClient
{
  static final int SOCKET_TIMEOUT = 5000;

  static final int CONNECT_TIMEOUT = SOCKET_TIMEOUT;

  @Inject
  public PingHdsClient(
      InsightProxy proxy,
      ProductLicense productLicense,
      InsightConfig insightConfig,
      ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO,
      VersionService versionService,
      TelemetryId telemetryId)
  {
    super(proxy, productLicense, insightConfig, reverseProxyAuthenticationConfigurationDAO, versionService,
        telemetryId);
  }

  @Override
  protected void customizeConfiguration(Configuration configuration) {
    configuration.setSocketTimeout(SOCKET_TIMEOUT);
    configuration.setConnectTimeout(CONNECT_TIMEOUT);
  }
}
