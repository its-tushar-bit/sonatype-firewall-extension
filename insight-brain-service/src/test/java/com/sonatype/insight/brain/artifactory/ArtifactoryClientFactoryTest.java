/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory.ArtifactoryClientBuilder;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryClientFactoryTest
    extends AbstractComponentTest
{
  @Inject
  private ArtifactoryClientFactory artifactoryClientFactory;

  @Test
  public void testCreate_DefaultConfiguration() {
    ArtifactoryClientBuilder artifactoryClientBuilder = artifactoryClientFactory.create();

    assertThat(artifactoryClientBuilder).isNotNull();
    RepositoryClientConfiguration defaultRepositoryClientConfiguration = new RepositoryClientConfiguration();
    assertConfiguration(artifactoryClientBuilder.config, defaultRepositoryClientConfiguration);
  }

  @Test
  public void testCreate_CustomConfiguration() {
    RepositoryClientConfiguration defaultRepositoryClientConfiguration = new RepositoryClientConfiguration();
    RepositoryClientConfiguration repositoryClientConfiguration =
        tempEntity.newRepositoryClientConfiguration(defaultRepositoryClientConfiguration.getConnectionTimeout() + 1,
            defaultRepositoryClientConfiguration.getSocketTimeout() + 1);

    ArtifactoryClientBuilder artifactoryClientBuilder = artifactoryClientFactory.create();

    assertThat(artifactoryClientBuilder).isNotNull();
    assertConfiguration(artifactoryClientBuilder.config, repositoryClientConfiguration);
  }

  @Test
  public void testArtifactoryClientBuilder_ForArtifactory_NoCredentials() {
    Configuration configuration = new Configuration();
    ArtifactoryClientBuilder artifactoryClientBuilder = new ArtifactoryClientBuilder(configuration);
    String serverUrl = "http://serverUrl";

    ArtifactoryClient artifactoryClient = artifactoryClientBuilder.forArtifactory(serverUrl, null, null);

    assertThat(artifactoryClient).isInstanceOf(ArtifactoryClient.class);
    assertThat(configuration.getServerUrl()).isEqualTo(serverUrl);
    assertThat(configuration.getServerAuth()).isNull();
  }

  @Test
  public void testArtifactoryClientBuilder_ForArtifactory() {
    Configuration configuration = new Configuration();
    ArtifactoryClientBuilder artifactoryClientBuilder = new ArtifactoryClientBuilder(configuration);
    String serverUrl = "http://serverUrl";
    String username = "username";
    char[] password = "password".toCharArray();

    ArtifactoryClient artifactoryClient = artifactoryClientBuilder.forArtifactory(serverUrl, username, password);

    assertThat(artifactoryClient).isInstanceOf(ArtifactoryClient.class);
    assertThat(configuration.getServerUrl()).isEqualTo(serverUrl);
    SimpleAuthentication authentication = new SimpleAuthentication();
    authentication.setUsername(username);
    authentication.setPassword(password);
    assertThat(configuration.getServerAuth()).usingRecursiveComparison().isEqualTo(authentication);
  }

  private void assertConfiguration(Configuration config, RepositoryClientConfiguration repositoryClientConfiguration) {
    assertThat(config).isNotNull();
    assertThat(config.getConnectTimeout()).isEqualTo(repositoryClientConfiguration.getConnectionTimeout() * 1000);
    assertThat(config.getSocketTimeout()).isEqualTo(repositoryClientConfiguration.getSocketTimeout() * 1000);
  }
}
