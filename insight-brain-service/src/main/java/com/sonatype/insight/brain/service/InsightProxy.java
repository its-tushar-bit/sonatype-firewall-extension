/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.SimpleAuthentication;

@Named
@Singleton
public class InsightProxy
{
  private final InsightConfig insightConfig;

  private final ProxyConfigurationDAO proxyConfigurationDAO;

  private final PasswordHandler passwordHandler;

  @Inject
  public InsightProxy(
      InsightConfig insightConfig,
      ProxyConfigurationDAO proxyConfigurationDAO,
      PasswordHandler passwordHandler)
  {
    this.insightConfig = insightConfig;
    this.proxyConfigurationDAO = proxyConfigurationDAO;
    this.passwordHandler = passwordHandler;
  }

  public <T extends HttpClientUtils.Configuration> T contextualize(final T httpConfig) {
    return contextualize(httpConfig, insightConfig.getHdsUrl());
  }

  public <T extends HttpClientUtils.Configuration> T contextualize(final T httpConfig, final String serverUrl) {
    httpConfig.setServerUrl(serverUrl);
    httpConfig.setUserAgent(httpConfig.getUserAgent() + " " + insightConfig.getUserAgentSuffix());

    ProxyConfiguration proxyConfig = proxyConfigurationDAO.get();
    if (proxyConfig != null) {
      httpConfig.setProxyHost(proxyConfig.getHostname());
      httpConfig.setProxyPort(proxyConfig.getPort());
      httpConfig.setProxyExcludeHosts(proxyConfig.getExcludeHostsList());
      if (proxyConfig.getUsername() != null) {
        final SimpleAuthentication proxyAuth = new SimpleAuthentication();
        proxyAuth.setUsername(proxyConfig.getUsername());

        char[] password = passwordHandler.decryptPassword(proxyConfig.getPassword());
        proxyAuth.setPassword(password);
        if (password != null) {
          Arrays.fill(password, '0');
        }

        // TODO: do we need to support NTLM?
        httpConfig.setProxyAuth(proxyAuth);
      }
    }

    return httpConfig;
  }
}
