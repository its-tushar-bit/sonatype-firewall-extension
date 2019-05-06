/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiProxyConfigurationServiceV2;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.SimpleAuthentication;

@Named
@Singleton
public class InsightProxy
{
  private final InsightConfig insightConfig;

  private final ApiProxyConfigurationServiceV2 proxyConfigurationService;

  @Inject
  public InsightProxy(final InsightConfig insightConfig,
                      final ApiProxyConfigurationServiceV2 proxyConfigurationService)
  {
    this.insightConfig = insightConfig;
    this.proxyConfigurationService = proxyConfigurationService;
  }

  public <T extends HttpClientUtils.Configuration> T contextualize(final T httpConfig) {
    return contextualize(httpConfig, insightConfig.getHdsUrl());
  }

  public <T extends HttpClientUtils.Configuration> T contextualize(final T httpConfig, final String serverUrl) {
    httpConfig.setServerUrl(serverUrl);
    httpConfig.setUserAgent(httpConfig.getUserAgent() + " " + insightConfig.getUserAgentSuffix());

    final ProxyConfig proxyConfig = insightConfig.getProxyConfig();
    if (proxyConfig.getHostname() != null) {
      httpConfig.setProxyHost(proxyConfig.getHostname());
      httpConfig.setProxyPort(proxyConfig.getPort());
      httpConfig.setProxyExcludeHosts(proxyConfigurationService.get().getProxyExcludeHosts());
      if (proxyConfig.getUsername() != null) {
        final SimpleAuthentication proxyAuth = new SimpleAuthentication();
        proxyAuth.setUsername(proxyConfig.getUsername());
        proxyAuth.setPassword(proxyConfig.getPassword());
        // TODO: do we need to support NTLM?
        httpConfig.setProxyAuth(proxyAuth);
      }
    }

    return httpConfig;
  }
}
