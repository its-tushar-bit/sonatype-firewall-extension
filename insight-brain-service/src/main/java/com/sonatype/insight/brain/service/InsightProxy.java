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

import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.SimpleAuthentication;

@Named
@Singleton
public class InsightProxy
{
  private final InsightConfig insightConfig;

  private final ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  private final PasswordHandler passwordHandler;

  @Inject
  public InsightProxy(
      InsightConfig insightConfig,
      ProxyServerConfigurationDAO proxyServerConfigurationDAO,
      PasswordHandler passwordHandler)
  {
    this.insightConfig = insightConfig;
    this.proxyServerConfigurationDAO = proxyServerConfigurationDAO;
    this.passwordHandler = passwordHandler;
  }

  public <T extends HttpClientUtils.Configuration> T contextualize(final T httpConfig) {
    return contextualize(httpConfig, insightConfig.getHdsUrl());
  }

  public <T extends HttpClientUtils.Configuration> T contextualize(final T httpConfig, final String serverUrl) {
    httpConfig.setServerUrl(serverUrl);
    httpConfig.setUserAgent(httpConfig.getUserAgent() + " " + insightConfig.getUserAgentSuffix());

    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    if (proxyServerConfiguration != null) {
      httpConfig.setProxyHost(proxyServerConfiguration.getHostname());
      httpConfig.setProxyPort(proxyServerConfiguration.getPort());
      httpConfig.setProxyExcludeHosts(proxyServerConfiguration.getExcludeHostsList());
      if (proxyServerConfiguration.getUsername() != null) {
        final SimpleAuthentication proxyAuth = new SimpleAuthentication();
        proxyAuth.setUsername(proxyServerConfiguration.getUsername());

        char[] password = passwordHandler.decryptPassword(proxyServerConfiguration.getPassword());
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
