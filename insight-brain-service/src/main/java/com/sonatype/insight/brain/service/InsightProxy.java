/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class InsightProxy
{
  private final Logger log = LoggerFactory.getLogger(InsightProxy.class);

  private final Configuration configuration;

  private final PasswordHandler passwordHandler;

  @Inject
  public InsightProxy(Configuration configuration, PasswordHandler passwordHandler) {
    this.configuration = configuration;
    this.passwordHandler = passwordHandler;
  }

  public <T extends HttpClientUtils.Configuration> T contextualize(final T httpConfig) {
    return contextualize(httpConfig, configuration.getHdsUrl());
  }

  public <T extends HttpClientUtils.Configuration> T contextualize(final T httpConfig, final String serverUrl) {
    httpConfig.setServerUrl(serverUrl);
    String userAgentSuffix = configuration.getUserAgentSuffix();
    httpConfig.setUserAgent(httpConfig.getUserAgent() + " " + Objects.toString(userAgentSuffix, ""));

    ProxyServerConfiguration proxyServerConfiguration = configuration.getProxyServerConfiguration();
    if (proxyServerConfiguration != null) {
      log.debug("proxyServerConfiguration: {}:{}", proxyServerConfiguration.getHostname(),
          proxyServerConfiguration.getPort());
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
    else {
      log.debug("proxyServerConfiguration is null");
    }

    return httpConfig;
  }
}
