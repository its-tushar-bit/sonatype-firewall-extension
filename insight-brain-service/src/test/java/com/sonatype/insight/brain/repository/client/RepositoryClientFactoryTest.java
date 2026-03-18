/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.client;

import com.sonatype.insight.brain.dataaccess.configuration.RepositoryClientConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory.RepositoryClientBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryClientFactoryTest
{
  @Mock
  private RepositoryClientConfigurationDAO dao;

  private RepositoryClientFactory factory;

  @Before
  public void before() {
    factory = new RepositoryClientFactory(dao);
  }

  @Test
  public void testCreate_withDefaultConfiguration() {
    when(dao.get()).thenReturn(null);

    RepositoryClientBuilder clientBuilder = factory.create();
    clientBuilder.forNexus3("baseUrl", "user", "pass".toCharArray());
    assertClientConfiguration(clientBuilder, 30, 120);
  }

  @Test
  public void testCreate_withCustomConfiguration() {
    RepositoryClientConfiguration clientConfiguration = new RepositoryClientConfiguration();
    clientConfiguration.setConnectionTimeout(5);
    clientConfiguration.setSocketTimeout(15);
    when(dao.get()).thenReturn(clientConfiguration);

    RepositoryClientBuilder clientBuilder = factory.create();
    clientBuilder.forNexus3("baseUrl", "user", "pass".toCharArray());
    assertClientConfiguration(clientBuilder, 5, 15);
  }

  @Test
  public void testCreate_withNoAuth() {
    when(dao.get()).thenReturn(null);

    RepositoryClientBuilder clientBuilder = factory.create();
    clientBuilder.forNexus3("baseUrl", null, null);

    assertThat(clientBuilder.config.getServerAuth()).isNull();
  }

  @Test
  public void testCreate_withNoBaseUrl() {
    when(dao.get()).thenReturn(null);

    RepositoryClientBuilder clientBuilder = factory.create();
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> clientBuilder.forNexus3(null, "user", "pass".toCharArray()))
        .withMessage("Missing repository base url");
  }

  private void assertClientConfiguration(
      RepositoryClientBuilder clientBuilder,
      int connectionTimeout,
      int socketTimeout)
  {
    assertThat(clientBuilder.config.getServerUrl()).isEqualTo("baseUrl");
    assertThat(clientBuilder.config.getServerAuth().getUsername()).isEqualTo("user");
    assertThat(clientBuilder.config.getServerAuth().getPassword()).isEqualTo("pass".toCharArray());
    assertThat(clientBuilder.config.getConnectTimeout()).isEqualTo(connectionTimeout * 1000);
    assertThat(clientBuilder.config.getSocketTimeout()).isEqualTo(socketTimeout * 1000);
  }
}
