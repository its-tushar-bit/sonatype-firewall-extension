/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.RepositoryClientConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import static com.google.common.base.Preconditions.checkState;

@Named
@Singleton
public class ArtifactoryClientFactory
{
  private final RepositoryClientConfigurationDAO repositoryClientConfigurationDAO;

  @Inject
  public ArtifactoryClientFactory(final RepositoryClientConfigurationDAO repositoryClientConfigurationDAO) {
    this.repositoryClientConfigurationDAO = repositoryClientConfigurationDAO;
  }

  public ArtifactoryClientBuilder create() {
    RepositoryClientConfiguration clientConfig = getClientConfig();
    Configuration config = new Configuration();
    config.setConnectTimeout(clientConfig.getConnectionTimeout() * 1000);
    config.setSocketTimeout(clientConfig.getSocketTimeout() * 1000);
    return new ArtifactoryClientBuilder(config);
  }

  private RepositoryClientConfiguration getClientConfig() {
    RepositoryClientConfiguration configuration = repositoryClientConfigurationDAO.get();
    return configuration == null ? new RepositoryClientConfiguration() : configuration;
  }

  public static class ArtifactoryClientBuilder
  {
    // Visible for testing
    final Configuration config;

    ArtifactoryClientBuilder(final Configuration config) {
      this.config = config;
    }

    public ArtifactoryClient forArtifactory(String baseUrl, String username, char[] password) {
      config.setServerUrl(baseUrl);
      checkState(baseUrl != null, "Missing artifactory base url");

      // Anonymous access does not require authentication
      if (username != null && password != null) {
        SimpleAuthentication authentication = new SimpleAuthentication();
        authentication.setUsername(username);
        authentication.setPassword(password);
        config.setServerAuth(authentication);
      }
      return new ArtifactoryClient(config);
    }
  }
}
