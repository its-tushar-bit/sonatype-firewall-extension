/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * Dedicated HTTP client for user telemetry requests. Unlike the standard HdsClient the User-Agent of requests is
 * propagated for Pendo.
 *
 * @since 1.50
 */
@Named
@Singleton
public class UserTelemetryHdsClient extends HdsClient
{
  @Inject
  public UserTelemetryHdsClient(InsightProxy proxy,
                                CLMLicenseManager licenseManager,
                                InsightConfig insightConfig,
                                VersionService versionService,
                                TelemetryId telemetryId)
  {
    super(proxy, licenseManager, insightConfig, versionService, telemetryId);
  }

  @Override
  protected void populateUserAgents(HttpServletRequest orig, HttpUriRequest req) {
    String userAgent = orig != null ? getClientUserAgent(orig) : config.getUserAgent();
    req.setHeader(HttpHeaders.USER_AGENT, userAgent);
  }
}
