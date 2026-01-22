/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.client;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.RepositoryClientConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.brain.repository.RepositoryClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import static com.google.common.base.Preconditions.checkState;

/**
 * @since 1.127
 */
@Named
@Singleton
public class RepositoryClientFactory
{
  private final RepositoryClientConfigurationDAO clientConfigurationDAO;

  @Inject
  public RepositoryClientFactory(final RepositoryClientConfigurationDAO clientConfigurationDAO) {
    this.clientConfigurationDAO = clientConfigurationDAO;
  }

  public RepositoryClientBuilder create() {
    RepositoryClientConfiguration clientConfig = getClientConfig();
    Configuration config = new Configuration();
    config.setConnectTimeout(clientConfig.getConnectionTimeout() * 1000);
    config.setSocketTimeout(clientConfig.getSocketTimeout() * 1000);
    return new RepositoryClientBuilder(config);
  }

  private RepositoryClientConfiguration getClientConfig() {
    RepositoryClientConfiguration configuration = clientConfigurationDAO.get();
    return configuration == null ? new RepositoryClientConfiguration() : configuration;
  }

  public static class RepositoryClientBuilder
  {
    //visible for testing
    final Configuration config;

    RepositoryClientBuilder(final Configuration config) {
      this.config = config;
    }

    public RepositoryClient forNexus3(String baseUrl, String username, char[] password) {
      config.setServerUrl(baseUrl);
      checkState(baseUrl != null, "Missing repository base url");

      //anonymous access does not require authentication
      if (username != null && password != null) {
        SimpleAuthentication authentication = new SimpleAuthentication();
        authentication.setUsername(username);
        authentication.setPassword(password);
        config.setServerAuth(authentication);
      }
      return new NexusRepository3Client(config);
    }
  }
}
