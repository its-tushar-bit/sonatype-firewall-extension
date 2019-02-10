/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.impl.client.HttpClientBuilder;

public class RestClientConfiguration
{
  private final Configuration config;

  public RestClientConfiguration() {
    config = new Configuration();
  }

  Configuration getConfig() {
    return config;
  }

  public String getServerUrl() {
    return config.getServerUrl();
  }

  public RestClientConfiguration setServerUrl(final String serverUrl) {
    config.setServerUrl(serverUrl);
    return this;
  }

  public String getProxyHost() {
    return config.getProxyHost();
  }

  public RestClientConfiguration setProxyHost(final String proxyHost) {
    config.setProxyHost(proxyHost);
    return this;
  }

  public int getProxyPort() {
    return config.getProxyPort();
  }

  public RestClientConfiguration setProxyPort(final int proxyPort) {
    config.setProxyPort(proxyPort);
    return this;
  }

  public RestClientConfiguration setProxy(final String proxy) {
    config.setProxy(proxy);
    return this;
  }

  public RestClientConfiguration setProxyAuth(final String username,
                                              final String password,
                                              final String ntlmDomain,
                                              final String ntlmWorkstation)
  {
    final SimpleAuthentication auth = new SimpleAuthentication();
    auth.setUsername(username);
    auth.setPassword(password);
    auth.setNtlmDomain(ntlmDomain);
    auth.setNtlmWorkstation(ntlmWorkstation);
    config.setProxyAuth(auth);
    return this;
  }

  public RestClientConfiguration setHttpClientProvider(final HttpClientProvider httpClientProvider) {
    config.setHttpClientProvider(new HttpClientUtils.HttpClientProvider()
    {
      @Override
      public HttpClientBuilder create(final Configuration config) {
        return httpClientProvider.createHttpClient(RestClientConfiguration.this);
      }
    });
    return this;
  }

  public static interface HttpClientProvider
  {
    HttpClientBuilder createHttpClient(RestClientConfiguration config);
  }
}
